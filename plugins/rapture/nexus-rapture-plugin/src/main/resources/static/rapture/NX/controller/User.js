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
 * User controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.User', {
  extend: 'Ext.app.Controller',
  requires: [
    'NX.util.Base64',
    'NX.Messages',
    'NX.State',
    'NX.I18n'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  views: [
    'header.SignIn',
    'header.SignOut',
    'header.UserMode',
    'Authenticate',
    'SignIn',
    'ExpireSession'
  ],

  refs: [
    {
      ref: 'signInButton',
      selector: 'nx-header-signin'
    },
    {
      ref: 'signOutButton',
      selector: 'nx-header-signout'
    },
    {
      ref: 'userButton',
      selector: 'nx-header-user-mode'
    },
    {
      ref: 'signIn',
      selector: 'nx-signin'
    },
    {
      ref: 'authenticate',
      selector: 'nx-authenticate'
    }
  ],

  init: function () {
    var me = this;

    me.getApplication().getIconController().addIcons({
      'authenticate': {
        file: 'lock.png',
        variants: ['x16', 'x32']
      }
    });

    me.listen({
      controller: {
        '#State': {
          userchanged: me.onUserChanged
        }
      },
      component: {
        'nx-header-panel': {
          afterrender: me.manageButtons
        },
        'nx-header-signin': {
          click: me.showSignInWindow
        },
        'nx-expire-session button[action=signin]': {
          click: me.showSignInWindow
        },
        'nx-header-signout': {
          click: me.onClickSignOut
        },
        'nx-signin button[action=signin]': {
          click: me.signIn
        },
        'nx-authenticate button[action=authenticate]': {
          click: me.doAuthenticateAction
        }
      }
    });

    me.addEvents(
        /**
         * @event signin
         * Fires when a user had been successfully signed-in.
         * @param {Object} user
         */
        'signin',

        /**
         * @event beforesignout
         * Fires before a user is signed out
         */
        'beforesignout',

        /**
         * @event signout
         * Fires when a user had been successfully signed-out.
         */
        'signout'
    );
  },

  /**
   * @private
   */
  onUserChanged: function (user, oldUser) {
    var me = this;

    if (user && !oldUser) {
      NX.Messages.add({text: NX.I18n.format('GLOBAL_SERVER_SIGNED_IN', user.id), type: 'default'});
      me.fireEvent('signin', user);
    }
    else if (!user && oldUser) {
      NX.Messages.add({text: NX.I18n.get('GLOBAL_SERVER_SIGNED_OUT'), type: 'default'});
      me.fireEvent('signout');
    }

    me.manageButtons();
  },

  /**
   * Returns true if there is an authenticated user.
   *
   * @public
   * @return {boolean}
   */
  hasUser: function () {
    return Ext.isDefined(NX.State.getUser());
  },

  /**
   * @public
   * Shows sign-in or authentication window based on the fact that we have an user or not.
   * @param {String} [message] Message to be shown in authentication window
   * @param {Object} [options] TODO
   */
  askToAuthenticate: function (message, options) {
    var me = this;

    if (me.hasUser()) {
      me.showAuthenticateWindow(message, Ext.apply(options || {}, {authenticateAction: me.authenticate}));
    }
    else {
      me.showSignInWindow(options);
    }
  },

  /**
   * @public
   * Shows authentication window in order to retrieve an authentication token.
   * @param {String} [message] Message to be shown in authentication window
   * @param {Object} [options] TODO
   */
  doWithAuthenticationToken: function (message, options) {
    var me = this;

    me.showAuthenticateWindow(message,
        Ext.apply(options || {}, {authenticateAction: me.retrieveAuthenticationToken})
    );
  },

  /**
   * @private
   * Shows sign-in window.
   * @param {Object} [options] TODO
   */
  showSignInWindow: function (options) {
    var me = this;

    if (!me.getSignIn()) {
      me.getSignInView().create({options: options});
    }
  },

  /**
   * @private
   * Shows authenticate window.
   * @param {String} [message] Message to be shown in authentication window
   * @param {Object} [options] TODO
   */
  showAuthenticateWindow: function (message, options) {
    var me = this,
        user = NX.State.getUser(),
        win;

    if (!me.getAuthenticate()) {
      win = me.getAuthenticateView().create({message: message, options: options});
      if (me.hasUser()) {
        win.down('form').getForm().setValues({username: user.id});
        win.down('#password').focus();
      }
    }
  },

  /**
   * @private
   */
  signIn: function (button) {
    var me = this,
        win = button.up('window'),
        form = button.up('form'),
        values = form.getValues(),
        b64username = NX.util.Base64.encode(values.username),
        b64password = NX.util.Base64.encode(values.password);

    win.getEl().mask(NX.I18n.get('GLOBAL_SIGN_IN_MASK'));

    me.logDebug('Sign-in user: "' + values.username + '" ...');

    Ext.Ajax.request({
      url: NX.util.Url.urlOf('service/rapture/session'),
      method: 'POST',
      params: {
        username: b64username,
        password: b64password,
        rememberMe: values.rememberMe
      },
      scope: me,
      suppressStatus: true,
      success: function() {
        win.getEl().unmask();
        NX.State.setUser({ id: values.username });
        win.close();

        // invoke optional success callback registered on window
        if (win.options && Ext.isFunction(win.options.success)) {
          win.options.success.call(win.options.scope, win.options);
        }
      },
      failure: function(response) {
        var message = NX.I18n.get('GLOBAL_SERVER_INCORRECT_CREDENTIALS_WARNING');
        if(response.status === 0) {
          message = NX.I18n.get('GLOBAL_SERVER_CONNECT_FAILURE');  
        }
        win.getEl().unmask();
        NX.Messages.add({
          text: message,
          type: 'warning'
        });
      }
    });
  },

  /**
   * @private
   */
  doAuthenticateAction: function (button) {
    var me = this,
        win = button.up('window');

    // invoke optional authenticateAction callback registered on window
    if (win.options && Ext.isFunction(win.options.authenticateAction)) {
      win.options.authenticateAction.call(me, button);
    }
  },

  // TODO: anything that may change the authentication/session should probably not be
  // TODO: done via extjs as it can batch, and the batch operation could implact the
  // TODO: sanity of the requests if authentication changes mid execution of batch operations

  /**
   * @private
   */
  authenticate: function (button) {
    var me = this,
        win = button.up('window'),
        form = button.up('form'),
        user = NX.State.getUser(),
        values = Ext.applyIf(form.getValues(), {username: user ? user.id : undefined}),
        b64username = NX.util.Base64.encode(values.username),
        b64password = NX.util.Base64.encode(values.password);

    win.getEl().mask(NX.I18n.get('GLOBAL_AUTHENTICATE_MASK'));

    me.logDebug('Authenticating user "' + values.username + '" ...');

    NX.direct.rapture_Security.authenticate(b64username, b64password, function (response) {
      win.getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        NX.State.setUser(response.data);
        win.close();

        // invoke optional success callback registered on window
        if (win.options && Ext.isFunction(win.options.success)) {
          win.options.success.call(win.options.scope, win.options);
        }
      }
    });
  },

  /**
   * @private
   */
  retrieveAuthenticationToken: function (button) {
    var me = this,
        win = button.up('window'),
        form = button.up('form'),
        user = NX.State.getUser(),
        values = Ext.applyIf(form.getValues(), {username: user ? user.id : undefined}),
        b64username = NX.util.Base64.encode(values.username),
        b64password = NX.util.Base64.encode(values.password);

    win.getEl().mask(NX.I18n.get('GLOBAL_AUTHENTICATE_RETRIEVING_MASK'));

    me.logDebug('Retrieving authentication token...');

    NX.direct.rapture_Security.authenticationToken(b64username, b64password, function (response) {
      win.getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        win.close();

        // invoke optional success callback registered on window
        if (win.options && Ext.isFunction(win.options.success)) {
          win.options.success.call(win.options.scope, response.data, win.options);
        }
      }
    });
  },

  /**
   * @private
   */
  onClickSignOut: function() {
    var me = this;

    if (me.fireEvent('beforesignout')) {
      me.signOut();
    }
  },

  /**
   * @public
   */
  signOut: function () {
    var me = this;

    me.logDebug('Sign-out');

    // TODO: Mask?

    Ext.Ajax.request({
      url: NX.util.Url.urlOf('service/rapture/session'),
      method: 'DELETE',
      scope: me,
      suppressStatus: true,
      success: function() {
        NX.State.setUser(undefined);
      }
    });
  },

  manageButtons: function () {
    var me = this,
        user = NX.State.getUser(),
        signInButton = me.getSignInButton(),
        signOutButton = me.getSignOutButton(),
        userButton = me.getUserButton();

    if (signInButton) {
      if (user) {
        signInButton.hide();
        userButton.up('nx-header-mode').show();
        userButton.setText(user.id);
        userButton.setTooltip(NX.I18n.format('GLOBAL_HEADER_USER_TOOLTIP', user.id));
        signOutButton.show();
      }
      else {
        signInButton.show();
        userButton.up('nx-header-mode').hide();
        signOutButton.hide();
      }
    }
  }

});
