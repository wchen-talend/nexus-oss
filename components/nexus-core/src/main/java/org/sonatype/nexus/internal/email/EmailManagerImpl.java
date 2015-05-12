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
package org.sonatype.nexus.internal.email;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.email.EmailConfigurationStore;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.common.Mutex;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link EmailManager}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class EmailManagerImpl
    extends ComponentSupport
    implements EmailManager
{
  private final EmailConfigurationStore store;

  private final Provider<EmailConfiguration> defaults;

  private final Mutex lock = new Mutex();

  private EmailConfiguration configuration;

  @Inject
  public EmailManagerImpl(final EmailConfigurationStore store,
                          @Named("initial") final Provider<EmailConfiguration> defaults)
  {
    this.store = checkNotNull(store);
    this.defaults = checkNotNull(defaults);
  }

  //
  // Configuration
  //

  /**
   * Load configuration from store, or use defaults.
   */
  private EmailConfiguration loadConfiguration() {
    EmailConfiguration model = store.load();

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
  private EmailConfiguration getConfigurationInternal() {
    synchronized (lock) {
      if (configuration == null) {
        configuration = loadConfiguration();
      }
      return configuration;
    }
  }

  @Override
  public EmailConfiguration getConfiguration() {
    return getConfigurationInternal().copy();
  }

  @Override
  public void setConfiguration(final EmailConfiguration configuration) {
    checkNotNull(configuration);

    EmailConfiguration model = configuration.copy();
    // TODO: Validate configuration before saving?  Or leave to ext.direct?

    log.info("Saving configuration: {}", model);
    synchronized (lock) {
      store.save(model);
      this.configuration = model;
    }
  }

  //
  // Mail sending
  //

  /**
   * Apply server configuration to email.
   */
  private Email apply(final EmailConfiguration configuration, final Email mail) throws EmailException {
    mail.setHostName(configuration.getHost());
    mail.setSmtpPort(configuration.getPort());

    // TODO: ssl/tls configuration

    // default from address
    if (mail.getFromAddress() == null) {
      mail.setFrom(configuration.getFromAddress());
    }

    // apply subject prefix if configured
    String subjectPrefix = configuration.getSubjectPrefix();
    if (subjectPrefix != null) {
      String subject = mail.getSubject();
      mail.setSubject(String.format("%s %s", subjectPrefix, subject));
    }

    return mail;
  }

  @Override
  public void send(final Email mail) throws EmailException {
    checkNotNull(mail);

    EmailConfiguration model = getConfigurationInternal();
    if (model.isEnabled()) {
      Email prepared = apply(model, mail);
      prepared.send();
    }
  }

  @Override
  public void sendVerification(final EmailConfiguration configuration, final String address) throws EmailException {
    checkNotNull(configuration);
    checkNotNull(address);

    Email mail = new SimpleEmail();
    mail.setSubject("Email configuration verification");
    mail.addTo(address);
    mail.setMsg("Verification successful");
    mail = apply(configuration, mail);
    mail.send();
  }
}
