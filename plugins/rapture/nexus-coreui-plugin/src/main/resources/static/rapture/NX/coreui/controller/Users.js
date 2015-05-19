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
 * Users controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Users', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Conditions',
    'NX.State',
    'NX.Permissions',
    'NX.Security',
    'NX.Icons',
    'NX.Messages',
    'NX.Dialogs',
    'NX.I18n'
  ],

  masters: 'nx-coreui-user-list',

  models: [
    'User'
  ],
  stores: [
    'User',
    'UserSource',
    'Role'
  ],
  views: [
    'user.UserAccount',
    'user.UserAdd',
    'user.UserChangePassword',
    'user.UserFeature',
    'user.UserList',
    'user.UserSearchBox',
    'user.UserSettings',
    'user.UserSettingsForm',
    'user.UserSettingsExternal',
    'user.UserSettingsExternalForm'
  ],
  refs: [
    { ref: 'feature', selector: 'nx-coreui-user-feature' },
    { ref: 'list', selector: 'nx-coreui-user-list' },
    { ref: 'userSearchBox', selector: 'nx-coreui-user-list nx-coreui-user-searchbox' },
    { ref: 'settings', selector: 'nx-coreui-user-feature nx-coreui-user-settings' },
    { ref: 'externalSettings', selector: 'nx-coreui-user-feature nx-coreui-user-settings-external' },
    { ref: 'privilegeTrace', selector: 'nx-coreui-user-feature nx-coreui-privilege-trace' },
    { ref: 'roleTree', selector: 'nx-coreui-user-feature nx-coreui-role-tree' }
  ],
  icons: {
    'user-default': {
      file: 'user.png',
      variants: ['x16', 'x32']
    },
    'default-security-source': {
      file: 'user.png',
      variants: ['x16']
    },
    'allconfigured-security-source': {
      file: 'user.png',
      variants: ['x16']
    }
  },
  features: [
    {
      mode: 'admin',
      path: '/Security/Users',
      text: NX.I18n.get('ADMIN_USERS_TITLE'),
      description: NX.I18n.get('ADMIN_USERS_SUBTITLE'),
      view: { xtype: 'nx-coreui-user-feature' },
      iconConfig: {
        file: 'group.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Permissions.check('nexus:users:read');
      },
      weight: 30
    },
    {
      mode: 'user',
      path: '/Account',
      text: NX.I18n.get('USER_ACCOUNT_TITLE'),
      description: NX.I18n.get('USER_ACCOUNT_SUBTITLE'),
      view: { xtype: 'nx-coreui-user-account' },
      iconConfig: {
        file: 'user.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Security.hasUser();
      }
    }
  ],
  permission: 'nexus:users',

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
        '#UserSource': {
          load: me.onUserSourceLoad
        }
      },
      component: {
        'nx-coreui-user-list': {
          beforerender: me.loadRelatedStores
        },
        'nx-coreui-user-list button[action=new]': {
          click: me.showAddWindow
        },
        'nx-coreui-user-settings-form': {
          submitted: me.onSettingsSubmitted
        },
        'nx-coreui-user-settings-external-form': {
          submitted: me.onSettingsSubmitted
        },
        'nx-coreui-user-list menuitem[action=filter]': {
          click: me.onSourceChanged
        },
        'nx-coreui-user-feature button[action=more]': {
          afterrender: me.bindMoreButton
        },
        'nx-coreui-user-feature menuitem[action=setpassword]': {
          click: me.showChangePasswordWindowForSelection
        },
        'nx-coreui-user-list nx-coreui-user-searchbox': {
          search: me.loadStore,
          searchcleared: me.onSearchCleared
        },
        'nx-coreui-user-account button[action=changepassword]': {
          click: me.showChangePasswordWindowForUserAccount,
          afterrender: me.bindChangePasswordButton
        },
        'nx-coreui-user-changepassword button[action=changepassword]': {
          click: me.changePassword
        },
        'nx-coreui-user-feature nx-coreui-privilege-trace': {
          activate: me.onPrivilegeTraceActivate
        },
        'nx-coreui-user-feature nx-coreui-role-tree': {
          activate: me.onRoleTreeActivate
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function(model) {
    return model.get('firstName') + ' ' + model.get('lastName');
  },

  /**
   * @override
   */
  onSelection: function(list, model) {
    var me = this,
        settingsPanel = me.getSettings(),
        externalSettingsPanel = me.getExternalSettings();

    if (Ext.isDefined(model)) {
      if (model.get('external')) {
        if (!externalSettingsPanel) {
          me.getFeature().addTab({ xtype: 'nx-coreui-user-settings-external', title: 'Settings', weight: 10 });
          externalSettingsPanel = me.getExternalSettings();
        }
        externalSettingsPanel.loadRecord(model)
      }
      else {
        if (!settingsPanel) {
          me.getFeature().addTab({ xtype: 'nx-coreui-user-settings', title: 'Settings', weight: 10 });
          settingsPanel = me.getSettings();
        }
        settingsPanel.loadRecord(model)
      }

      if (model.get('external')) {
        if (settingsPanel) {
          me.getFeature().removeTab(settingsPanel);
        }
      }
      else {
        if (externalSettingsPanel) {
          me.getFeature().removeTab(externalSettingsPanel);
        }
      }
      me.getPrivilegeTrace().loadRecord(model.data);
      me.getRoleTree().loadRecord(model.data);
    }
  },

  /**
   * @private
   */
  showAddWindow: function() {
    var me = this,
      feature = me.getFeature();

    // Show the first panel in the create wizard, and set the breadcrumb
    feature.setItemName(1, NX.I18n.get('ADMIN_USERS_CREATE_TITLE'));
    me.loadCreateWizard(1, true, Ext.create('widget.nx-coreui-user-add'));
  },

  /**
   * @private
   */
  showChangePasswordWindowForSelection: function() {
    var me = this,
        list = me.getList(),
        userId = list.getSelectionModel().getSelection()[0].getId();

    NX.Security.doWithAuthenticationToken(
        'Changing password requires validation of your credentials.',
        {
          success: function(authToken) {
            Ext.widget('nx-coreui-user-changepassword', { userId: userId, authToken: authToken });
          }
        }
    );
  },

  /**
   * @private
   */
  showChangePasswordWindowForUserAccount: function(button) {
    var userId = button.up('form').down('#userId').getValue();

    NX.Security.doWithAuthenticationToken(
        'Changing password requires validation of your credentials.',
        {
          success: function(authToken) {
            Ext.widget('nx-coreui-user-changepassword', { userId: userId, authToken: authToken });
          }
        }
    );
  },

  /**
   * @private
   * Load store for selected source.
   */
  loadStore: function(cb) {
    var me = this,
        list = me.getList(),
        userSourceButton;

    if (list) {
      userSourceButton = list.down('button[action=filter]');
      if (!userSourceButton.sourceId) {
        userSourceButton.sourceId = 'default';
      }
      me.updateEmptyText();
      me.getUserStore().load({
        params: {
          filter: [
            { property: 'source', value: userSourceButton.sourceId },
            { property: 'userId', value: me.getUserSearchBox().getValue() }
          ]
        },
        callback: cb
      });
    }
    me.callParent();  // triggers transition between list/detail view
  },

  /**
   * @private
   * (Re)load related stores.
   */
  loadRelatedStores: function() {
    var me = this,
        list = me.getList();

    if (list) {
      me.getUserSourceStore().load();
      me.getRoleStore().load();
    }
  },

  /**
   * @private
   * (Re)create user source filters.
   */
  onUserSourceLoad: function(store) {
    var me = this,
        list = me.getList(),
        userSourceButton;

    if (list) {
      userSourceButton = list.down('button[action=filter]');
      if (userSourceButton.menu.items.length > 1) {
        userSourceButton.menu.removeAll();
      }
      if (!userSourceButton.sourceId) {
        userSourceButton.sourceId = 'default';
      }
      store.each(function(source) {
        var iconCls = NX.Icons.cls(source.getId().toLowerCase() + '-security-source', 'x16');
        userSourceButton.menu.add({
          text: source.get('name'),
          iconCls: iconCls,
          group: 'usersource',
          checked: userSourceButton.sourceId === source.getId(),
          action: 'filter',
          source: source
        });
        if (userSourceButton.sourceId === source.getId()) {
          userSourceButton.setText(source.get('name'));
          userSourceButton.setIconCls(iconCls);
        }
      });
    }
  },

  /**
   * @private
   */
  onSearchCleared: function() {
    var me = this,
        list = me.getList(),
        userSourceButton = list.down('button[action=filter]');

    if (userSourceButton.sourceId === 'default') {
      me.loadStore();
    }
    else {
      me.updateEmptyText();
      me.getUserStore().removeAll();
    }
  },

  /**
   * @private
   */
  onSourceChanged: function(menuItem) {
    var me = this,
        list = me.getList(),
        userSourceButton = list.down('button[action=filter]');

    userSourceButton.setText(menuItem.source.get('name'));
    userSourceButton.setIconCls(menuItem.iconCls);
    userSourceButton.sourceId = menuItem.source.getId();

    me.getUserSearchBox().setValue(undefined);
    if (userSourceButton.sourceId === 'default') {
      me.loadStore();
    }
    else {
      me.updateEmptyText();
      me.getUserStore().removeAll();
    }
  },

  /**
   * @private
   * Update grid empty text based on source/user id from search box.
   */
  updateEmptyText: function() {
    var me = this,
        list = me.getList(),
        userSourceButton = list.down('button[action=filter]'),
        userId = me.getUserSearchBox().getValue(),
        emptyText;

    emptyText = '<div class="x-grid-empty">';
    if (userSourceButton.sourceId === 'default') {
      if (userId) {
        emptyText += 'No user matched query criteria "' + userId + '"';
      }
      else {
        emptyText += 'No users defined';
      }
    }
    else {
      emptyText += 'No ' + userSourceButton.getText() + ' user matched query criteria';
      if (userId) {
        emptyText += ' "' + userId + '"';
      }
    }
    emptyText += '</div>';

    list.getView().emptyText = emptyText;
  },

  /**
   * @private
   */
  onRoleTreeActivate: function(panel) {
    var me = this,
        settingPanel = me.getSettings();

    if (!settingPanel) {
      settingPanel = me.getExternalSettings();
    }

    if (settingPanel) {
      panel.loadRecord({
        roles: settingPanel.down('#roles').getValue()
      });
    }
  },

  /**
   * @private
   */
  onPrivilegeTraceActivate: function(panel) {
    var me = this,
        settingPanel = me.getSettings();

    if (!settingPanel) {
      settingPanel = me.getExternalSettings();
    }

    if (settingPanel) {
      panel.loadRecord({
        roles: settingPanel.down('#roles').getValue()
      });
    }
  },

  /**
   * @private
   */
  onSettingsSubmitted: function(form, action) {
    var me = this,
        win = form.up('nx-coreui-user-add');

    if (win) {
      me.loadStoreAndSelect(action.result.data.userId, false);
    } else {
      me.loadStore(Ext.emptyFn);
    }
  },

  /**
   * @protected
   * Enable 'Delete' when user has 'delete' permission and user is not read only and user is not the current signed on
   * used or the anonymous user.
   */
  bindDeleteButton: function(button) {
    var me = this;
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(me.permission + ':delete'),
            NX.Conditions.gridHasSelection(me.masters[0], function(model) {
              return !model.get('external')
                  && (model.getId() !== NX.State.getUser().id)
                  && (model.getId() !== NX.State.getValue('anonymousUsername'));
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
   * @override
   * Deletes a user.
   * @param model user to be deleted
   */
  deleteModel: function(model) {
    var me = this,
        description = me.getDescription(model);

    NX.direct.coreui_User.remove(model.getId(), model.get('realm'), function(response) {
      me.loadStore();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({
          text: NX.I18n.format('ADMIN_USERS_DETAILS_DELETE_SUCCESS', description), type: 'success'
        });
      }
    });
  },

  /**
   * @protected
   * Enable 'More' actions as appropriate for user's permissions.
   */
  bindMoreButton: function(button) {
    var me = this,
        setMenuItem = button.down('menuitem[action=setpassword]');

    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:userschangepw:create'),
            NX.Conditions.gridHasSelection(me.masters[0], function(model) {
              return !model.get('external') && model.getId() !== NX.State.getValue('anonymousUsername');
            })
        ),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );

    setMenuItem.mon(
        NX.Conditions.isPermitted('nexus:userschangepw:create'),
        {
          satisfied: setMenuItem.enable,
          unsatisfied: setMenuItem.disable,
          scope: setMenuItem
        }
    );
  },

  /**
   * @override
   * @private
   * Enable 'Change Password' when user has 'nexus:userschangepw:create' permission.
   */
  bindChangePasswordButton: function(button) {
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:userschangepw:create')
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
  changePassword: function(button) {
    var win = button.up('window'),
        password = button.up('form').down('#password').getValue();

    NX.direct.coreui_User.changePassword(win.authToken, win.userId, password, function(response) {
      if (Ext.isObject(response) && response.success) {
        win.close();
        NX.Messages.add({ text: NX.I18n.get('ADMIN_USERS_DETAILS_CHANGE_SUCCESS'), type: 'success' });
      }
    });
  }

});
