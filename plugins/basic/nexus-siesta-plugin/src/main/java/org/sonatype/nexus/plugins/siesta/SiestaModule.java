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
package org.sonatype.nexus.plugins.siesta;

import javax.inject.Named;

import org.sonatype.nexus.security.FilterChainModule;
import org.sonatype.nexus.security.SecurityFilter;
import org.sonatype.nexus.security.anonymous.AnonymousFilter;
import org.sonatype.nexus.security.authc.NexusBasicHttpAuthenticationFilter;
import org.sonatype.siesta.server.SiestaServlet;
import org.sonatype.siesta.server.resteasy.ResteasyModule;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.servlet.ServletModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Siesta plugin module.
 *
 * @since 2.4
 */
@Named
public class SiestaModule
    extends AbstractModule
{
  private static final Logger log = LoggerFactory.getLogger(SiestaModule.class);

  public static final String SERVICE_NAME = "siesta";

  public static final String MOUNT_POINT = "/service/" + SERVICE_NAME;

  public static final String SKIP_MODULE_CONFIGURATION = SiestaModule.class.getName() + ".skip";

  @Override
  protected void configure() {
    // HACK: avoid configuration of this module in casses as it is not wanted. e.g. automatically discovered by sisu
    if (!Boolean.getBoolean(SKIP_MODULE_CONFIGURATION)) {
      doConfigure();
    }
  }

  private void doConfigure() {
    install(new ResteasyModule());

    install(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        log.debug("Mount point: {}", MOUNT_POINT);

        bind(SiestaServlet.class);
        serve(MOUNT_POINT + "/*").with(SiestaServlet.class, ImmutableMap.of(
            "resteasy.servlet.mapping.prefix", MOUNT_POINT
        ));
        filter(MOUNT_POINT + "/*").through(SecurityFilter.class);
      }
    });

    install(new FilterChainModule()
    {
      @Override
      protected void configure() {
        addFilterChain(MOUNT_POINT + "/**", NexusBasicHttpAuthenticationFilter.NAME, AnonymousFilter.NAME);
      }
    });
  }
}
