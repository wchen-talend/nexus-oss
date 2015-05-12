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
package org.sonatype.nexus.internal.httpclient

import org.sonatype.nexus.httpclient.config.ConnectionConfiguration
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration
import org.sonatype.nexus.orient.DatabaseInstanceRule
import org.sonatype.sisu.litmus.testsupport.TestSupport

import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Trials for {@link HttpClientConfigurationEntityAdapter}.
 */
class HttpClientConfigurationEntityAdapterTrial
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = new DatabaseInstanceRule('test')

  private HttpClientConfigurationEntityAdapter underTest

  @Before
  void setUp() {
    underTest = new HttpClientConfigurationEntityAdapter()
  }

  @Test
  void 'set and get simple'() {
    database.instance.connect().withCloseable {db ->
      underTest.register(db)

      def config = new HttpClientConfiguration(
          connection: new ConnectionConfiguration(
              timeout: 1234
          ),
          authentication: new UsernameAuthenticationConfiguration(
              username: 'admin',
              password: 'admin123'
          )
      )
      underTest.set(db, config)

      config = underTest.get(db)
      log config
    }
  }
}
