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
 * Add blobstore window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.blobstore.BlobstoreAdd', {
  extend: 'NX.view.AddPanel',
  alias: 'widget.nx-coreui-blobstore-add',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  defaultFocus: 'type',

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.settingsForm = {
      xtype: 'nx-coreui-blobstore-settings-form',
      api: {
        submit: 'NX.direct.coreui_Blobstore.create'
      },
      settingsFormSuccessMessage: function(data) {
        return NX.I18n.get('ADMIN_BLOBSTORES_CREATE_SUCCESS') + data['name'];
      },
      editableCondition: NX.Conditions.isPermitted('nexus:blobstores:create'),
      editableMarker: NX.I18n.get('ADMIN_BLOBSTORES_CREATE_ERROR'),

      buttons: [
        { text: NX.I18n.get('ADMIN_BLOBSTORES_LIST_NEW_BUTTON'), action: 'add', formBind: true, ui: 'nx-primary' },
        { text: NX.I18n.get('GLOBAL_DIALOG_ADD_CANCEL_BUTTON'), handler: function() {
          this.up('nx-drilldown').showChild(0, true);
        }}
      ]
    };

    me.callParent();

    me.down('#name').setReadOnly(false);
    me.down('nx-settingsform').add(0, {
      xtype: 'combo',
      name: 'type',
      itemId: 'type',
      fieldLabel: NX.I18n.get('ADMIN_BLOBSTORES_SETTINGS_TYPE'),
      emptyText: NX.I18n.get('ADMIN_BLOBSTORES_SETTINGS_TYPE_PLACEHOLDER'),
      editable: false,
      store: 'BlobstoreType',
      queryMode: 'local',
      displayField: 'name',
      valueField: 'id'
    });
  }
});
