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
package org.sonatype.nexus.internal.httpclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.sonatype.nexus.httpclient.SSLContextSelector;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.protocol.HttpContext;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import sun.security.ssl.SSLSocketImpl;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Nexus specific implementation of {@link LayeredConnectionSocketFactory}, used for HTTPS connections.
 *
 * @since 2.8
 */
public class NexusSSLConnectionSocketFactory
    implements LayeredConnectionSocketFactory
{
  private static final Splitter propertiesSplitter = Splitter.on(',').trimResults().omitEmptyStrings();

  private final SSLSocketFactory defaultSocketFactory;

  @Nullable
  private final List<SSLContextSelector> sslContextSelectors;

  private final X509HostnameVerifier hostnameVerifier;

  private final String[] supportedProtocols;

  private final String[] supportedCipherSuites;

  public NexusSSLConnectionSocketFactory(final SSLSocketFactory defaultSocketFactory,
                                         final X509HostnameVerifier hostnameVerifier,
                                         @Nullable final List<SSLContextSelector> sslContextSelectors)
  {
    this.defaultSocketFactory = checkNotNull(defaultSocketFactory);
    this.hostnameVerifier = checkNotNull(hostnameVerifier);
    this.sslContextSelectors = sslContextSelectors; // might be null
    this.supportedProtocols = split(System.getProperty("https.protocols"));
    this.supportedCipherSuites = split(System.getProperty("https.cipherSuites"));
  }

  public NexusSSLConnectionSocketFactory(@Nullable final List<SSLContextSelector> sslContextSelectors) {
    this((javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault(),
        SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER,
        sslContextSelectors);
  }

  private SSLSocketFactory select(final HttpContext context) {
    if (sslContextSelectors != null) {
      for (SSLContextSelector selector : sslContextSelectors) {
        SSLContext sslContext = selector.select(context);
        if (sslContext != null) {
          return sslContext.getSocketFactory();
        }
      }
    }
    return defaultSocketFactory;
  }

  private void verifyHostname(final SSLSocket sslsock, final String hostname) throws IOException {
    try {
      hostnameVerifier.verify(hostname, sslsock);
    }
    catch (final IOException e) {
      Closeables.close(sslsock, true);
      throw e;
    }
  }

  @Override
  public Socket createSocket(final HttpContext context) throws IOException {
    return configure((SSLSocket) select(context).createSocket());
  }

  @Override
  @IgnoreJRERequirement
  public Socket connectSocket(final int connectTimeout,
                              final Socket socket,
                              final HttpHost host,
                              final InetSocketAddress remoteAddress,
                              final InetSocketAddress localAddress,
                              final HttpContext context)
      throws IOException
  {
    checkNotNull(host);
    checkNotNull(remoteAddress);
    final Socket sock = socket != null ? socket : createSocket(context);
    if (localAddress != null) {
      sock.bind(localAddress);
    }
    // NEXUS-6838: Server Name Indication support, a TLS feature that allows SSL
    // "virtual hosting" (multiple certificates) over single IP address + port.
    // Some CDN solutions requires this for HTTPS, as they choose certificate
    // to use based on "expected" hostname that is being passed here below
    // and is used during SSL handshake. Requires Java7+
    if (sock instanceof SSLSocketImpl) {
      ((SSLSocketImpl) sock).setHost(host.getHostName());
    }
    try {
      sock.connect(remoteAddress, connectTimeout);
    }
    catch (final IOException e) {
      Closeables.close(sock, true);
      throw e;
    }
    // Setup SSL layering if necessary
    if (sock instanceof SSLSocket) {
      final SSLSocket sslsock = (SSLSocket) sock;
      sslsock.startHandshake();
      verifyHostname(sslsock, host.getHostName());
      return sock;
    }
    else {
      return createLayeredSocket(sock, host.getHostName(), remoteAddress.getPort(), context);
    }
  }

  @Override
  public Socket createLayeredSocket(final Socket socket, final String target, final int port, final HttpContext context)
      throws IOException
  {
    checkNotNull(socket);
    checkNotNull(target);
    final SSLSocket sslsock = configure((SSLSocket) select(context).createSocket(socket, target, port, true));
    sslsock.startHandshake();
    verifyHostname(sslsock, target);
    return sslsock;
  }

  private SSLSocket configure(final SSLSocket socket) {
    if (supportedProtocols != null) {
      socket.setEnabledProtocols(supportedProtocols);
    }
    else {
      // If supported protocols are not explicitly set, remove all SSL protocol versions
      String[] allProtocols = socket.getSupportedProtocols();
      List<String> enabledProtocols = new ArrayList<>(allProtocols.length);
      for (String protocol : allProtocols) {
        if (!protocol.startsWith("SSL")) {
          enabledProtocols.add(protocol);
        }
      }
      socket.setEnabledProtocols(enabledProtocols.toArray(new String[enabledProtocols.size()]));
    }
    if (supportedCipherSuites != null) {
      socket.setEnabledCipherSuites(supportedCipherSuites);
    }
    return socket;
  }

  private static String[] split(final String s) {
    if (StringUtils.isBlank(s)) {
      return null;
    }
    return Iterables.toArray(propertiesSplitter.split(s), String.class);
  }
}
