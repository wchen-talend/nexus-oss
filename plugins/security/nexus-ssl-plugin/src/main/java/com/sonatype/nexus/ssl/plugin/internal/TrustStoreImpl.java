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
package com.sonatype.nexus.ssl.plugin.internal;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.sonatype.nexus.ssl.model.TrustStoreKey;
import com.sonatype.nexus.ssl.plugin.TrustStore;

import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.ssl.keystore.CertificateUtil;
import org.sonatype.sisu.goodies.ssl.keystore.KeyStoreManager;
import org.sonatype.sisu.goodies.ssl.keystore.KeystoreException;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link TrustStore} implementation.
 *
 * @since ssl 1.0
 */
@Named
@Singleton
public class TrustStoreImpl
    extends ComponentSupport
    implements TrustStore
{

  public static final SecureRandom DEFAULT_RANDOM = null;

  private final KeyManager[] keyManagers;

  private final TrustManager[] trustManagers;

  private final Set<TrustStoreKey> enabledKeys;

  private final KeyStoreManager keyStoreManager;

  private volatile SSLContext sslcontext;

  @Inject
  public TrustStoreImpl(
      final @Named(SSLConstants.ID_PREFIX) KeyStoreManager keyStoreManager)
      throws Exception
  {
    this.keyStoreManager = checkNotNull(keyStoreManager);
    keyManagers = getSystemKeyManagers();
    trustManagers = getTrustManagers(keyStoreManager);
    enabledKeys = Sets.newCopyOnWriteArraySet();
  }

  @Override
  public Certificate importTrustCertificate(final Certificate certificate, final String alias)
      throws KeystoreException
  {
    keyStoreManager.importTrustCertificate(certificate, alias);
    return certificate;
  }

  @Override
  public Certificate importTrustCertificate(final String certificateInPEM, final String alias)
      throws KeystoreException, CertificateParsingException
  {
    final Certificate certificate = CertificateUtil.decodePEMFormattedCertificate(certificateInPEM);
    keyStoreManager.importTrustCertificate(certificate, alias);
    return certificate;
  }

  @Override
  public Certificate getTrustedCertificate(final String alias)
      throws KeystoreException
  {
    return keyStoreManager.getTrustedCertificate(alias);
  }

  @Override
  public Collection<Certificate> getTrustedCertificates()
      throws KeystoreException
  {
    return keyStoreManager.getTrustedCertificates();
  }

  @Override
  public void removeTrustCertificate(final String alias)
      throws KeystoreException
  {
    keyStoreManager.removeTrustCertificate(alias);
    sslcontext = null;
  }

  @Override
  public void enableFor(final TrustStoreKey key) {
    enabledKeys.add(key);
    log.info("Nexus SSL Trust Store enabled for {}", key);
  }

  @Override
  public void disableFor(final TrustStoreKey key) {
    enabledKeys.remove(key);
    log.info("Nexus SSL Trust Store disabled for {}", key);
  }

  @Override
  public SSLContext getSSLContext() {
    SSLContext _sslcontext = this.sslcontext; // local variable allows concurrent removeTrustCertificate
    if (_sslcontext == null) {
      try {
        _sslcontext = SSLContext.getInstance(SSLConnectionSocketFactory.TLS);
        _sslcontext.init(keyManagers, trustManagers, DEFAULT_RANDOM);
        this.sslcontext = _sslcontext;
      }
      catch (Exception e) {
        log.debug("Could not create SSL context", e);
        throw Throwables.propagate(e);
      }
    }
    return _sslcontext;
  }

  @Override
  public SSLContext getSSLContextFor(final TrustStoreKey key) {
    if (enabledKeys.contains(key)) {
      return getSSLContext();
    }
    return null;
  }

  private static TrustManager[] getTrustManagers(final KeyStoreManager keyStoreManager)
      throws Exception
  {
    final X509TrustManager managedTrustManager = getManagedTrustManager(checkNotNull(keyStoreManager));
    final TrustManager[] systemTrustManagers = getSystemTrustManagers();

    if (systemTrustManagers != null && managedTrustManager != null) {
      final TrustManager[] trustManagers = new TrustManager[systemTrustManagers.length];
      for (int i = 0; i < systemTrustManagers.length; i++) {
        final TrustManager tm = trustManagers[i] = systemTrustManagers[i];
        if (tm instanceof X509TrustManager) {
          trustManagers[i] = new X509TrustManager()
          {

            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException
            {
              ((X509TrustManager) tm).checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException
            {
              try {
                ((X509TrustManager) tm).checkServerTrusted(chain, authType);
              }
              catch (CertificateException e) {
                try {
                  managedTrustManager.checkServerTrusted(chain, authType);
                }
                catch (CertificateException ignore) {
                  throw e;
                }
              }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return ((X509TrustManager) tm).getAcceptedIssuers();
            }
          };
        }
      }
      return trustManagers;
    }
    return null;
  }

  private static X509TrustManager getManagedTrustManager(final KeyStoreManager keyStoreManager)
      throws KeystoreException
  {
    final TrustManager[] managedTrustManagers = keyStoreManager.getTrustManagers();
    if (managedTrustManagers != null) {
      for (TrustManager tm : managedTrustManagers) {
        if (tm instanceof X509TrustManager) {
          return (X509TrustManager) tm;
        }
      }
    }
    return null;
  }

  private static KeyManager[] getSystemKeyManagers()
      throws Exception
  {
    KeyManagerFactory keyManagerFactory;

    String keyAlgorithm = System.getProperty("ssl.KeyManagerFactory.algorithm");
    if (keyAlgorithm == null) {
      keyAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
    }
    String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType");
    if (keyStoreType == null) {
      keyStoreType = KeyStore.getDefaultType();
    }
    if ("none".equalsIgnoreCase(keyStoreType)) {
      keyManagerFactory = KeyManagerFactory.getInstance(keyAlgorithm);
    }
    else {
      final String keyStoreFileName = System.getProperty("javax.net.ssl.keyStore");
      if (keyStoreFileName != null) {
        File keyStoreFile = new File(keyStoreFileName);
        keyManagerFactory = KeyManagerFactory.getInstance(keyAlgorithm);
        String keyStoreProvider = System.getProperty("javax.net.ssl.keyStoreProvider");
        KeyStore keyStore;
        if (keyStoreProvider != null) {
          keyStore = KeyStore.getInstance(keyStoreType, keyStoreProvider);
        }
        else {
          keyStore = KeyStore.getInstance(keyStoreType);
        }
        String password = System.getProperty("javax.net.ssl.keyStorePassword");
        FileInputStream in = new FileInputStream(keyStoreFile);
        try {
          keyStore.load(in, password != null ? password.toCharArray() : null);
        }
        finally {
          in.close();
        }
        keyManagerFactory.init(keyStore, password != null ? password.toCharArray() : null);
      }
      else {
        return null;
      }
    }

    return keyManagerFactory.getKeyManagers();
  }

  private static TrustManager[] getSystemTrustManagers()
      throws Exception
  {
    TrustManagerFactory trustManagerFactory;

    String trustAlgorithm = System.getProperty("ssl.TrustManagerFactory.algorithm");
    if (trustAlgorithm == null) {
      trustAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
    }
    String trustStoreType = System.getProperty("javax.net.ssl.trustStoreType");
    if (trustStoreType == null) {
      trustStoreType = KeyStore.getDefaultType();
    }
    if ("none".equalsIgnoreCase(trustStoreType)) {
      trustManagerFactory = TrustManagerFactory.getInstance(trustAlgorithm);
    }
    else {
      File trustStoreFile;
      KeyStore trustStore;

      String trustStoreFileName = System.getProperty("javax.net.ssl.trustStore");
      if (trustStoreFileName != null) {
        trustStoreFile = new File(trustStoreFileName);
        trustManagerFactory = TrustManagerFactory.getInstance(trustAlgorithm);
        final String trustStoreProvider = System.getProperty("javax.net.ssl.trustStoreProvider");
        if (trustStoreProvider != null) {
          trustStore = KeyStore.getInstance(trustStoreType, trustStoreProvider);
        }
        else {
          trustStore = KeyStore.getInstance(trustStoreType);
        }
      }
      else {
        File javaHome = new File(System.getProperty("java.home"));
        File file = new File(javaHome, "lib/security/jssecacerts");
        if (!file.exists()) {
          file = new File(javaHome, "lib/security/cacerts");
          trustStoreFile = file;
        }
        else {
          trustStoreFile = file;
        }

        trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      }
      final String password = System.getProperty("javax.net.ssl.trustStorePassword");
      final FileInputStream in = new FileInputStream(trustStoreFile);
      try {
        trustStore.load(in, password != null ? password.toCharArray() : null);
      }
      finally {
        in.close();
      }
      trustManagerFactory.init(trustStore);
    }
    return trustManagerFactory.getTrustManagers();
  }

}
