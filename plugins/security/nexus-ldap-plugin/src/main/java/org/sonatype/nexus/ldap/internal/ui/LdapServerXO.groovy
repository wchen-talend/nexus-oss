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
package org.sonatype.nexus.ldap.internal.ui

import groovy.transform.ToString
import org.hibernate.validator.constraints.NotEmpty

import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

/**
 * LDAP Server exchange object.
 *
 * @since 3.0
 */
@ToString(includePackage = false, includeNames = true)
class LdapServerXO
extends LdapServerConnectionXO
{

  String userBaseDn
  Boolean userSubtree

  @NotEmpty
  String userObjectClass

  String userLdapFilter

  @NotEmpty
  String userIdAttribute

  @NotEmpty
  String userRealNameAttribute

  @NotEmpty
  String userEmailAddressAttribute

  String userPasswordAttribute

  Boolean ldapGroupsAsRoles

  String groupType

  String groupBaseDn

  Boolean groupSubtree

  @NotEmpty(groups = GroupStatic)
  String groupObjectClass

  @NotEmpty(groups = GroupStatic)
  String groupIdAttribute

  @NotEmpty(groups = GroupStatic)
  String groupMemberAttribute

  @NotEmpty(groups = GroupStatic)
  String groupMemberFormat

  @NotEmpty(groups = GroupStatic)
  String userMemberOfAttribute

  public interface GroupDynamic
  {}

  public interface GroupStatic
  {}

}
