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
/*global Ext, NX*/

/**
 * LDAP Server "Connection" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.ldap.LdapServerConnectionForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-ldapserver-connection-form',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  items: { xtype: 'nx-coreui-ldapserver-connection-fieldset' },

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.editableCondition = me.editableCondition || NX.Conditions.isPermitted('nexus:ldap:update');

    me.callParent(arguments);

    me.getDockedItems('toolbar[dock="bottom"]')[0].add(
        { xtype: 'button', text: NX.I18n.get('ADMIN_LDAP_CONNECTION_VERIFY_BUTTON'), formBind: true, action: 'verifyconnection' }
    );
  }

});
