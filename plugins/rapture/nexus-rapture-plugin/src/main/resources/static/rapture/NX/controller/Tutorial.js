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
 * Refresh controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Tutorial', {
  extend: 'Ext.app.Controller',

  views: [
    'header.Tutorial'
  ],

  steps: [
    'start',
    'end'
  ],

  currentStep: -1,
  currentTip: null,

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.listen({
      component: {
        'nx-header-tutorial menuitem[action=start]': {
          click: me.onStart
        },
        'nx-header-user-mode': {
          click: me.onClickUserMode
        },
        'nx-coreui-user-account button[action=changepassword]': {
          click: me.onClickChangePassword
        },
        'nx-authenticate': {
          boxready: function() {
            if (me.currentStep == 2) {
              Ext.Function.defer(function() {
                me.showTip({
                  target: 'nx-authenticate textfield[name=password]',
                  calloutArrowLocation: 'left',
                  relativePosition: 'l-r',
                  cls: 'default',
                  width: 200,
                  autoHide: false,
                  html: 'Password: admin123'
                });

                me.currentStep++;
              }, 10);
            }
          }
        },
        'nx-authenticate textfield[name=password]': {
          change: function(cmp, val) {
            if (val == "admin123") {
              me.onChangePassword();
            }
          }
        },
        'nx-authenticate button[action=authenticate]': {
          click: me.onClickAuthenticate
        }
      }
    });

    me.callParent(arguments);
  },

  /**
   * @private
   */
  showTip: function(tip) {
    var me = this;

    if (me.currentTip) {
      me.currentTip.destroy();
    }

    me.currentTip = Ext.create('Ext.ux.callout.Callout', tip);
    me.currentTip.show();

    // Bring the tooltip to the front
    Ext.Function.defer(function() {
      me.currentTip.zIndexManager.bringToFront(me.currentTip);
    }, 10);
  },

  /**
   * @private
   */
  onStart: function() {
    var me = this;

    me.currentStep = 0;

    me.showTip({
      target: 'nx-header-user-mode',
      calloutArrowLocation: 'top',
      relativePosition: 't-b',
      cls: 'default',
      width: 200,
      autoHide: false,
      html: 'Open user mode'
    });
  },

  /**
   * @private
   */
  onClickUserMode: function() {
    var me = this;

    if (me.currentStep == 0) {
      me.showTip({
        target: 'nx-coreui-user-account button[action=changepassword]',
        calloutArrowLocation: 'top',
        relativePosition: 't-b',
        cls: 'default',
        width: 200,
        autoHide: false,
        html: 'Change your password'
      });

      me.currentStep++;
    }
  },

  /**
   * @private
   */
  onClickChangePassword: function() {
    var me = this;

    if (me.currentStep == 1) {
      me.currentStep++;
    }
  },

  /**
   * @private
   */
  onChangePassword: function() {
    var me = this;

    if (me.currentStep == 3) {
      me.showTip({
        target: 'nx-authenticate button[action=authenticate]',
        calloutArrowLocation: 'top',
        relativePosition: 't-b',
        cls: 'default',
        width: 200,
        autoHide: false,
        html: 'Authenticate yourself'
      });

      me.currentStep++;
    }
  },

  /**
   * @private
   */
  onClickAuthenticate: function() {
    var me = this;

    if (me.currentStep == 4) {
      /*me.showTip({

      });*/

      me.currentStep++;
    }
  }
});
