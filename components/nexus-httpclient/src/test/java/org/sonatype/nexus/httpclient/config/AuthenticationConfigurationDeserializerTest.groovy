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

package org.sonatype.nexus.httpclient.config

import org.sonatype.sisu.litmus.testsupport.TestSupport

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import groovy.transform.ToString
import org.junit.Before
import org.junit.Test

/**
 * Tests for {@link AuthenticationConfigurationDeserializer}.
 */
class AuthenticationConfigurationDeserializerTest
    extends TestSupport
{
  private ObjectMapper objectMapper

  @Before
  void setUp() {
    objectMapper = new ObjectMapper()
  }

  @ToString
  static class AuthContainer
  {
    @JsonDeserialize(using = AuthenticationConfigurationDeserializer.class)
    AuthenticationConfiguration auth
  }

  @Test
  void 'read username'() {
    def example = new AuthContainer(auth:
        new UsernameAuthenticationConfiguration(username: 'admin', password: 'admin123')
    )

    def json = objectMapper.writeValueAsString(example)
    log json

    def obj = objectMapper.readValue(json, AuthContainer.class)
    log obj

    assert obj != null
    assert obj.auth != null
    assert obj.auth instanceof UsernameAuthenticationConfiguration
    assert obj.auth.type == UsernameAuthenticationConfiguration.TYPE

    UsernameAuthenticationConfiguration target = obj.auth as UsernameAuthenticationConfiguration
    assert target.username == 'admin'
    assert target.password == 'admin123'
  }

  @Test(expected = JsonMappingException.class)
  void 'invalid type'() {
    def json = '{"auth":{"type":"invalid"}}'
    objectMapper.readValue(json, AuthContainer.class)
  }
}
