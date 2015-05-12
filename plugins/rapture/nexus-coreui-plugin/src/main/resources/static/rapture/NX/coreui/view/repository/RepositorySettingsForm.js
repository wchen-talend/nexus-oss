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
 * Repository "Settings" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.RepositorySettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-repository-settings-form',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  api: {
    submit: 'NX.direct.coreui_Repository.update'
  },
  settingsFormSuccessMessage: function(data) {
    return NX.I18n.get('ADMIN_REPOSITORIES_UPDATE_SUCCESS') + data['name'];
  },

  editableMarker: NX.I18n.get('ADMIN_REPOSITORIES_UPDATE_ERROR'),

  initComponent: function() {
    var me = this,
        permittedCondition;

    if (!me.editableCondition) {
      me.editableCondition = NX.Conditions.and(
          permittedCondition = NX.Conditions.isPermitted('nexus:repository-admin:*:*', 'edit'),
          NX.Conditions.formHasRecord('nx-coreui-repository-settings-form', function(model) {
            var permission = 'nexus:repository-admin:' + model.get('format') + ':' + model.get('name');
            permittedCondition.name = permission;
            permittedCondition.evaluate();
            return true;
          })
      );
    }

    me.items = me.items || [];
    Ext.Array.insert(me.items, 0, [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section nx-no-title',

        items: [
          {
            xtype: 'textfield',
            cls: 'nx-no-border',
            name: 'name',
            itemId: 'name',
            labelAlign: 'left',
            fieldLabel: NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_NAME'),
            readOnly: true,
            vtype: 'nx-name'
          },
          {
            xtype: 'textfield',
            cls: 'nx-no-border',
            name: 'format',
            itemId: 'format',
            labelAlign: 'left',
            fieldLabel: NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_FORMAT'),
            allowBlank: true,
            readOnly: true
          },
          {
            xtype: 'textfield',
            cls: 'nx-no-border',
            name: 'type',
            itemId: 'type',
            labelAlign: 'left',
            fieldLabel: NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_TYPE'),
            allowBlank: true,
            readOnly: true
          },

          {
            xtype: 'textfield',
            cls: 'nx-no-border',
            name: 'url',
            itemId: 'url',
            labelAlign: 'left',
            fieldLabel: NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_URL'),
            allowBlank: true,
            readOnly: true
          },
          {
            xtype: 'checkbox',
            name: 'online',
            labelAlign: 'left',
            fieldLabel: NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_ONLINE'),
            helpText: NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_ONLINE_HELP'),
            value: true
          }
        ]
      }
    ]);

    me.callParent(arguments);

    //map repository attributes raw map structure to/from a flattened representation
    Ext.override(me.getForm(), {
      getValues: function() {
        var processed = { attributes: {} },
            values = this.callParent(arguments);

        Ext.Object.each(values, function(key, value) {
          var segments = key.split('.'),
              parent = processed;

          Ext.each(segments, function(segment, pos) {
            if (pos === segments.length - 1) {
              parent[segment] = value;
            }
            else {
              if (!parent[segment]) {
                parent[segment] = {};
              }
              parent = parent[segment];
            }
          });
        });

        return processed;
      },

      setValues: function(values) {
        var process = function(child, prefix) {
              Ext.Object.each(child, function(key, value) {
                var newPrefix = (prefix ? prefix + '.' : '') + key;
                if (Ext.isObject(value)) {
                  process(value, newPrefix);
                }
                else {
                  values[newPrefix] = value;
                }
              });
            };

        process(values);

        this.callParent(arguments);
      }
    });
  }

});
