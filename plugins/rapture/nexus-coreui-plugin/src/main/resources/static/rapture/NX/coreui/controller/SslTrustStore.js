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
 * Ssl TrustStore controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.SslTrustStore', {
  extend: 'Ext.app.Controller',
  requires: [
    'NX.Conditions',
    'NX.coreui.controller.SslCertificates'
  ],

  mixins: {
    logAware: 'NX.LogAware'
  },

  views: [
    'ssl.SslUseTrustStore'
  ],

  refs: [
    {
      ref: 'main',
      selector: 'nx-main'
    }
  ],

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.listen({
      component: {
        'field[useTrustStore]': {
          change: me.manageTrustStore
        },
        'nx-coreui-sslusetruststore button[action=showcertificate]': {
          click: me.loadCertificate
        }
      }
    });
  },

  /**
   * @private
   * @param field
   * @param field.useTrustStore
   * @param field.useTrustStoreField
   */
  manageTrustStore: function(field) {
    var me = this,
        container = field.up('container'),
        useTrustStoreField = field.useTrustStoreField,
        config, hostAndPort;

    if (Ext.isFunction(field.useTrustStore)) {
      config = field.useTrustStore.call(field, field);
      if (config) {
        hostAndPort = me.getHostAndPort(config);
        if (useTrustStoreField && ((useTrustStoreField.name !== config.name) || !hostAndPort.host)) {
          container.remove(useTrustStoreField);
          delete field.useTrustStoreField;
          useTrustStoreField = undefined;
        }
        if (hostAndPort.host) {
          if (!useTrustStoreField) {
            container.insert(container.items.indexOf(field) + 1, {
              xtype: 'nx-coreui-sslusetruststore',
              name: config.name,
              fieldLabel: field.useTrustStoreFieldLabel,
              boxLabel: field.useTrustStoreBoxLabel,
              useTrustStoreConfig: config,
              cls: 'nx-clear-both',
              listeners: {
                afterrender: me.bindConditions
              }
            });
            field.useTrustStoreField = container.down('nx-coreui-sslusetruststore[name=' + config.name + ']');
          }
          else {
            useTrustStoreField.useTrustStoreConfig = config;
          }
        }
      }
    }
    if ((!config) && useTrustStoreField) {
      container.remove(useTrustStoreField);
      delete field.useTrustStoreField;
    }
  },

  /**
   * @private
   * Retrieves certificate, showing the certificate details if successful.
   */
  loadCertificate: function(button) {
    var me = this,
        panel = button.up('panel').up('panel'),
        hostAndPort = me.getHostAndPort(button.up('nx-coreui-sslusetruststore').useTrustStoreConfig),
        sslCertificates = me.getController('NX.coreui.controller.SslCertificates');

    me.getMain().getEl().mask(NX.I18n.get('ADMIN_SSL_LOAD_MASK'));
    NX.direct.ssl_Certificate.retrieveFromHost(hostAndPort.host, hostAndPort.port, undefined, function(response) {
      me.getMain().getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        sslCertificates.showCertificateDetailsWindow(response.data);
      }
    });
  },

  /**
   * @private
   * Get host/port out of config.
   */
  getHostAndPort: function(config) {
    var me = this,
        sslCertificates = me.getController('NX.coreui.controller.SslCertificates'),
        valueOf = function(value) {
          if (Ext.isString(value)) {
            return value;
          }
          else if (value && Ext.isFunction(value.getValue)) {
            return value.getValue();
          }
          return undefined;
        },
        parsed, host, port;

    if (config.url) {
      parsed = sslCertificates.parseHostAndPort(valueOf(config.url));
      host = parsed[0];
      port = parsed[1];
    }
    else {
      host = valueOf(config.host);
      port = valueOf(config.port);
    }

    return {
      host: host,
      port: port
    };
  },

  bindConditions: function(useTrustStoreField) {
    var useTrustStoreCheckbox, useTrustStoreButton;

    useTrustStoreCheckbox = useTrustStoreField.down('checkbox');
    useTrustStoreButton = useTrustStoreField.down('button');
    useTrustStoreField.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:ssl:truststore', 'create'),
            NX.Conditions.isPermitted('nexus:ssl:truststore', 'update')
        ),
        {
          satisfied: useTrustStoreCheckbox.enable,
          unsatisfied: useTrustStoreCheckbox.disable,
          scope: useTrustStoreCheckbox
        }
    );
    useTrustStoreField.mon(
        NX.Conditions.isPermitted('nexus:ssl:truststore', 'read'),
        {
          satisfied: useTrustStoreButton.enable,
          unsatisfied: useTrustStoreButton.disable,
          scope: useTrustStoreButton
        }
    );
  }

});
