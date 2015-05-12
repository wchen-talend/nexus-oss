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
package org.sonatype.nexus.httpclient.internal;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.protocol.HttpContext;

import static com.google.common.base.Preconditions.checkNotNull;

// TODO: Should we instead be using ProxySelectorRoutePlanner and implement a custom ProxySelector instead?

/**
 * An {@link HttpRoutePlanner} that uses different proxies / url scheme (http/https) and bypasses proxy for specific
 * hosts (non proxy hosts).
 *
 * @since 2.6
 */
public class NexusHttpRoutePlanner
    extends DefaultRoutePlanner
{
  /**
   * Set of patterns for matching hosts names against. Never null.
   */
  private final Set<Pattern> patterns;

  /**
   * Mapping between protocol scheme and proxy to be used
   */
  private final Map<String, HttpHost> proxies;

  /**
   * @since 2.5
   */
  public NexusHttpRoutePlanner(final Map<String, HttpHost> proxies,
                               final Set<Pattern> patterns)
  {
    super(DefaultSchemePortResolver.INSTANCE);
    this.proxies = checkNotNull(proxies);
    this.patterns = checkNotNull(patterns);
  }

  @Override
  protected HttpHost determineProxy(final HttpHost host,
                                    final HttpRequest request,
                                    final HttpContext context)
      throws HttpException
  {
    if (noProxyFor(host.getHostName())) {
      return null;
    }
    return proxies.get(host.getSchemeName());
  }

  // TODO: Should we guard this with a cache to avoid rematching regex constantly?

  /**
   * Determine if proxy should be configured for given host or not.
   *
   * @return true if no proxy should be configured.
   */
  private boolean noProxyFor(final String hostName) {
    for (Pattern pattern : patterns) {
      if (pattern.matcher(hostName).matches()) {
        return true;
      }
    }
    return false;
  }
}
