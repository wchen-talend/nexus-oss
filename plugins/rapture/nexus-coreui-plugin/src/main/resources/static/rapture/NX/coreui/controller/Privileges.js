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
 * Privilege controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Privileges', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Conditions',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n'
  ],

  masters: 'nx-coreui-privilege-list',

  stores: [
    'Privilege'
  ],
  views: [
    'privilege.PrivilegeFeature',
    'privilege.PrivilegeList',
    'privilege.PrivilegeTrace'
  ],
  refs: [
    {ref: 'feature', selector: 'nx-coreui-privilege-feature'},
    {ref: 'list', selector: 'nx-coreui-privilege-list'},
    {ref: 'info', selector: 'nx-coreui-privilege-feature nx-info-panel'}
  ],
  icons: {
    'privilege-default': {
      file: 'medal_gold_red.png',
      variants: ['x16', 'x32']
    },
    'privilege-application': {
      file: 'medal_gold_green.png',
      variants: ['x16', 'x32']
    },
    'privilege-wildcard': {
      file: 'medal_gold_blue.png',
      variants: ['x16', 'x32']
    },
    'privilege-repository': {
      file: 'database.png',
      variants: ['x16', 'x32']
    },
    'privilege-repository-admin': {
      file: 'database_red.png',
      variants: ['x16', 'x32']
    },
    'privilege-repository-view': {
      file: 'database.png',
      variants: ['x16', 'x32']
    }
  },
  features: {
    mode: 'admin',
    path: '/Security/Privileges',
    text: NX.I18n.get('ADMIN_PRIVILEGES_TITLE'),
    description: NX.I18n.get('ADMIN_PRIVILEGES_SUBTITLE'),
    view: {xtype: 'nx-coreui-privilege-feature'},
    iconConfig: {
      file: 'medal_gold_green.png',
      variants: ['x16', 'x32']
    },
    visible: function () {
      return NX.Permissions.check('nexus:privileges:read');
    },
    weight: 10
  },

  permission: 'nexus:privileges',

  /**
   * @override
   */
  getDescription: function (model) {
    return model.get('name');
  },

  onSelection: function (list, model) {
    var me = this,
        info = {};

    if (Ext.isDefined(model)) {
      me.getFeature().setItemClass(1, NX.Icons.cls('privilege-' + model.get('type'), 'x16'));

      info[NX.I18n.get('ADMIN_PRIVILEGES_SUMMARY_ID')] = model.getId();
      info[NX.I18n.get('ADMIN_PRIVILEGES_SUMMARY_TYPE')] = model.get('type');
      info[NX.I18n.get('ADMIN_PRIVILEGES_SUMMARY_NAME')] = model.get('name');
      info[NX.I18n.get('ADMIN_PRIVILEGES_SUMMARY_DESCRIPTION')] = model.get('description');
      info[NX.I18n.get('ADMIN_PRIVILEGES_SUMMARY_PERMISSION')] = model.get('permission');

      Ext.iterate(model.get('properties'), function (key, value) {
        info[NX.I18n.format('ADMIN_PRIVILEGES_SUMMARY_PROPERTY', key)] = value;
      });

      me.getInfo().showInfo(info);
    }
  },

  /**
   * @protected
   * Enable 'Delete' when user has 'delete' permission and privilege is not read only.
   */
  bindDeleteButton: function (button) {
    var me = this;

    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(me.permission + ':delete'),
            NX.Conditions.gridHasSelection(me.masters[0], function (model) {
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
   * @override
   * Deletes a privilege.
   * @param model privilege to be deleted
   */
  deleteModel: function (model) {
    var me = this;

    NX.direct.coreui_Privilege.remove(model.getId(), function (response) {
      me.loadStore();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({
          text: NX.I18n.format('ADMIN_PRIVILEGES_DELETE_SUCCESS', model.get('name')),
          type: 'success'
        });
      }
    });
  }

});
