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
/*global Ext*/

/**
 * Unlicensed uber mode controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Unlicensed', {
  extend: 'Ext.app.Controller',
  mixins: {
    logAware: 'NX.LogAware'
  },

  views: [
    'Unlicensed',
    'header.Panel',
    'header.Branding',
    'header.Logo',
    'footer.Panel',
    'footer.Branding'
  ],

  refs: [
    {
      ref: 'viewport',
      selector: 'viewport'
    },
    {
      ref: 'unlicensed',
      selector: 'nx-unlicensed'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      component: {
        'viewport': {
          afterrender: me.onLaunch
        }
      }
    });
  },

  /**
   * @override
   * Show {@link NX.view.Unlicensed} view from {@link Ext.container.Viewport}.
   */
  onLaunch: function () {
    var me = this,
        viewport = me.getViewport();

    if (viewport) {
      //<if debug>
      me.logDebug('Showing unlicensed view');
      //</if>

      viewport.add({ xtype: 'nx-unlicensed' });
    }
  },

  /**
   * @public
   * Removes {@link NX.view.Unlicensed} view from {@link Ext.container.Viewport}.
   */
  onDestroy: function () {
    var me = this,
        viewport = me.getViewport();

    if (viewport) {
      //<if debug>
      me.logDebug('Removing unlicensed view');
      //</if>

      viewport.remove(me.getUnlicensed());
    }
  }

});
