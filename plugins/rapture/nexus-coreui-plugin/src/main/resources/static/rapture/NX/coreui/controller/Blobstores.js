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
 * Blobstores controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Blobstores', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Dialogs',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n'
  ],

  masters: 'nx-coreui-blobstore-list',

  models: [
    'Blobstore'
  ],
  stores: [
    'NX.coreui.store.Blobstore',
    'BlobstoreType'
  ],
  views: [
    'blobstore.BlobstoreAdd',
    'blobstore.BlobstoreFeature',
    'blobstore.BlobstoreList',
    'blobstore.BlobstoreSettings',
    'blobstore.BlobstoreSettingsForm'
  ],
  refs: [
    { ref: 'feature', selector: 'nx-coreui-blobstore-feature' },
    { ref: 'list', selector: 'nx-coreui-blobstore-list' },
    { ref: 'settings', selector: 'nx-coreui-blobstore-feature nx-coreui-blobstore-settings' }
  ],
  icons: {
    'blobstore-default': {
      file: 'drive_network.png',
      variants: ['x16', 'x32']
    }
  },
  features: {
    mode: 'admin',
    path: '/Repository/Blobstores',
    text: NX.I18n.get('ADMIN_BLOBSTORES_TITLE'),
    description: NX.I18n.get('ADMIN_BLOBSTORES_SUBTITLE'),
    view: { xtype: 'nx-coreui-blobstore-feature' },
    iconConfig: {
      file: 'drive_network.png',
      variants: ['x16', 'x32']
    },
    visible: function() {
      return NX.Permissions.check('nexus:blobstores:read') && NX.State.getUser();
    }
  },
  permission: 'nexus:blobstores',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadRecipe
        }
      },
      component: {
        'nx-coreui-blobstore-list': {
          beforerender: me.loadRecipe
        },
        'nx-coreui-blobstore-list button[action=new]': {
          click: me.showAddWindow
        },
        'nx-coreui-blobstore-settings-form': {
          submitted: me.onSettingsSubmitted
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

  /**
   * @override
   */
  onSelection: function(list, model) {
    var me = this;

    if (Ext.isDefined(model)) {
      me.getSettings().loadRecord(model);
    }
  },

  /**
   * @private
   */
  showAddWindow: function() {
    var me = this,
        feature = me.getFeature();

    // Show the first panel in the create wizard, and set the breadcrumb
    feature.setItemName(1, NX.I18n.get('ADMIN_BLOBSTORES_CREATE_TITLE'));
    me.loadCreateWizard(1, true, Ext.create('widget.nx-coreui-blobstore-add'));
  },

  /**
   * @private
   */
  loadRecipe: function() {
    var me = this,
        list = me.getList();

    if (list) {
      me.getBlobstoreTypeStore().load();
    }
  },

  /**
   * @private
   */
  onSettingsSubmitted: function(form, action) {
    var me = this,
        win = form.up('nx-coreui-blobstore-add');

    if (win) {
      me.loadStoreAndSelect(action.result.data.id, false);
    }
    else {
      me.loadStore(Ext.emptyFn);
    }
  },

  /**
   * @private
   */
  deleteModel: function(model) {
    var me = this,
        description = me.getDescription(model);

    NX.direct.coreui_Blobstore.remove(model.getId(), function(response) {
      me.loadStore(Ext.emptyFn);
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({ text: 'Blobstore deleted: ' + description, type: 'success' });
      }
    });
  }

});
