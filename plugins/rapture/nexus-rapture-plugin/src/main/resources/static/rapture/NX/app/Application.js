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
 * Nexus application.
 *
 * @since 3.0
 */
Ext.define('NX.app.Application', {
  extend: 'Ext.app.Application',

  requires: [
    'Ext.Ajax',
    'Ext.Error',
    'Ext.direct.Manager',
    'Ext.state.Manager',
    'Ext.state.LocalStorageProvider',
    'Ext.util.LocalStorage',
    'NX.view.Viewport',
    'NX.util.Url',
    'NX.I18n',
    'NX.State'
  ],

  mixins: {
    logAware: 'NX.LogAware'
  },

  uses: [
    // framework patches
    'Ext.patch.Ticket_15227',
    'Ext.patch.Ticket_18960',
    'Ext.patch.Ticket_18964',
    'Ext.patch.Ticket_21425',
    'Ext.patch.Ticket_22557_1',
    'Ext.patch.Ticket_22557_2',

    // direct overrides
    'NX.ext.form.action.DirectLoad',
    'NX.ext.form.action.DirectSubmit',

    // form overrides
    'NX.ext.form.FieldContainer',
    'NX.ext.form.field.Base',
    'NX.ext.form.field.Checkbox',
    'NX.ext.form.field.Display',

    // custom form fields
    'NX.ext.form.OptionalFieldSet',
    'NX.ext.form.field.Email',
    'NX.ext.form.field.Password',
    'NX.ext.form.field.RegExp',
    'NX.ext.form.field.Url',
    'NX.ext.form.field.ClearableComboBox',
    'NX.ext.form.field.DateDisplayField',
    'NX.ext.form.field.ValueSet',
    'NX.ext.SearchBox',
    'Ext.ux.form.ItemSelector',

    // grid plugins
    'NX.ext.grid.plugin.FilterBox',
    'NX.ext.grid.plugin.Filtering',

    // grid overrides
    'NX.ext.grid.column.Date',

    // custom grid columns
    'NX.ext.grid.column.Icon',
    'NX.ext.grid.column.Link'
  ],

  name: 'NX',

  /**
   * Store application instance in "NX.application".
   */
  appProperty: 'application',

  /**
   * Relative to /index.html
   */
  appFolder: 'static/rapture/NX',

  paths: {
    'Ext.ux': 'static/rapture/Ext/ux',
    'Ext.patch': 'static/rapture/Ext/patch'
  },

  /**
   * Always active controllers.
   */
  controllers: [
    'State',
    'Bookmarking',
    'ExtDirect',
    'Features',
    'Icon',
    'KeyNav',
    'Message',
    'Permissions'
  ],

  /**
   * @private
   * {@link Ext.util.MixedCollection} containing managed controllers configurations
   */
  managedControllers: undefined,

  statics: {
    alwaysActive: function () {
      return true;
    },
    defaultActivation: function () {
      return NX.app.Application.supportedBrowser() && NX.app.Application.licensed();
    },
    supportedBrowser: function () {
      return NX.State.isBrowserSupported();
    },
    unsupportedBrowser: function () {
      return !NX.app.Application.supportedBrowser();
    },
    licensed: function () {
      return !NX.State.requiresLicense() || NX.State.isLicenseInstalled();
    },
    unlicensed: function () {
      return !NX.app.Application.licensed();
    },
    debugMode: function () {
      return NX.State.getValue('debug') === true;
    },
    bundleActive: function (symbolicName) {
      // FIXME: Rename key
      return NX.State.getValue('activeBundles').indexOf(symbolicName) > -1;
    }
  },

  /**
   * @override
   * @param {NX.app.Application} app this class
   */
  init: function (app) {
    var me = this;

    //<if debug>
    me.logDebug('Initializing');
    me.logDebug(me.managedControllers.getCount() + ' managed controllers');
    //</if>

    // Configure blank image URL
    Ext.BLANK_IMAGE_URL = NX.util.Url.baseUrl + '/static/rapture/resources/images/s.gif';

    Ext.Ajax.defaultHeaders = {
      // HACK: Setting request header to allow analytics to tell if the request came from the UI or not
      // HACK: This has some issues, will only catch ajax requests, etc... but may be fine for now
      'X-Nexus-UI': 'true'
    };

    app.initErrorHandler();
    app.initDirect();
    app.initState();
  },

  /**
   * @private
   * Hook into browser error handling (in order to log them).
   */
  initErrorHandler: function () {
    var me = this,
        originalOnError = NX.global.onerror;

    // FIXME: This needs further refinement, seems like javascript errors are lost in Firefox (but show up fine in Chrome)

    // pass unhandled errors to application error handler
    Ext.Error.handle = function (err) {
      me.handleError(err);
    };

    // FIXME: This will catch more errors, but duplicates messages for ext errors
    // FIXME: Without this however some javascript errors will go unhandled
    NX.global.onerror = function (msg, url, line) {
      me.handleError({ msg: msg + ' (' + url + ':' + line + ')' });

      // maybe delegate to original onerror handler?
      if (originalOnError) {
        originalOnError(msg, url, line);
      }
    };

    //<if debug>
    me.logDebug('Configured error handling');
    //</if>
  },

  /**
   * @private
   * Log catched error.
   */
  handleError: function (error) {
    var me = this;
    NX.Messages.add({
      type: 'danger',
      text: me.errorAsString(error)
    });
  },

  /**
   * @private
   * Customize error to-string handling.
   *
   * Ext.Error.toString() assumes instance, but raise(String) makes anonymous object.
   */
  errorAsString: function (error) {
    var className = error.sourceClass || '',
        methodName = error.sourceMethod ? '.' + error.sourceMethod + '(): ' : '',
        msg = error.msg || '(No description provided)';
    return className + methodName + msg;
  },

  /**
   * @private
   * Initialize Ex.Direct remote providers.
   */
  initDirect: function () {
    var me = this,
        remotingProvider;

    remotingProvider = Ext.direct.Manager.addProvider(NX.direct.api.REMOTING_API);

    // disable retry
    remotingProvider.maxRetries = 0;

    // default request timeout to 60 seconds
    remotingProvider.timeout = 60 * 1000;

    //<if debug>
    me.logDebug('Configured Ext.Direct');
    //</if>
  },

  /**
   * @private
   * Initialize state manager.
   */
  initState: function () {
    var me = this;

    // If local storage is supported install state provider
    if (Ext.util.LocalStorage.supported) {
      Ext.state.Manager.setProvider(Ext.create('Ext.state.LocalStorageProvider'));
      //<if debug>
      me.logDebug('Configured state provider: local');
      //</if>
    }
    else {
      //<if debug>
      me.logWarn('Local storage not supported; state management not supported');
      //</if>
    }

    // HACK: for debugging
    //provider.on('statechange', function (provider, key, value, opts) {
    //  me.logDebug('State changed: ' + key + '=' + value);
    //});
  },

  /**
   * @public
   * Starts the application.
   */
  start: function () {
    var me = this, hideMask;

    //<if debug>
    me.logDebug('Starting');
    //</if>

    Ext.create('NX.view.Viewport');

    me.syncManagedControllers();
    me.listen({
      controller: {
        '#State': {
          changed: me.syncManagedControllers
        }
      }
    });

    // hide the loading mask after we have loaded
    hideMask = function () {
      Ext.get('loading').remove();
      Ext.fly('loading-mask').animate({ opacity: 0, remove: true });
    };

    // FIXME: Need a better way to know when the UI is actually rendered so we can hide the mask
    // HACK: for now increasing delay slightly to cope with longer loading times
    Ext.defer(hideMask, 500);
  },

  /**
   * @private
   * Create / Destroy managed controllers based on their active status.
   */
  syncManagedControllers: function () {
    var me = this,
        ref, initializedControllers = [],
        changes = false;

    //<if debug>
    me.logDebug('Refreshing controllers');
    //</if>

    // destroy all controllers that are become inactive
    me.managedControllers.eachKey(function (key) {
      ref = me.managedControllers.get(key);
      if (!ref.active()) {
        if (ref.controller) {
          changes = true;

          //<if debug>
          me.logDebug('Destroying controller: ' + key);
          //</if>

          ref.controller.fireEvent('destroy', ref.controller);
          ref.controller.eventbus.unlisten(ref.controller.id);
          if (Ext.isFunction(ref.controller.onDestroy)) {
            ref.controller.onDestroy();
          }
          me.controllers.remove(ref.controller);
          ref.controller.clearManagedListeners();
          if (Ext.isFunction(ref.controller.destroy)) {
            ref.controller.destroy();
          }
          delete ref.controller;
        }
      }
    });

    // create & init all controllers that become active
    me.managedControllers.eachKey(function (key) {
      ref = me.managedControllers.get(key);
      if (ref.active()) {
        if (!ref.controller) {
          changes = true;

          //<if debug>
          me.logDebug('Initializing controller: ' + key);
          //</if>

          ref.controller = me.getController(key);
          initializedControllers.push(ref.controller);
        }
      }
    });

    // launch any initialized controller
    Ext.each(initializedControllers, function (controller) {
      controller.onLaunch(me);
    });
    // finish init on any initialized controller
    Ext.each(initializedControllers, function (controller) {
      controller.finishInit(me);
    });

    if (changes) {
      // TODO shall we do this on each refresh?
      me.getIconController().installStylesheet();
      me.fireEvent('controllerschanged');
    }
  }
});
