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
 * Logging configuration controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Loggers', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Conditions',
    'NX.Dialogs',
    'NX.Permissions',
    'NX.Messages',
    'NX.I18n'
  ],

  stores: [
    'Logger'
  ],
  models: [
    'Logger'
  ],
  views: [
    'logging.LoggerAdd',
    'logging.LoggerList',
    'logging.LoggerFeature'
  ],
  refs: [
    { ref: 'feature', selector: 'nx-coreui-logging-feature' },
    { ref: 'list', selector: 'nx-coreui-logger-list' }
  ],

  init: function () {
    var me = this;

    me.callParent();

    me.getApplication().getIconController().addIcons({
      'logger-default': {
        file: 'book.png',
        variants: ['x16', 'x32']
      }
    });

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/Support/Logging',
      text: NX.I18n.get('ADMIN_LOGGING_TITLE'),
      description: NX.I18n.get('ADMIN_LOGGING_SUBTITLE'),
      view: { xtype: 'nx-coreui-logging-feature' },
      iconConfig: {
        file: 'book.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:logging:read');
      }
    }, me);

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStore
        }
      },
      store: {
        '#Logger': {
          write: me.onWrite
        }
      },
      component: {
        'nx-coreui-logger-list': {
          beforerender: me.loadStore,
          beforeedit: me.onBeforeEdit
        },
        'nx-coreui-logger-list button[action=new]': {
          afterrender: me.bindNewButton,
          click: me.showAddWindow
        },
        'nx-coreui-logger-list button[action=delete]': {
          afterrender: me.bindDeleteButton,
          click: me.removeLogger
        },
        'nx-coreui-logger-list button[action=reset]': {
          afterrender: me.bindResetButton,
          click: me.resetLoggers
        },
        'nx-coreui-logger-add button[action=add]': {
          click: me.addLogger
        }
      }
    });
  },

  /**
   * @private
   * Loads logger store.
   */
  loadStore: function () {
    var me = this,
        list = me.getList();

    if (list) {
      list.getStore().load();
    }
  },

  /**
   * @private
   * Shows success messages after records has been written (create/update).
   */
  onWrite: function (store, operation) {
    var me = this;

    me.loadStore();
    if (operation.success) {
      Ext.Array.each(operation.records, function (model) {
        NX.Messages.add({ text: NX.I18n.format('ADMIN_LOGGING_WRITE_SUCCESS', operation.action, model.get('name')), type: 'success' });
      });
    }
  },

  /**
   * @private
   * Shows add logger window.
   */
  showAddWindow: function (button) {
    var me = this,
      feature = me.getFeature();

    // Show the first panel in the create wizard, and set the breadcrumb
    feature.setItemName(1, NX.I18n.get('ADMIN_LOGGING_CREATE_TITLE'));
    me.loadCreateWizard(1, true, Ext.create('widget.nx-coreui-logger-add'));
  },

  /**
   * @private
   * Cancels edit when user does not have 'update' permission.
   */
  onBeforeEdit: function () {
    return NX.Permissions.check('nexus:logging:update');
  },

  /**
   * @private
   * Adds a logger to logger store.
   * @param {Ext.Button} button add button that triggered the action (from add window)
   */
  addLogger: function (button) {
    var me = this,
        win = button.up('window'),
        form = button.up('form').getForm(),
        store = me.getLoggerStore(),
        values = form.getFieldValues(),
        model = store.getById(values.name);

    if (model) {
      NX.Dialogs.askConfirmation(NX.I18n.get('ADMIN_LOGGING_UPDATE_TITLE'),
          NX.I18n.format('ADMIN_LOGGING_UPDATE_HELP', values.name, values.level),
          function () {
            model.set('level', values.level);
            me.loadView(null, null, true);
          }
      );
    }
    else {
      model = me.getLoggerModel().create(values);
      store.addSorted(model);
      me.loadView(null, null, true);
    }
  },

  /**                                \
   * @private
   * Remove selected logger (if any).
   */
  removeLogger: function () {
    var me = this,
        list = me.getList(),
        selection = list.getSelectionModel().getSelection();

    if (selection.length) {
      NX.Dialogs.askConfirmation(NX.I18n.get('ADMIN_LOGGING_DELETE_CONFIRM'), selection[0].get('name'), function () {
        NX.direct.logging_Loggers.remove(selection[0].getId(), function (response) {
          me.loadStore();
          if (Ext.isObject(response) && response.success) {
            NX.Messages.add({ text: NX.I18n.format('ADMIN_LOGGING_DELETE_SUCCESS', selection[0].get('name')), type: 'success' });
          }
        });
      });
    }
  },

  /**
   * @private
   * Resets all loggers to their default levels.
   */
  resetLoggers: function () {
    var me = this;

    NX.Dialogs.askConfirmation(NX.I18n.get('ADMIN_LOGGING_RESET_TITLE'), NX.I18n.get('ADMIN_LOGGING_RESET_HELP'), function () {
      NX.direct.logging_Loggers.reset(function (response) {
        me.loadStore();
        if (Ext.isObject(response) && response.success) {
          NX.Messages.add({ text: NX.I18n.get('ADMIN_LOGGING_RESET_SUCCESS'), type: 'success' });
        }
      });
    });
  },

  /**
   * @protected
   * Enable 'New' when user has 'update' permission.
   */
  bindNewButton: function (button) {
    button.mon(
        NX.Conditions.isPermitted('nexus:logging:update'),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @protected
   * Enable 'Delete' when user has 'delete' permission.
   */
  bindDeleteButton: function (button) {
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:logging:update'),
            NX.Conditions.gridHasSelection('nx-coreui-logger-list', function (model) {
              return model.get('name') !== 'ROOT';
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
   * @protected
   * Enable 'Reset' when user has 'update' permission.
   */
  bindResetButton: function (button) {
    button.mon(
        NX.Conditions.isPermitted('nexus:logging:update'),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  }

});
