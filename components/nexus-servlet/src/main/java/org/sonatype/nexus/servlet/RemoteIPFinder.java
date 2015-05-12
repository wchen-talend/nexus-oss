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
package org.sonatype.nexus.servlet;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.common.text.Strings2;

import static com.google.common.net.HttpHeaders.X_FORWARDED_FOR;

/**
 * Helper class to detect "real" IP address of remote client.
 * 
 * @since 2.7.0
 */
public class RemoteIPFinder
{
  /**
   * Returns the "real" IP address (as string) of the passed in {@link HttpServletRequest}.
   */
  public static String findIP(final HttpServletRequest request) {
    String forwardedIP = getFirstForwardedIp(request.getHeader(X_FORWARDED_FOR));

    if (forwardedIP != null) {
      return forwardedIP;
    }

    return request.getRemoteAddr();
  }

  /**
   * Returns the *left-most* resolvable IP from the given XFF string; otherwise null.
   */
  private static String getFirstForwardedIp(final String forwardedFor) {
    if (!Strings2.isEmpty(forwardedFor)) {
      return resolveIp(forwardedFor.split("\\s*,\\s*"));
    }
    return null;
  }

  /**
   * Returns the *left-most* resolvable IP from the given sequence.
   */
  private static String resolveIp(final String[] ipAddresses) {
    for (String ip : ipAddresses) {
      InetAddress ipAdd;
      try {
        ipAdd = InetAddress.getByName(ip);
      }
      catch (UnknownHostException e) {
        continue;
      }
      if (ipAdd instanceof Inet4Address || ipAdd instanceof Inet6Address) {
        return ip;
      }
    }
    return null;
  }
}
