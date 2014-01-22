/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

package org.sonatype.sisu.ehcache;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.management.MBeanServer;

import com.google.common.annotations.VisibleForTesting;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.ehcache.InstrumentedEhcache;
import com.yammer.metrics.ehcache.InstrumentedEhcacheFactory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration.CacheDecoratorFactoryConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.management.ManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component holding a EHCache CacheManager, using it's "singleton pattern".
 *
 * Note: as SISU-93 is not yet here, and this component does need explicit shutdown
 * (in case when multiple instances are re-created of it, like in UT environment),
 * you have to use {@link #shutdown()} method.
 *
 * @since 2.8
 */
@Named
@Singleton
public class CacheManagerHolder
{
  private static final Logger logger = LoggerFactory.getLogger(CacheManagerHolder.class);

  private CacheManager cacheManager;

  @Inject
  public CacheManagerHolder() throws IOException {
    this(null);
  }

  @VisibleForTesting
  public CacheManagerHolder(final @Nullable File file) throws IOException {
    this.cacheManager = createCacheManager(file);

    try {
      final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ManagementService.registerMBeans(cacheManager, mBeanServer, false, false, true, true);
    }
    catch (Exception e) {
      logger.warn("Failed to register mbean: {}", e.toString());
    }
  }

  public CacheManager getCacheManager() {
    return cacheManager;
  }

  public synchronized void shutdown() {
    if (cacheManager != null) {
      logger.info("Shutting down EHCache CacheManager");
      cacheManager.shutdown();
      cacheManager = null;
    }
  }

  /**
   * Safety, to at least give JRE a chance to clean up.
   */
  @Override
  public void finalize() throws Throwable {
    try {
      shutdown();
      logger.warn("EHCache instance was not properly shut down, it is getting finalized.");
    }
    finally {
      super.finalize();
    }
  }

  // ==

  private CacheManager createCacheManager(final @Nullable File file) throws IOException {
    URL url;
    if (file != null) {
      url = file.toURI().toURL();
    }
    else {
      url = getClass().getResource("/ehcache.xml");
      if (url == null) {
        url = getClass().getResource("/ehcache-default.xml");
      }
    }

    if (url == null) {
      logger.warn("No configuration found; using defaults");
      return new CacheManager();
    }

    logger.info("Loading configuration from: {}", url);
    Configuration configuration = ConfigurationFactory.parseConfiguration(url);
    configuration.setUpdateCheck(false);
    // registering Yammer metrics decorator
    final CacheDecoratorFactoryConfiguration cacheDecoratorFactoryConfiguration = new CacheDecoratorFactoryConfiguration();
    cacheDecoratorFactoryConfiguration.setClass(InstrumentedEhcacheFactory.class.getName());
    configuration.getDefaultCacheConfiguration().addCacheDecoratorFactory(cacheDecoratorFactoryConfiguration);
    return new CacheManager(configuration);
  }
}
