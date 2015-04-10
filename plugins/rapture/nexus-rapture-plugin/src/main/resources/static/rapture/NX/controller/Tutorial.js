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
      html: 'Press this button'
    });
  },

  /**
   * @private
   */
  onClickUserMode: function() {
    var me = this;

    if (me.currentStep == 0) {
      me.showTip({
        target: 'nx-header-browse-mode',
        calloutArrowLocation: 'top',
        relativePosition: 't-b',
        cls: 'default',
        width: 200,
        autoHide: false,
        html: 'Press another button'
      });

      me.currentStep = me.currentStep + 1;
    }
  }
});
