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
 * Watches over Ext.Direct communication.
 *
 * @since 3.0
 */
Ext.define('NX.controller.ExtDirect', {
  extend: 'Ext.app.Controller',
  mixins: [
    'NX.LogAware'
  ],
  requires: [
    'NX.Security',
    'NX.Messages'
  ],

  init: function() {
    var me = this;

    me.listen({
      direct: {
        '*': {
          beforecallback: me.checkResponse
        }
      }
    });
  },

  /**
   * @private
   * Checks Ext.Direct response and automatically show warning messages if an error occurred.
   * If response specifies that authentication is required, will show the sign-in window.
   */
  checkResponse: function(provider, transaction) {
    var me = this,
        result = transaction.result,
        message;

    if (Ext.isDefined(result)) {
      if (Ext.isDefined(result.success) && result.success === false) {

        if (Ext.isDefined(result.authenticationRequired) && result.authenticationRequired === true) {
          message = result.message;
          NX.Security.askToAuthenticate();
        }
        else if (Ext.isDefined(result.message)) {
          message = result.message;
        }
        else if (Ext.isDefined(result.messages)) {
          message = Ext.Array.from(result.messages).join('<br/>');
        }
      }

      if (Ext.isDefined(transaction.serverException)) {
        message = transaction.serverException.exception.message;
      }
    }
    else {
      message = NX.I18n.get('GLOBAL_SERVER_CONNECT_FAILURE');
    }

    if (message) {
      NX.Messages.add({text: message, type: 'warning'});
    }

    //<if debug>
    var logMsg = transaction.action + ':' + transaction.method + " -> " + (message ? 'Failed: ' + message : 'OK');
    if (Ext.isDefined(result) && result.errors) {
      logMsg += (' Errors: ' + Ext.encode(result.errors));
    }
    me.logDebug(logMsg);
    //</if>
  }

});
