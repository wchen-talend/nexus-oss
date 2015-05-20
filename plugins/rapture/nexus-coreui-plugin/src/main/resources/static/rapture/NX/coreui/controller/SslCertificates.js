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
 * Ssl Certificates controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.SslCertificates', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n'
  ],

  masters: 'nx-coreui-sslcertificate-list',

  models: [
    'SslCertificate'
  ],
  stores: [
    'SslCertificate'
  ],
  views: [
    'ssl.SslCertificateAddFromPem',
    'ssl.SslCertificateAddFromServer',
    'ssl.SslCertificateDetails',
    'ssl.SslCertificateDetailsForm',
    'ssl.SslCertificateDetailsPanel',
    'ssl.SslCertificateDetailsWindow',
    'ssl.SslCertificateFeature',
    'ssl.SslCertificateList'
  ],
  refs: [
    { ref: 'feature', selector: 'nx-coreui-sslcertificate-feature' },
    { ref: 'content', selector: 'nx-feature-content' },
    { ref: 'list', selector: 'nx-coreui-sslcertificate-list' },
    { ref: 'details', selector: 'nx-coreui-sslcertificate-feature nx-coreui-sslcertificate-details-form' }
  ],
  icons: {
    'sslcertificate-default': {
      file: 'ssl_certificates.png',
      variants: ['x16', 'x32']
    },
    'sslcertificate-add-by-pem': {
      file: 'server_add.png',
      variants: ['x16', 'x32']
    },
    'sslcertificate-add-by-server': {
      file: 'server_connect.png',
      variants: ['x16', 'x32']
    }
  },
  features: {
    mode: 'admin',
    path: '/Security/SSL Certificates',
    view: { xtype: 'nx-coreui-sslcertificate-feature' },
    text: NX.I18n.get('ADMIN_SSL_TITLE'),
    description: NX.I18n.get('ADMIN_SSL_SUBTITLE'),
    iconConfig: {
      file: 'ssl_certificates.png',
      variants: ['x16', 'x32']
    },
    visible: function () {
      return NX.Permissions.check('nexus:ssl-truststore:read');
    }
  },
  permission: 'nexus:ssl-truststore',

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.callParent();

    me.listen({
      component: {
        'nx-coreui-sslcertificate-list menuitem[action=newfromserver]': {
          click: me.showAddWindowFromServer
        },
        'nx-coreui-sslcertificate-list menuitem[action=newfrompem]': {
          click: me.showAddWindowFromPem
        },
        'nx-coreui-sslcertificate-add-from-pem button[action=load]': {
          click: me.loadCertificateByPem
        },
        'nx-coreui-sslcertificate-add-from-server button[action=load]': {
          click: me.loadCertificateByServer
        },
        'nx-coreui-sslcertificate-details-panel button[action=add]': {
          click: me.create
        },
        'nx-coreui-sslcertificate-details-panel button[action=remove]': {
          click: me.remove
        },
        'nx-coreui-sslcertificate-details-window button[action=add]': {
          click: me.create
        },
        'nx-coreui-sslcertificate-details-window button[action=remove]': {
          click: me.remove
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function (model) {
    return model.get('subjectCommonName');
  },

  /**
   * @override
   */
  onSelection: function (list, model) {
    var me = this;

    if (Ext.isDefined(model)) {
      me.getDetails().loadRecord(model);
    }
  },

  /**
   * @private
   */
  showAddWindowFromServer: function () {
    var me = this,
      feature = me.getFeature();

    // Show the first panel in the create wizard, and set the breadcrumb
    feature.setItemName(1, NX.I18n.get('ADMIN_SSL_LOAD_TITLE'));
    me.loadCreateWizard(1, true, Ext.create('widget.nx-coreui-sslcertificate-add-from-server'));
  },

  /**
   * @private
   */
  showAddWindowFromPem: function () {
    var me = this,
      feature = me.getFeature();

    // Show the first panel in the create wizard, and set the breadcrumb
    feature.setItemName(1, NX.I18n.get('ADMIN_SSL_PASTE_TITLE'));
    me.loadCreateWizard(1, true, Ext.create('widget.nx-coreui-sslcertificate-add-from-pem'));
  },

  /**
   * @private
   * Shows certificate details panel.
   */
  showCertificateDetailsPanel: function(certificate) {
    var me = this,
        feature = me.getFeature(),
        panel = Ext.widget('nx-coreui-sslcertificate-details-panel'),
        form = panel.down('form'),
        model = me.getSslCertificateModel().create(certificate);

    // override form to show buttons
    me.overrideLoadRecord(form);
    // Load the certificate
    form.loadRecord(model);

    // Show the second panel in the create wiard, and set the breadcrumb
    feature.setItemName(2, NX.I18n.get('ADMIN_SSL_DETAILS_TITLE'));
    me.loadCreateWizard(2, true, panel);
  },

  /**
   * @private
   * Shows certificate details window.
   */
  showCertificateDetailsWindow: function (certificate) {
    var me = this,
        window = Ext.widget('nx-coreui-sslcertificate-details-window'),
        form = window.down('form'),
        model = me.getSslCertificateModel().create(certificate);

    // override form to show buttons
    me.overrideLoadRecord(form);
    // Load the certificate
    form.loadRecord(model);
  },

  /**
   * @private
   * Retrieves details of a certificate specified by PEM, showing the certificate details if successful.
   */
  loadCertificateByPem: function (button) {
    var me = this,
        basicForm = button.up('form').getForm(),
        pem = basicForm.getFieldValues()['pem'];

    me.getContent().getEl().mask(NX.I18n.get('ADMIN_SSL_LOAD_MASK'));
    NX.direct.ssl_Certificate.details(pem, function(response) {
      me.getContent().getEl().unmask();
      if (Ext.isObject(response)) {
        if (response.success) {
          me.showCertificateDetailsPanel(response.data);
        }
        else if (response.errors) {
          basicForm.markInvalid(response.errors);
        }
      }
    });
  },

  /**
   * @private
   * Retrieves certificate from host, showing the certificate details if successful.
   */
  loadCertificateByServer: function (button) {
    var me = this,
        server = button.up('form').getForm().getFieldValues()['server'],
        parsed = me.parseHostAndPort(server),
        protocolHint = server && Ext.String.startsWith(server, "https://") ? 'https' : undefined;

    me.getContent().getEl().mask(NX.I18n.get('ADMIN_SSL_LOAD_MASK'));
    NX.direct.ssl_Certificate.retrieveFromHost(parsed[0], parsed[1], protocolHint, function (response) {
      me.getContent().getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        me.showCertificateDetailsPanel(response.data);
      }
    });
  },

  /**
   * @private
   * Parses server string into an array of [host,port].
   */
  parseHostAndPort: function (server) {
    var rx, matches;

    if (!server) {
      return [];
    }
    if (server.indexOf(":") === -1) { // neither URL nor host:port
      return [server];
    }
    if (server.indexOf("/") === -1) { // definitely no URL
      server.split(":");
    }

    rx = /[^:]*:\/\/([^:\/]*)(:([0-9]*))?/;
    matches = server.match(rx);
    if (matches) {
      return [matches[1], Ext.Number.from(matches[3], undefined)];
    }

    return [server];
  },

  /**
   * @private
   * Creates an SSL certificate.
   */
  create: function (button) {
    var me = this,
        win = button.up('nx-coreui-sslcertificate-details-window'),
        form = button.up('form'),
        model = form.getRecord(),
        description = me.getDescription(model);

    NX.direct.ssl_TrustStore.create(model.get('pem'), function (response) {
      if (Ext.isObject(response) && response.success) {
        if (win) {
          win.close();
        }
        me.loadStoreAndSelect(model.internalId, false);
        NX.Messages.add({ text: NX.I18n.format('ADMIN_SSL_LOAD_SUCCESS', description), type: 'success' });
      }
    });
  },

  /**
   * @private
   * Removes an SSL certificate.
   */
  remove: function (button) {
    var me = this,
        win = button.up('nx-coreui-sslcertificate-details-window'),
        form = button.up('form'),
        model = form.getRecord(),
        description = me.getDescription(model);

    NX.direct.ssl_TrustStore.remove(model.getId(), function (response) {
      if (Ext.isObject(response) && response.success) {
        if (win) {
          win.close();
        }
        me.loadStore(Ext.emptyFn);
        NX.Messages.add({ text: NX.I18n.format('ADMIN_SSL_DETAILS_DELETE_SUCCESS', description), type: 'success' });
      }
    });
  },

  /**
   * @private
   * @override
   * Deletes an SSL certificate.
   * @param {NX.coreui.model.SslCertificate} model to be deleted
   */
  deleteModel: function (model) {
    var me = this,
        description = me.getDescription(model);

    NX.direct.ssl_TrustStore.remove(model.getId(), function (response) {
      me.loadStore();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({ text: NX.I18n.format('ADMIN_SSL_DETAILS_DELETE_SUCCESS', description), type: 'success' });
      }
    });
  },

  /**
   * @private
   * Override loadRecord() in order to show add/delete from store buttons.
   */
  overrideLoadRecord: function (form) {
    Ext.override(form, {
      loadRecord: function (model) {
        var me = this,
            tbar = me.getDockedItems('toolbar[dock="bottom"]')[0],
            button;

        if (model) {
          if (model.get('inTrustStore')) {
            tbar.insert(0, {
              text: NX.I18n.get('ADMIN_SSL_DETAILS_REMOVE'),
              action: 'remove',
              formBind: true,
              disabled: true,
              ui: 'nx-primary',
              glyph: 'xf056@FontAwesome' /* fa-minus-circle */
            });
            button = tbar.down('button[action=remove]');
            me.mon(
                NX.Conditions.isPermitted('nexus:ssl-truststore:delete'),
                {
                  satisfied: button.enable,
                  unsatisfied: button.disable,
                  scope: button
                }
            );
          }
          else {
            tbar.insert(0, {
              text: NX.I18n.get('ADMIN_SSL_DETAILS_ADD'),
              action: 'add',
              formBind: true,
              disabled: true,
              ui: 'nx-primary',
              glyph: 'xf055@FontAwesome' /* fa-plus-circle */
            });
            button = tbar.down('button[action=add]');
            me.mon(
                NX.Conditions.isPermitted('nexus:ssl-truststore:create'),
                {
                  satisfied: button.enable,
                  unsatisfied: button.disable,
                  scope: button
                }
            );
          }
        }
        me.callParent(arguments);
      }
    });
  }

});
