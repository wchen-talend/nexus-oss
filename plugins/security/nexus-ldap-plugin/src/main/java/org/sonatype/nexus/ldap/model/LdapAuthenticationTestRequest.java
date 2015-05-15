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

import javax.xml.bind.annotation.XmlRootElement;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Request wrapper object that contains the connection info
 */
@XStreamAlias(value = "connectionInfoTest")
@XmlRootElement(name = "connectionInfoTest")
public class LdapAuthenticationTestRequest
{

  private LdapConnectionInfoDTO data;

  /**
   * Get the ldap connection info.
   */
  public LdapConnectionInfoDTO getData() {
    return data;
  }

  /**
   * Set the ldap connection info.
   */
  public void setData(LdapConnectionInfoDTO data) {
    this.data = data;
  }


}
