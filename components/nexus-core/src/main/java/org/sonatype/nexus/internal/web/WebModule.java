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
package org.sonatype.nexus.internal.web;

import javax.inject.Named;

import org.sonatype.nexus.internal.metrics.MetricsModule;
import org.sonatype.nexus.internal.orient.OrientModule;

import com.google.inject.AbstractModule;
import com.google.inject.servlet.DynamicGuiceFilter;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;
import org.eclipse.sisu.inject.DefaultRankingFunction;
import org.eclipse.sisu.inject.RankingFunction;

/**
 * Web module.
 * 
 * @since 3.0
 */
@Named
public class WebModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(GuiceFilter.class).to(DynamicGuiceFilter.class);

    install(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        bind(EnvironmentFilter.class);
        bind(ErrorPageFilter.class);

        filter("/*").through(EnvironmentFilter.class);
        filter("/*").through(ErrorPageFilter.class);

        bind(ErrorPageServlet.class);

        serve("/error.html").with(ErrorPageServlet.class);

        // our configuration needs to be first-most when calculating order (some fudge room for edge-cases)
        bind(RankingFunction.class).toInstance(new DefaultRankingFunction(0x70000000));
      }
    });

    install(new MetricsModule());
    install(new OrientModule());
  }
}
