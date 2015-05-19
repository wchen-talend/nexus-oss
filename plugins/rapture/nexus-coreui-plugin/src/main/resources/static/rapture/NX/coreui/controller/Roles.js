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
 * Roles controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Roles', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Conditions',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n'
  ],

  masters: 'nx-coreui-role-list',

  models: [
    'Role'
  ],
  stores: [
    'Role',
    'RoleSource',
    'RoleBySource',
    'Privilege'
  ],
  views: [
    'role.RoleAdd',
    'role.RoleFeature',
    'role.RoleList',
    'role.RoleSettings',
    'role.RoleSettingsForm',
    'role.RoleTree'
  ],
  refs: [
    { ref: 'feature', selector: 'nx-coreui-role-feature' },
    { ref: 'list', selector: 'nx-coreui-role-list' },
    { ref: 'settings', selector: 'nx-coreui-role-feature nx-coreui-role-settings' },
    { ref: 'privilegeTrace', selector: 'nx-coreui-role-feature nx-coreui-privilege-trace' },
    { ref: 'roleTree', selector: 'nx-coreui-role-feature nx-coreui-role-tree' }
  ],
  icons: {
    'role-default': {
      file: 'user_policeman.png',
      variants: ['x16', 'x32']
    },
    'role-externalmapping': {
      file: 'shield.png',
      variants: ['x16', 'x32']
    }
  },
  features: {
    mode: 'admin',
    path: '/Security/Roles',
    text: NX.I18n.get('ADMIN_ROLES_TITLE'),
    description: NX.I18n.get('ADMIN_ROLES_SUBTITLE'),
    view: { xtype: 'nx-coreui-role-feature' },
    iconConfig: {
      file: 'user_policeman.png',
      variants: ['x16', 'x32']
    },
    visible: function() {
      return NX.Permissions.check('nexus:roles:read');
    },
    weight: 20
  },
  permission: 'nexus:roles',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadRelatedStores
        }
      },
      store: {
        '#RoleSource': {
          load: me.onRoleSourceLoad
        }
      },
      component: {
        'nx-coreui-role-list': {
          beforerender: me.loadRelatedStores
        },
        'nx-coreui-role-list menuitem[action=newrole]': {
          click: me.showAddWindowRole
        },
        'nx-coreui-role-list menuitem[action=newmapping]': {
          click: me.showAddWindowMapping
        },
        'nx-coreui-role-settings-form': {
          submitted: me.onSettingsSubmitted
        },
        'nx-coreui-role-feature nx-coreui-privilege-trace': {
          activate: me.onPrivilegeTraceActivate
        },
        'nx-coreui-role-feature nx-coreui-role-tree': {
          activate: me.onRoleTreeActivate
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function(model) {
    return model.get('name');
  },

  onSelection: function(list, model) {
    var me = this;

    if (Ext.isDefined(model)) {
      me.getSettings().loadRecord(model);
      me.getPrivilegeTrace().loadRecord(model.data);
      me.getRoleTree().loadRecord(model.data);
    }
  },

  /**
   * @private
   */
  showAddWindowRole: function() {
    var me = this,
      feature = me.getFeature();

    // Show the first panel in the create wizard, and set the breadcrumb
    feature.setItemName(1, NX.I18n.get('ADMIN_ROLES_CREATE_TITLE'));
    me.loadCreateWizard(1, true, Ext.create('widget.nx-coreui-role-add'));
  },

  /**
   * @private
   */
  showAddWindowMapping: function(menuItem) {
    var me = this,
      feature = me.getFeature();

    me.getRoleBySourceStore().load({
      params: {
        source: menuItem.source
      }
    });

    // Show the first panel in the create wizard, and set the breadcrumb
    feature.setItemName(1, NX.I18n.get('ADMIN_ROLES_CREATE_TITLE'));
    me.loadCreateWizard(1, true, Ext.create('widget.nx-coreui-role-add', { source: menuItem.source }));
  },

  /**
   * @private
   * (Re)load related stores.
   */
  loadRelatedStores: function() {
    var me = this,
        list = me.getList();

    if (list) {
      me.getRoleSourceStore().load();
      me.getPrivilegeStore().load();
      me.getSettings().down('#roles').getStore().load();
    }
  },

  /**
   * @private
   * (Re)create external role mapping entries.
   */
  onRoleSourceLoad: function(store) {
    var me = this,
        list = me.getList(),
        newButton, menuItems = [];

    if (list) {
      newButton = list.down('button[action=new]');
      if (newButton.menu.items.length > 1) {
        newButton.menu.remove(1);
      }
      store.each(function(source) {
        menuItems.push({
          text: source.get('name'),
          iconCls: NX.Icons.cls(source.getId().toLowerCase() + '-security-source', 'x16'),
          action: 'newmapping',
          source: source.getId()
        });
      });
      newButton.menu.add({
        text: NX.I18n.get('ADMIN_ROLES_LIST_EXTERNAL_ROLE_ITEM'),
        menu: menuItems,
        iconCls: NX.Icons.cls('role-externalmapping', 'x16')
      });
    }
  },

  /**
   * @protected
   * Enable 'Delete' when user has 'delete' permission and role is not read only.
   */
  bindDeleteButton: function(button) {
    var me = this;
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(me.permission + ':delete'),
            NX.Conditions.gridHasSelection(me.masters[0], function(model) {
              return !model.get('readOnly');
            })
        ),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @private
   */
  onPrivilegeTraceActivate: function(panel) {
    var me = this;

    panel.loadRecord({
      roles: me.getSettings().down('#roles').getValue(),
      privileges: me.getSettings().down('#privileges').getValue()
    });
  },

  /**
   * @private
   */
  onRoleTreeActivate: function(panel) {
    var me = this;

    panel.loadRecord({
      roles: me.getSettings().down('#roles').getValue(),
      privileges: me.getSettings().down('#privileges').getValue()
    });
  },

  /**
   * @private
   */
  onSettingsSubmitted: function(form, action) {
    var me = this,
        win = form.up('nx-coreui-role-add');

    if (win) {
      me.loadStoreAndSelect(action.result.data.id, false);
    } else {
      me.loadStore(Ext.emptyFn);
    }
  },

  /**
   * @private
   * @override
   * Deletes a role.
   * @param model role to be deleted
   */
  deleteModel: function(model) {
    var me = this,
        description = me.getDescription(model);

    NX.direct.coreui_Role.remove(model.getId(), function(response) {
      me.loadStore();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({
          text: NX.I18n.format('ADMIN_ROLES_DETAILS_DELETE_ROLE', description), type: 'success'
        });
      }
    });
  }

});
