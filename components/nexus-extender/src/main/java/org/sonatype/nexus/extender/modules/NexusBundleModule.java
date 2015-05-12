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
package org.sonatype.nexus.extender.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.guice.AbstractInterceptorModule;
import org.sonatype.nexus.validation.ValidationModule;
import org.sonatype.sisu.goodies.inject.converter.TypeConverterSupport;

import com.google.common.base.Strings;
import com.google.inject.Module;
import org.apache.shiro.guice.aop.ShiroAopModule;
import org.eclipse.sisu.bean.LifecycleModule;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.launch.BundleModule;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Nexus specific {@link BundleModule} that uses bundle imports to decide what to install.
 * 
 * @since 3.0
 */
public class NexusBundleModule
    extends BundleModule
{
  private static final ShiroAopModule shiroAopModule = new ShiroAopModule();

  private static final SecurityFilterModule securityFilterModule = new SecurityFilterModule();

  private static final MetricsRegistryModule metricsRegistryModule = new MetricsRegistryModule();

  private static final InstrumentationModule instrumentationModule = new InstrumentationModule();

  private static final ValidationModule validationModule = new ValidationModule();

  private static final WebResourcesModule webResourcesModule = new WebResourcesModule();

  private static final RankingModule rankingModule = new RankingModule();

  private final Map<?, ?> nexusProperties;

  private final ServletContextModule servletContextModule;

  private final List<AbstractInterceptorModule> interceptorModules;

  private final List<TypeConverterSupport> converterModules;

  private final LifecycleModule lifecycleModule;

  private final String imports;

  public NexusBundleModule(final Bundle bundle, final MutableBeanLocator locator, final Map<?, ?> nexusProperties,
      final ServletContextModule servletContextModule, final List<AbstractInterceptorModule> interceptorModules,
      final List<TypeConverterSupport> converterModules, final LifecycleModule lifecycleModule)
  {
    super(bundle, locator);

    this.nexusProperties = nexusProperties;
    this.servletContextModule = servletContextModule;
    this.interceptorModules = interceptorModules;
    this.converterModules = converterModules;
    this.lifecycleModule = lifecycleModule;

    imports = Strings.nullToEmpty(bundle.getHeaders().get(Constants.IMPORT_PACKAGE));
  }

  @Override
  protected List<Module> modules() {
    List<Module> modules = new ArrayList<>();

    maybeAddShiroAOP(modules);
    maybeAddSecurityFilter(modules);
    maybeAddServletContext(modules);
    maybeAddMetricsRegistry(modules);
    maybeAddInstrumentation(modules);
    maybeAddValidation(modules);
    maybeAddWebResources(modules);
    maybeAddInterceptors(modules);
    maybeAddLifecycle(modules);
    modules.addAll(super.modules());
    modules.addAll(converterModules);
    modules.add(rankingModule);

    return modules;
  }

  @Override
  protected Map<?, ?> getProperties() {
    return nexusProperties;
  }

  @Override
  protected Module spaceModule() {
    return new SpaceModule(space, BeanScanning.GLOBAL_INDEX);
  }

  private void maybeAddShiroAOP(List<Module> modules) {
    if (imports.contains("org.apache.shiro.authz.annotation")) {
      modules.add(shiroAopModule);
    }
  }

  private void maybeAddSecurityFilter(List<Module> modules) {
    if (imports.contains("org.sonatype.nexus.security")) {
      modules.add(securityFilterModule);
    }
  }

  private void maybeAddServletContext(List<Module> modules) {
    if (imports.contains("com.google.inject.servlet")) {
      modules.add(servletContextModule);
    }
  }

  private void maybeAddMetricsRegistry(List<Module> modules) {
    if (imports.contains("com.codahale.metrics")) {
      modules.add(metricsRegistryModule);
    }
  }

  private void maybeAddInstrumentation(List<Module> modules) {
    if (imports.contains("com.codahale.metrics.annotation")) {
      modules.add(instrumentationModule);
    }
  }

  private void maybeAddValidation(List<Module> modules) {
    if (imports.contains("org.sonatype.nexus.validation")) {
      modules.add(validationModule);
    }
  }

  private void maybeAddWebResources(List<Module> modules) {
    if (space.getBundle().getEntry("static") != null) {
      modules.add(webResourcesModule);
    }
  }

  private void maybeAddInterceptors(List<Module> modules) {
    for (AbstractInterceptorModule aim : interceptorModules) {
      if (aim.appliesTo(space)) {
        modules.add(aim);
      }
    }
  }

  private void maybeAddLifecycle(List<Module> modules) {
    if (imports.contains("javax.annotation")) {
      modules.add(lifecycleModule);
    }
  }
}
