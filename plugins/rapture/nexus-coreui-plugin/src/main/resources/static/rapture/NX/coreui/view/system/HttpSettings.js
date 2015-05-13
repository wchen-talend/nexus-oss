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
 * HTTP System Settings form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.system.HttpSettings', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-system-http-settings',
  requires: [
    'NX.Conditions',
    'NX.coreui.view.system.AuthenticationSettings',
    'NX.coreui.view.system.HttpRequestSettings',
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.settingsForm = [
      {
        xtype: 'nx-settingsform',
        settingsFormSuccessMessage: NX.I18n.get('ADMIN_HTTP_UPDATE_SUCCESS'),
        api: {
          load: 'NX.direct.coreui_HttpSettings.read',
          submit: 'NX.direct.coreui_HttpSettings.update'
        },
        editableCondition: NX.Conditions.isPermitted('nexus:settings', 'update'),
        editableMarker: NX.I18n.get('ADMIN_HTTP_UPDATE_ERROR'),
        items: [
          // request settings
          {
            xtype: 'nx-coreui-system-httprequestsettings'
          },
          {
            xtype: 'nx-optionalfieldset',
            title: NX.I18n.get('ADMIN_HTTP_PROXY'),
            checkboxToggle: true,
            checkboxName: 'httpEnabled',
            items: [
              {
                xtype: 'textfield',
                name: 'httpHost',
                fieldLabel: NX.I18n.get('ADMIN_HTTP_PROXY_HOST'),
                helpText: NX.I18n.get('ADMIN_HTTP_PROXY_HOST_HELP'),
                allowBlank: false
              },
              {
                xtype: 'numberfield',
                name: 'httpPort',
                fieldLabel: NX.I18n.get('ADMIN_HTTP_PROXY_PORT'),
                minValue: 1,
                maxValue: 65535,
                allowDecimals: false,
                allowExponential: false,
                allowBlank: false
              },
              {
                xtype: 'nx-optionalfieldset',
                title: NX.I18n.get('ADMIN_HTTP_PROXY_AUTHENTICATION'),
                checkboxToggle: true,
                checkboxName: 'httpAuthEnabled',
                collapsed: true,
                items: {
                  xtype: 'nx-coreui-system-authenticationsettings',
                  namePrefix: 'http'
                }
              },
              {
                xtype: 'nx-valueset',
                name: 'nonProxyHosts',
                fieldLabel: NX.I18n.get('ADMIN_HTTP_PROXY_NON_PROXY'),
                helpText: NX.I18n.get('ADMIN_HTTP_PROXY_NON_PROXY_HELP'),
                sorted: true
              }
            ]
          },

          {
            xtype: 'nx-optionalfieldset',
            title: NX.I18n.get('ADMIN_HTTPS_PROXY'),
            itemId: 'httpsProxy',
            checkboxToggle: true,
            checkboxName: 'httpsEnabled',
            collapsed: true,
            items: [
              {
                xtype: 'textfield',
                name: 'httpsHost',
                fieldLabel: NX.I18n.get('ADMIN_HTTPS_PROXY_HOST'),
                helpText: NX.I18n.get('ADMIN_HTTPS_PROXY_HOST_HELP'),
                allowBlank: false
              },
              {
                xtype: 'numberfield',
                name: 'httpsPort',
                fieldLabel: NX.I18n.get('ADMIN_HTTPS_PROXY_PORT'),
                minValue: 1,
                maxValue: 65535,
                allowDecimals: false,
                allowExponential: false,
                allowBlank: false
              },
              {
                xtype: 'nx-optionalfieldset',
                title: NX.I18n.get('ADMIN_HTTPS_PROXY_AUTHENTICATION'),
                checkboxToggle: true,
                checkboxName: 'httpsAuthEnabled',
                collapsed: true,
                items: {
                  xtype: 'nx-coreui-system-authenticationsettings',
                  namePrefix: 'https'
                }
              }
            ]
          }
        ]
      }
    ];

    me.callParent();
  }
});
