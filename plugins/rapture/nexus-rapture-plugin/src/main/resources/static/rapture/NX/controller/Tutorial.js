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

  currentStep: 0,
  currentTip: null,

  refs: [
    {
      ref: 'tutorials',
      selector: 'nx-header-tutorial'
    },
    {
      ref: 'tutorial01',
      selector: 'nx-header-tutorial menuitem[action=start]'
    }
  ],

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.listen({
      component: {
        'nx-header-tutorial': {
          boxready: me.onTutorialPrompt
        },
        'nx-header-tutorial menuitem[action=start]': {
          click: me.onInitTutorial
        },
        'nx-header-user-mode': {
          click: me.onStep1
        },
        'nx-authenticate': {
          boxready: me.onStep2
        },
        'nx-authenticate textfield[name=password]': {
          change: me.onStep3
        },
        'nx-coreui-user-changepassword': {
          boxready: me.onStep4
        },
        'nx-coreui-user-changepassword textfield:not([name=password])': {
          change: me.onStep5
        },
        'nx-coreui-user-changepassword button[action=changepassword]': {
          click: me.onStep6
        }
      }
    });

    me.callParent(arguments);
  },

  /**
   * @private
   * Helper function to show a callout with some intelligent defaults
   */
  showTip: function(tip) {
    var me = this,
      defaults = {
        target: 'nx-header-tutorial',
        calloutArrowLocation: 'top',
        relativePosition: 't-b',
        cls: 'yellow',
        width: 200,
        fadeInDuration: 400,
        autoHide: false,
        html: 'Click this'
      };

    // Destroy existing tooltip, if it exists
    if (me.currentTip) {
      me.currentTip.destroy();
    }

    // Apply defaults
    Ext.apply(defaults, tip);

    // Add an offset based on the callout arrow location
    if (defaults.calloutArrowLocation == 'top') {
      defaults.relativeOffsets = [0, 15];
    } else if (defaults.calloutArrowLocation == 'left') {
      defaults.relativeOffsets = [15, 0];
    }

    // Create the tooltip
    me.currentTip = Ext.create('Ext.ux.callout.Callout', defaults);
    me.currentTip.show();

    // Bring the tooltip to the front
    Ext.Function.defer(function() {
      me.currentTip.zIndexManager.bringToFront(me.currentTip);
    }, 10);
  },

  /**
   * @private
   * Helper function to prompt the user to start the tutorial
   */
  onTutorialPrompt: function() {
    var me = this;

    // Create a tutorial prompt
    me.showTip({
      html: 'Welcome to Nexus. Here are some tutorials to help you get the most from your installation.',
      autoHide: true
    });
  },

  /**
   * @private
   * Helper function to initialize the tutorial
   */
  onInitTutorial: function() {
    var me = this;

    me.currentStep = 1;

    me.showTip({
      target: 'nx-header-user-mode',
      html: 'Open user mode'
    });
  },

  /**
   * @private
   */
  onStep1: function() {
    var me = this;

    if (me.currentStep == 1) {
      me.showTip({
        target: 'nx-coreui-user-account button[action=changepassword]',
        html: 'Change your password'
      });

      me.currentStep++;
    }
  },

  /**
   * @private
   */
  onStep2: function() {
    var me = this;

    if (me.currentStep == 2) {
      Ext.Function.defer(function() {
        me.showTip({
          target: 'nx-authenticate textfield[name=password]',
          calloutArrowLocation: 'left',
          relativePosition: 'l-r',
          html: 'Password: admin123'
        });

        me.currentStep++;
      }, 10);
    }
  },

  /**
   * @private
   */
  onStep3: function(cmp, val) {
    var me = this;

    if (val == "admin123") {
      if (me.currentStep == 3) {
        me.showTip({
          target: 'nx-authenticate button[action=authenticate]',
          html: 'Authenticate yourself'
        });

        me.currentStep++;
      }
    }
  },

  /**
   * @private
   */
  onStep4: function() {
    var me = this;

    if (me.currentStep == 4) {
      Ext.Function.defer(function() {
        me.showTip({
          target: 'nx-coreui-user-changepassword textfield[name=password]',
          calloutArrowLocation: 'left',
          relativePosition: 'l-r',
          html: 'Choose a new password'
        });

        me.currentStep++;
      }, 10);
    }
  },

  /**
   * @private
   */
  onStep5: function(cmp, val) {
    var me = this;

    var password = Ext.ComponentQuery.query('nx-coreui-user-changepassword textfield[name=password]')[0];
    var value = password.getValue();

    if (val == value) {
      if (me.currentStep == 5) {
        me.showTip({
          target: 'nx-coreui-user-changepassword button[action=changepassword]',
          html: 'Confirm your changes'
        });

        me.currentStep++;
      }
    }
  },

  /**
   * @private
   */
  onStep6: function() {
    var me = this,
      tutorialMenuItem = me.getTutorial01(),
      tutorials = me.getTutorials();

    if (me.currentStep == 6) {
      me.showTip({
        dismissDelay: 3000,
        html: 'Congratulations! You’ve changed your password.'
      });

      // Change the tutorial icon to show completion
      tutorialMenuItem.setIconCls('nx-icon-repositorybrowse-inIndex-x16');
      tutorials.setTooltip('1/10 tutorials completed');
    }
  }
});
