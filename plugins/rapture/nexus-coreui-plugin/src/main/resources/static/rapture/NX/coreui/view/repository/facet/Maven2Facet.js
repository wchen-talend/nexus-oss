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
 * Configuration specific to Maven repositories.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.Maven2Facet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-maven2-facet',
  requires: [
    'NX.I18n'
  ],

  defaults: {
    allowBlank: false,
    queryMode: 'local',
    itemCls: 'required-field'
  },

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('ADMIN_REPOSITORIES_DETAILS_SETTINGS_MAVEN2_FACET'),

        items: [
          {
            xtype: 'combo',
            name: 'attributes.maven.versionPolicy',
            itemId: 'versionPolicy',
            fieldLabel: NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_VERSION_POLICY'),
            helpText: NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_VERSION_POLICY_HELP'),
            emptyText: NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_VERSION_POLICY_PLACEHOLDER'),
            editable: false,
            disabledCls: 'nx-combo-disabled',
            store: [
              ['RELEASE', NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_VERSION_POLICY_RELEASE_ITEM')],
              ['SNAPSHOT', NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_VERSION_POLICY_SNAPSHOT_ITEM')],
              ['MIXED', NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_VERSION_POLICY_MIXED_ITEM')]
            ],
            value: 'RELEASE',
            readOnlyOnUpdate: true
          },
          {
            xtype: 'checkbox',
            name: 'attributes.maven.strictContentTypeValidation',
            fieldLabel: NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_CONTENT_TYPE_VALIDATION'),
            value: true
          }
        ]
      }
    ];

    me.callParent(arguments);
  }

});
