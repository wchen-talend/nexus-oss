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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.app.BaseUrlManager;
import org.sonatype.nexus.common.app.SystemStatus;
import org.sonatype.nexus.security.UserIdMdcHelper;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.SERVER;
import static com.google.common.net.HttpHeaders.X_CONTENT_TYPE_OPTIONS;
import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;

/**
 * Sets up the basic environment for web-requests.
 *
 * @since 3.0
 */
@Singleton
public class EnvironmentFilter
  extends ComponentSupport
  implements Filter
{
  private final String serverHeader;

  private final BaseUrlManager baseUrlManager;

  @Inject
  public EnvironmentFilter(final Provider<SystemStatus> statusProvider,
                           final BaseUrlManager baseUrlManager)
  {
    checkNotNull(statusProvider);
    this.baseUrlManager = checkNotNull(baseUrlManager);

    // cache "Server" header value
    SystemStatus status = statusProvider.get();
    this.serverHeader = String.format("Nexus/%s (%s)", status.getVersion(), status.getEditionShort());
  }

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    // ignore
  }

  @Override
  public void destroy() {
    // ignore
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException
  {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final HttpServletResponse httpResponse = (HttpServletResponse) response;

    // start with default unknown user-id in MDC
    UserIdMdcHelper.unknown();

    // detect base-url
    baseUrlManager.detectAndHoldUrl();

    // fill in default response headers
    defaultHeaders(httpResponse);

    try {
      chain.doFilter(request, response);
    }
    finally {
      // unset user-id MDC
      UserIdMdcHelper.unset();
    }
  }

  /**
   * Add default headers to servlet response.
   */
  private void defaultHeaders(final HttpServletResponse response) {
    response.setHeader(SERVER, serverHeader);

    // NEXUS-6569 Add X-Frame-Options header
    response.setHeader(X_FRAME_OPTIONS, "SAMEORIGIN");

    // NEXUS-5023 disable IE for sniffing into response content
    response.setHeader(X_CONTENT_TYPE_OPTIONS, "nosniff");
  }
}
