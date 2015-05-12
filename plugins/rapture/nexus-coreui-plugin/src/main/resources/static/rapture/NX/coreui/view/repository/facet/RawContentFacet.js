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
 * Configuration for content type validation of Repository content.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.facet.RawContentFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-content-rawcontent-facet',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: NX.I18n.get('ADMIN_REPOSITORIES_DETAILS_SETTINGS_RAW_FACET'),

        items: [
          {
            xtype: 'checkbox',
            name: 'attributes.rawContent.strictContentTypeValidation',
            fieldLabel: NX.I18n.get('ADMIN_REPOSITORIES_SETTINGS_CONTENT_TYPE_VALIDATION'),
            value: true
          }
        ]
      }
    ];

    me.callParent(arguments);
  }

});
