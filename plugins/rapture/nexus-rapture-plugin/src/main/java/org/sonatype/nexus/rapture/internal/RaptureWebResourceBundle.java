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
package org.sonatype.nexus.rapture.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.app.SystemStatus;
import org.sonatype.nexus.rapture.UiPluginDescriptor;
import org.sonatype.nexus.rapture.internal.ui.StateComponent;
import org.sonatype.nexus.webresources.GeneratedWebResource;
import org.sonatype.nexus.webresources.WebResource;
import org.sonatype.nexus.webresources.WebResourceBundle;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.template.TemplateAccessible;
import org.sonatype.sisu.goodies.template.TemplateEngine;
import org.sonatype.sisu.goodies.template.TemplateParameters;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Rapture {@link WebResourceBundle}.
 *
 * Provides resources:
 * <ul>
 * <li>{@code /index.html}</li>
 * <li>{@code /static/rapture/bootstrap.js}</li>
 * <li>{@code /static/rapture/app.js}</li>
 * </ul>
 *
 * @since 3.0
 */
@Named
@Singleton
public class RaptureWebResourceBundle
    extends ComponentSupport
    implements WebResourceBundle
{
  private final Provider<SystemStatus> systemStatusProvider;

  private final Provider<HttpServletRequest> servletRequestProvider;

  private final Provider<StateComponent> stateComponentProvider;

  private final TemplateEngine templateEngine;

  private final List<UiPluginDescriptor> pluginDescriptors;

  private final Gson gson;

  @Inject
  public RaptureWebResourceBundle(final Provider<SystemStatus> systemStatusProvider,
                                  final Provider<HttpServletRequest> servletRequestProvider,
                                  final Provider<StateComponent> stateComponentProvider,
                                  final TemplateEngine templateEngine,
                                  final List<UiPluginDescriptor> pluginDescriptors)
  {
    this.systemStatusProvider = checkNotNull(systemStatusProvider);
    this.servletRequestProvider = checkNotNull(servletRequestProvider);
    this.stateComponentProvider = checkNotNull(stateComponentProvider);
    this.templateEngine = checkNotNull(templateEngine);

    this.pluginDescriptors = checkNotNull(pluginDescriptors);

    // HACK: bring back logging to help sort out why dynamic collection isn't including the impl from this plugin
    log.info("UI plugin descriptors:");
    for (UiPluginDescriptor descriptor : pluginDescriptors) {
      log.info("  {}", descriptor.getPluginId());
    }

    // FIXME: Should be updated to use Jackson?
    gson = new GsonBuilder().setPrettyPrinting().create();
  }

  //
  // FIXME: optimize so that we dont duplicate things like isDebug() over and over each request
  // FIXME: for now we simply do a bit more work than is needed :-(
  //

  @Override
  public List<WebResource> getResources() {
    return ImmutableList.of(
        index_html(),
        bootstrap_js(),
        app_js()
    );
  }

  private abstract class TemplateWebResource
      extends GeneratedWebResource
  {
    private URL template(final String name) {
      URL template = getClass().getResource(name);
      checkState(template != null, "Missing template: %s", name);
      return template;
    }

    protected byte[] render(final String template, final TemplateParameters parameters) throws IOException {
      log.trace("Rendering template: {}, with params: {}", template, parameters);
      return templateEngine.render(this, template(template), parameters).getBytes();
    }
  }

  @TemplateAccessible
  public static class TemplateHelper
  {
    /**
     * Helper to return the filename for a URI.
     */
    public String fileName(final URI uri) {
      String path = uri.getPath();
      int i = path.lastIndexOf("/");
      return path.substring(i + 1, path.length());
    }
  }

  /**
   * The index.html resource.
   */
  private WebResource index_html() {
    return new TemplateWebResource()
    {
      @Override
      public String getPath() {
        return "/index.html";
      }

      @Override
      public String getContentType() {
        return HTML;
      }

      @Override
      protected byte[] generate() throws IOException {
        return render("index.vm", new TemplateParameters()
                .set("baseUrl", BaseUrlHolder.get())
                .set("debug", isDebug())
                .set("urlSuffix", generateUrlSuffix())
                .set("styles", getStyles())
                .set("scripts", getScripts())
                .set("helper", new TemplateHelper())
        );
      }
    };
  }

  /**
   * The bootstrap.js resource.
   */
  private WebResource bootstrap_js() {
    return new TemplateWebResource()
    {
      @Override
      public String getPath() {
        return "/static/rapture/bootstrap.js";
      }

      @Override
      public String getContentType() {
        return JAVASCRIPT;
      }

      @Override
      protected byte[] generate() throws IOException {
        return render("bootstrap.vm", new TemplateParameters()
                .set("baseUrl", BaseUrlHolder.get())
                .set("debug", isDebug())
                .set("namespaces", getNamespaces())
        );
      }
    };
  }

  /**
   * The app.js resource.
   */
  private WebResource app_js() {
    return new TemplateWebResource()
    {
      @Override
      public String getPath() {
        return "/static/rapture/app.js";
      }

      @Override
      public String getContentType() {
        return JAVASCRIPT;
      }

      @Override
      protected byte[] generate() throws IOException {
        return render("app.vm", new TemplateParameters()
                .set("baseUrl", BaseUrlHolder.get())
                .set("debug", isDebug())
                .set("state", gson.toJson(getState()))
                .set("pluginConfigs", getPluginConfigs())
        );
      }
    };
  }

  /**
   * Generate URL suffix to use on all requests when loading the index.
   */
  private String generateUrlSuffix() {
    StringBuilder buff = new StringBuilder();
    String version = systemStatusProvider.get().getVersion();
    buff.append("_v=").append(version);

    // if version is a SNAPSHOT, then append additional timestamp to disable cache
    if (version.endsWith("SNAPSHOT")) {
      buff.append("&_dc=").append(System.currentTimeMillis());
    }

    // when debug, add parameter
    if (isDebug()) {
      buff.append("&debug=true");
    }

    return buff.toString();
  }

  /**
   * Check if ?debug parameter is given on the request.
   */
  private boolean isDebug() {
    HttpServletRequest request = servletRequestProvider.get();
    String value = request.getParameter("debug");

    // not set
    if (value == null) {
      return false;
    }

    // ?debug
    if (value.trim().length() == 0) {
      return true;
    }

    // ?debug=<flag>
    return Boolean.parseBoolean(value);
  }

  /**
   * Returns the initial state for the application.
   */
  private Map<String, Object> getState() {
    return stateComponentProvider.get().getValues(Maps.<String, String>newHashMap());
  }

  /**
   * Find all plugin configs.
   */
  private List<String> getPluginConfigs() {
    List<String> classNames = Lists.newArrayList();
    for (UiPluginDescriptor descriptor : pluginDescriptors) {
      String className = descriptor.getConfigClassName();
      if (className != null) {
        classNames.add(className);
      }
    }
    return classNames;
  }

  /**
   * Determine all plugin namespaces.
   */
  private List<String> getNamespaces() {
    List<String> namespaces = Lists.newArrayList();
    for (UiPluginDescriptor descriptor : pluginDescriptors) {
      String ns = descriptor.getNamespace();
      if (ns != null) {
        namespaces.add(ns);
      }
    }
    return namespaces;
  }

  /**
   * Replaces "{mode}" in given path with either "prod" or "debug".
   */
  private String mode(final String path) {
    String mode = isDebug() ? "debug" : "prod";
    return path.replaceAll("\\{mode\\}", mode);
  }

  /**
   * Generate a URI for the given path.
   */
  private URI uri(String path) {
    try {
      return new URI(String.format("%s/static/rapture/%s", BaseUrlHolder.get(), path));
    }
    catch (URISyntaxException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Generate the list of CSS styles to include in the index.html.
   */
  private List<URI> getStyles() {
    List<URI> styles = Lists.newArrayList();
    styles.add(uri(mode("resources/loading-{mode}.css")));
    styles.add(uri(mode("resources/baseapp-{mode}.css")));

    // add all plugin styles
    for (UiPluginDescriptor descriptor : pluginDescriptors) {
      if (descriptor.hasStyle()) {
        String path = String.format("resources/%s-{mode}.css", descriptor.getPluginId());
        styles.add(uri(mode(path)));
      }
    }

    return styles;
  }

  /**
   * Generate the list of javascript sources to include in the index.html.
   */
  private List<URI> getScripts() {
    List<URI> scripts = Lists.newArrayList();

    scripts.add(uri(mode("baseapp-{mode}.js")));
    scripts.add(uri(mode("extdirect-{mode}.js")));
    scripts.add(uri("bootstrap.js"));

    // add all "prod" plugin scripts if debug is not enabled
    if (!isDebug()) {
      for (UiPluginDescriptor descriptor : pluginDescriptors) {
        if (descriptor.hasScript()) {
          String path = String.format("%s-prod.js", descriptor.getPluginId());
          scripts.add(uri(path));
        }
      }
    }

    scripts.add(uri("app.js"));
    return scripts;
  }
}
