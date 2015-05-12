/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.security.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.security.SecurityConfigurationChanged;
import org.sonatype.nexus.security.UserPrincipalsExpired;
import org.sonatype.nexus.security.authz.AuthorizationConfigurationChanged;
import org.sonatype.nexus.security.realm.RealmConfiguration;
import org.sonatype.nexus.security.realm.RealmConfigurationStore;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.sisu.goodies.common.Mutex;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Default {@link RealmManager}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class RealmManagerImpl
  extends StateGuardLifecycleSupport
  implements RealmManager
{
  private final EventBus eventBus;

  private final RealmConfigurationStore store;

  private final Provider<RealmConfiguration> defaults;

  private final RealmSecurityManager realmSecurityManager;

  private final Map<String, Realm> availableRealms;

  private final Mutex lock = new Mutex();

  private RealmConfiguration configuration;

  @Inject
  public RealmManagerImpl(final EventBus eventBus,
                          final RealmConfigurationStore store,
                          final @Named("initial") Provider<RealmConfiguration> defaults,
                          final RealmSecurityManager realmSecurityManager,
                          final Map<String, Realm> availableRealms)
  {
    this.eventBus = checkNotNull(eventBus);
    this.store = checkNotNull(store);
    log.debug("Store: {}", store);
    this.defaults = checkNotNull(defaults);
    log.debug("Defaults: {}", defaults);
    this.realmSecurityManager = checkNotNull(realmSecurityManager);
    this.availableRealms = checkNotNull(availableRealms);
  }

  //
  // Lifecycle
  //

  @Override
  protected void doStart() throws Exception {
    installRealms();

    eventBus.register(this);
  }

  @Override
  protected void doStop() throws Exception {
    eventBus.unregister(this);

    configuration = null;

    // reset shiro caches
    Collection<Realm> realms = realmSecurityManager.getRealms();
    if (realms != null) {
      for (Realm realm : realms) {
        if (realm instanceof AuthenticatingRealm) {
          ((AuthenticatingRealm) realm).setAuthenticationCache(null);
        }
        if (realm instanceof AuthorizingRealm) {
          ((AuthorizingRealm) realm).setAuthorizationCache(null);
        }
      }
    }
  }

  //
  // Configuration
  //

  /**
   * Load configuration from store, or use defaults.
   */
  private RealmConfiguration loadConfiguration() {
    RealmConfiguration model = store.load();

    // use defaults if no configuration was loaded from the store
    if (model == null) {
      model = defaults.get();

      // default config must not be null
      checkNotNull(model);

      log.info("Using default configuration: {}", model);
    }
    else {
      log.info("Loaded configuration: {}", model);
    }

    return model;
  }

  /**
   * Return configuration, loading if needed.
   *
   * The result model should be considered _immutable_ unless copied.
   */
  private RealmConfiguration getConfigurationInternal() {
    synchronized (lock) {
      if (configuration == null) {
        configuration = loadConfiguration();
      }
      return configuration;
    }
  }

  /**
   * Return _copy_ of configuration.
   */
  @Override
  @Guarded(by = STARTED)
  public RealmConfiguration getConfiguration() {
    return getConfigurationInternal().copy();
  }

  @Override
  @Guarded(by = STARTED)
  public void setConfiguration(final RealmConfiguration configuration) {
    checkNotNull(configuration);

    RealmConfiguration model = configuration.copy();
    log.info("Saving configuration: {}", model);
    synchronized (lock) {
      store.save(model);
      this.configuration = model;
    }

    installRealms();
  }

  //
  // Realm installation
  //

  /**
   * Resolve and install realm components.
   */
  private void installRealms() {
    List<Realm> realms = resolveRealms();
    log.debug("Installing realms: {}", realms);
    realmSecurityManager.setRealms(realms);
  }

  /**
   * Resolve configured realm components.
   */
  private List<Realm> resolveRealms() {
    List<Realm> result = Lists.newArrayList();
    RealmConfiguration model = getConfigurationInternal();

    log.debug("Available realms: {}", availableRealms);

    for (String realmName : model.getRealmNames()) {
      Realm realm = availableRealms.get(realmName);

      // FIXME: Resolve what purpose this is for, looks like legacy?
      if (realm == null) {
        log.debug("Failed to look up realm '{}' as a component, trying reflection", realmName);
        // If that fails, will simply use reflection to load
        try {
          realm = (Realm) getClass().getClassLoader().loadClass(realmName).newInstance();
        }
        catch (Exception e) {
          log.error("Unable to lookup security realms", e);
        }
      }

      if (realm != null) {
        result.add(realm);
      }
    }

    return result;
  }

  //
  // Helpers
  //

  @Override
  public boolean isRealmEnabled(final String realmName) {
    checkNotNull(realmName);
    return getConfigurationInternal().getRealmNames().contains(realmName);
  }

  @Override
  public void enableRealm(final String realmName, final boolean enable) {
    if (enable) {
      enableRealm(realmName);
    }
    else {
      disableRealm(realmName);
    }
  }

  @Override
  public void enableRealm(final String realmName) {
    checkNotNull(realmName);

    log.debug("Enabling realm: {}", realmName);
    RealmConfiguration model = getConfiguration();
    if (!model.getRealmNames().contains(realmName)) {
      model.getRealmNames().add(realmName);
      setConfiguration(model);
    }
    else {
      log.debug("Realm already enabled: {}", realmName);
    }
  }

  @Override
  public void disableRealm(final String realmName) {
    checkNotNull(realmName);

    log.debug("Disabling realm: {}", realmName);
    RealmConfiguration model = getConfiguration();
    model.getRealmNames().remove(realmName);
    setConfiguration(model);
  }

  //
  // Event handling
  //

  @Subscribe
  public void onEvent(final UserPrincipalsExpired event) {
    // TODO: we could do this better, not flushing whole cache for single user being deleted
    clearAuthcRealmCaches();
  }

  @Subscribe
  public void onEvent(final AuthorizationConfigurationChanged event) {
    // TODO: we could do this better, not flushing whole cache for single user roles being updated
    clearAuthzRealmCaches();
  }

  @Subscribe
  public void onEvent(final SecurityConfigurationChanged event) {
    clearAuthcRealmCaches();
    clearAuthzRealmCaches();
    // FIXME: What is the purpose of nulling the configuration model here?
    //securitySettingsManager.clearCache();
    installRealms();
  }

  /**
   * Looks up registered {@link AuthenticatingRealm}s, and clears their authc caches if they have it set.
   */
  private void clearAuthcRealmCaches() {
    // NOTE: we don't need to iterate all the Sec Managers, they use the same Realms, so one is fine.
    Collection<Realm> realms = realmSecurityManager.getRealms();
    if (realms != null) {
      for (Realm realm : realms) {
        if (realm instanceof AuthenticatingRealm) {
          Cache cache = ((AuthenticatingRealm) realm).getAuthenticationCache();
          if (cache != null) {
            log.debug("Clearing cache: {}", cache);
            cache.clear();
          }
        }
      }
    }
  }

  /**
   * Looks up registered {@link AuthorizingRealm}s, and clears their authz caches if they have it set.
   */
  private void clearAuthzRealmCaches() {
    // NOTE: we don't need to iterate all the Sec Managers, they use the same Realms, so one is fine.
    Collection<Realm> realms = realmSecurityManager.getRealms();
    if (realms != null) {
      for (Realm realm : realms) {
        if (realm instanceof AuthorizingRealm) {
          Cache cache = ((AuthorizingRealm) realm).getAuthorizationCache();
          if (cache != null) {
            log.debug("Clearing cache: {}", cache);
            cache.clear();
          }
        }
      }
    }
  }
}
