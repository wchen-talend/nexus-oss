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
package org.sonatype.nexus.ldap.internal.ssl;

import java.util.Map;

import org.sonatype.nexus.capability.support.CapabilityConfigurationSupport;

import com.google.common.collect.Maps;
import org.hibernate.validator.constraints.NotBlank;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration adapter for {@link LdapCapability}.
 *
 * @since 2.4
 */
public class LdapCapabilityConfiguration
    extends CapabilityConfigurationSupport
{

  public static final String LDAP_SERVER_ID = "ldapServer";

  @NotBlank
  private String ldapServerId;

  public LdapCapabilityConfiguration() {
    super();
  }

  public LdapCapabilityConfiguration(final Map<String, String> properties) {
    checkNotNull(properties);
    this.ldapServerId = properties.get(LDAP_SERVER_ID);
  }

  public String getLdapServerId() {
    return ldapServerId;
  }

  public LdapCapabilityConfiguration withLdapServerId(final String ldapServerId) {
    this.ldapServerId = ldapServerId;
    return this;
  }

  public Map<String, String> asMap() {
    final Map<String, String> props = Maps.newHashMap();
    props.put(LDAP_SERVER_ID, ldapServerId);
    return props;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "ldapServerId='" + ldapServerId +
        '}';
  }

}
