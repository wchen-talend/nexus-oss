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
 * LDAP Server grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.ldap.LdapServerList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-ldapserver-list',
  requires: [
    'NX.I18n'
  ],

  config: {
    stateful: true,
    stateId: 'nx-coreui-ldapserver-list'
  },

  store: 'LdapServer',

  columns: [
    {
      xtype: 'nx-iconcolumn',
      width: 36,
      iconVariant: 'x16',
      iconName: function () {
        return 'ldapserver-default';
      }
    },
    { header: NX.I18n.get('ADMIN_LDAP_LIST_ORDER_COLUMN'), dataIndex: 'order', stateId: 'order', width: 80 },
    { header: NX.I18n.get('ADMIN_LDAP_LIST_NAME_COLUMN'), dataIndex: 'name', stateId: 'name', flex: 1 },
    { header: NX.I18n.get('ADMIN_LDAP_LIST_URL_COLUMN'), dataIndex: 'url', stateId: 'url', flex: 1 }
  ],

  viewConfig: {
    emptyText: NX.I18n.get('ADMIN_LDAP_LIST_EMPTY_STATE'),
    deferEmptyText: false
  },

  dockedItems: [{
    xtype: 'toolbar',
    dock: 'top',
    cls: 'nx-actions nx-borderless',
    items: [
      {
        xtype: 'button',
        text: NX.I18n.get('ADMIN_LDAP_LIST_NEW_BUTTON'),
        glyph: 'xf055@FontAwesome' /* fa-plus-circle */,
        action: 'new',
        disabled: true
      },
      {
        xtype: 'button',
        text: NX.I18n.get('ADMIN_LDAP_LIST_ORDER_BUTTON'),
        glyph: 'xf162@FontAwesome' /* fa-sort-numeric-asc */,
        action: 'changeorder',
        disabled: true
      },
      {
        xtype: 'button',
        text: NX.I18n.get('ADMIN_LDAP_LIST_CACHE_BUTTON'),
        glyph: 'xf014@FontAwesome' /* fa-trash-o */,
        action: 'clearcache',
        disabled: true
      }
    ]
  }],

  plugins: [
    { ptype: 'gridfilterbox', emptyText: NX.I18n.get('ADMIN_LDAP_LIST_FILTER_ERROR') }
  ]

});
