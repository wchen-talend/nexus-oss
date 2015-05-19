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
package com.sonatype.nexus.ssl.plugin.internal.keystore;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.nexus.ssl.plugin.internal.SSLConstants;

import org.sonatype.sisu.goodies.crypto.CryptoHelper;
import org.sonatype.sisu.goodies.ssl.keystore.KeyStoreManagerConfiguration;

/**
 * SSL plugin specific key store manager.
 *
 * @since ssl 1.0
 */
@Named(SSLConstants.ID_PREFIX)
@Singleton
public class KeyStoreManagerImpl
    extends org.sonatype.sisu.goodies.ssl.keystore.KeyStoreManagerImpl
{
  @Inject
  public KeyStoreManagerImpl(final CryptoHelper crypto,
                             final @Named(SSLConstants.ID_PREFIX) KeyStoreManagerConfiguration config)
  {
    super(crypto, config);
  }
}
