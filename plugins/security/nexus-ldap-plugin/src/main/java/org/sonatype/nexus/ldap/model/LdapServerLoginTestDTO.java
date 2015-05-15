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
package org.sonatype.nexus.ldap.model;

import javax.xml.bind.annotation.XmlType;

/**
 * Ldap Server login test parameters.
 */
@XmlType(name = "ldapServerLoginTest")
public class LdapServerLoginTestDTO
{
  private String username;

  private String password;

  private LdapServerConfigurationDTO configuration;

  /**
   * Get the ldap server configuration.
   */
  public LdapServerConfigurationDTO getConfiguration() {
    return configuration;
  }

  /**
   * Set the ldap server configuration
   */
  public void setConfiguration(LdapServerConfigurationDTO configuration) {
    this.configuration = configuration;
  }

  /**
   * Get the username to test.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Set the username to test.
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Get the password to test.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Set the password to test.
   */
  public void setPassword(String password) {
    this.password = password;
  }
}
