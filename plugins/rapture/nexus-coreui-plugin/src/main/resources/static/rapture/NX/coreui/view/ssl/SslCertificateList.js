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
 * Ssl Certificate grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.ssl.SslCertificateList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-sslcertificate-list',
  requires: [
    'NX.Icons',
    'NX.I18n'
  ],

  config: {
    stateful: true,
    stateId: 'nx-coreui-sslcertificate-list'
  },

  store: 'SslCertificate',

  columns: [
    {
      xtype: 'nx-iconcolumn',
      width: 36,
      iconVariant: 'x16',
      iconName: function () {
        return 'sslcertificate-default';
      }
    },
    {
      header: NX.I18n.get('ADMIN_SSL_LIST_NAME_COLUMN'),
      dataIndex: 'subjectCommonName',
      stateId: 'subjectCommonName',
      flex: 1
    },
    {
      header: NX.I18n.get('ADMIN_SSL_LIST_TO_COLUMN'),
      dataIndex: 'subjectOrganization',
      stateId: 'subjectOrganization',
      flex: 1
    },
    {
      header: NX.I18n.get('ADMIN_SSL_LIST_BY_COLUMN'),
      dataIndex: 'issuerOrganization',
      stateId: 'issuerOrganization',
      flex: 1
    },
    {
      header: NX.I18n.get('ADMIN_SSL_LIST_FINGERPRINT_COLUMN'),
      dataIndex: 'fingerprint',
      stateId: 'fingerprint',
      flex: 1
    }
  ],

  viewConfig: {
    emptyText: NX.I18n.get('ADMIN_SSL_LIST_EMPTY_STATE'),
    deferEmptyText: false
  },

  plugins: [
    { ptype: 'gridfilterbox', emptyText: NX.I18n.get('ADMIN_SSL_LIST_FILTER_ERROR') }
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.dockedItems = [{
      xtype: 'toolbar',
      dock: 'top',
      cls: 'nx-actions nx-borderless',
      items: [
        {
          xtype: 'button',
          text: NX.I18n.get('ADMIN_SSL_LIST_NEW_BUTTON'),
          glyph: 'xf055@FontAwesome' /* fa-plus-circle */,
          action: 'new',
          disabled: true,
          menu: [
            {
              text: NX.I18n.get('ADMIN_SSL_LIST_LOAD_BUTTON'),
              action: 'newfromserver',
              iconCls: NX.Icons.cls('sslcertificate-add-by-server', 'x16')
            },
            {
              text: NX.I18n.get('ADMIN_SSL_LIST_PASTE_BUTTON'),
              action: 'newfrompem',
              iconCls: NX.Icons.cls('sslcertificate-add-by-pem', 'x16')
            }
          ]
        }
      ]
    }];

    me.callParent();
  }
});
