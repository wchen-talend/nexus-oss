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
 * Role grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.role.RoleList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-role-list',
  requires: [
    'NX.Icons',
    'NX.I18n'
  ],

  config: {
    stateful: true,
    stateId: 'nx-coreui-role-list'
  },

  store: 'Role',

  columns: [
    {
      xtype: 'nx-iconcolumn',
      width: 36,
      iconVariant: 'x16',
      iconName: function () {
        return 'role-default';
      }
    },
    {header: NX.I18n.get('ADMIN_ROLES_LIST_NAME_COLUMN'), dataIndex: 'name', stateId: 'name', flex: 1},
    {header: NX.I18n.get('ADMIN_ROLES_LIST_SOURCE_COLUMN'), dataIndex: 'source', stateId: 'source'},
    {header: NX.I18n.get('ADMIN_ROLES_LIST_DESCRIPTION_COLUMN'), dataIndex: 'description', stateId: 'description', flex: 1}
  ],

  viewConfig: {
    emptyText: NX.I18n.get('ADMIN_ROLES_LIST_EMPTY_STATE'),
    deferEmptyText: false
  },

  plugins: [
    { ptype: 'gridfilterbox', emptyText: NX.I18n.get('ADMIN_ROLES_LIST_FILTER_ERROR') }
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
        { xtype: 'button', text: NX.I18n.get('ADMIN_ROLES_LIST_NEW_BUTTON'), glyph: 'xf055@FontAwesome' /* fa-plus-circle */, action: 'new', disabled: true,
          menu: [
            { text: NX.I18n.get('ADMIN_ROLES_LIST_NEXUS_ROLE_ITEM'), action: 'newrole', iconCls: NX.Icons.cls('role-default', 'x16') }
          ]
        }
      ]
    }];

    me.callParent();
  }
});
