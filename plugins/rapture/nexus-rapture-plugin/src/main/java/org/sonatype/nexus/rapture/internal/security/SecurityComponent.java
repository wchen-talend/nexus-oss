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
package org.sonatype.nexus.rapture.internal.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.authz.WildcardPermission2;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.validation.Validate;
import org.sonatype.nexus.wonderland.AuthTicketService;

import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.Subject;
import org.hibernate.validator.constraints.NotEmpty;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.rapture.internal.state.StateComponent.shouldSend;

/**
 * Security Ext.Direct component.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = "rapture_Security")
public class SecurityComponent
    extends DirectComponentSupport
    implements StateContributor
{
  private final SecuritySystem securitySystem;

  private final AnonymousManager anonymousManager;

  private final AuthTicketService authTickets;

  @Inject
  public SecurityComponent(final SecuritySystem securitySystem,
                           final AnonymousManager anonymousManager,
                           final AuthTicketService authTickets)
  {
    this.securitySystem = checkNotNull(securitySystem);
    this.anonymousManager = checkNotNull(anonymousManager);
    this.authTickets = checkNotNull(authTickets);
  }

  // FIXME: Move authenticate to session servlet

  @DirectMethod
  @Validate
  public UserXO authenticate(@NotEmpty final String base64Username, @NotEmpty final String base64Password)
      throws Exception
  {
    boolean rememberMe = false;
    Subject subject = securitySystem.getSubject();
    if (subject != null) {
      rememberMe = subject.isRemembered();
    }

    try {
      securitySystem.login(new UsernamePasswordToken(
          Strings2.decodeBase64(base64Username),
          Strings2.decodeBase64(base64Password),
          rememberMe
      ));
    }
    catch (Exception e) {
      throw new Exception("Authentication failed", e);
    }

    return getUser();
  }

  @DirectMethod
  @Validate
  public String authenticationToken(@NotEmpty final String base64Username, @NotEmpty final String base64Password)
      throws Exception
  {
    Subject subject = securitySystem.getSubject();
    if (subject == null || !subject.isAuthenticated()) {
      authenticate(base64Username, base64Password);
    }

    String username = Strings2.decodeBase64(base64Username);
    String password = Strings2.decodeBase64(base64Password);
    log.debug("Authenticate w/username: {}, password: {}", username, Strings2.mask(password));

    // Require current user to be the requested user to authenticate
    subject = securitySystem.getSubject();
    if (!subject.getPrincipal().toString().equals(username)) {
      throw new Exception("Username mismatch");
    }

    // Ask the sec-manager to authenticate, this won't alter the current subject
    try {
      securitySystem.getRealmSecurityManager().authenticate(new UsernamePasswordToken(username, password));
    }
    catch (AuthenticationException e) {
      throw new Exception("Authentication failed", e);
    }

    // At this point we should be authenticated, return a new ticket
    return authTickets.createTicket();
  }

  @DirectMethod
  public UserXO getUser() {
    UserXO userXO = null;

    Subject subject = securitySystem.getSubject();
    if (isLoggedIn(subject)) {
      userXO = new UserXO();
      userXO.setAuthenticated(subject.isAuthenticated());

      // HACK: roles for the current user are not exposed to the UI.
      // HACK: but we need to know if user is admin or not for some things (like outreach)
      if (subject.hasRole("admin")) {
        userXO.setAdministrator(true);
      }

      Object principal = subject.getPrincipal();
      if (principal != null) {
        userXO.setId(principal.toString());
        AnonymousConfiguration anonymousConfiguration = anonymousManager.getConfiguration();
        if (anonymousConfiguration.isEnabled() && userXO.getId().equals(anonymousConfiguration.getUserId())) {
          userXO = null;
        }
      }
    }
    return userXO;
  }

  @DirectMethod
  public List<PermissionXO> getPermissions() {
    List<PermissionXO> permissions = calculatePermissions();
    // store hash so we do not send later on a command to fetch
    shouldSend("permissions", permissions);
    return permissions;
  }

  public List<PermissionXO> calculatePermissions() {
    List<PermissionXO> permissions = null;
    Subject subject = securitySystem.getSubject();
    if (isLoggedIn(subject)) {
      permissions = calculatePermissions(subject);
    }
    return permissions;
  }

  @Override
  public Map<String, Object> getState() {
    Map<String, Object> state = new HashMap<>();
    state.put("user", getUser());

    AnonymousConfiguration anonymousConfiguration = anonymousManager.getConfiguration();
    state.put("anonymousUsername", anonymousConfiguration.isEnabled() ? anonymousConfiguration.getUserId() : null);
    return state;
  }

  @Override
  public Map<String, Object> getCommands() {
    HashMap<String, Object> commands = new HashMap<>();

    List<PermissionXO> permissions = calculatePermissions();
    if (permissions != null && shouldSend("permissions", permissions)) {
      commands.put("fetchpermissions", null);
    }

    return commands;
  }

  private boolean isLoggedIn(final Subject subject) {
    return subject != null && (subject.isRemembered() || subject.isAuthenticated());
  }

  // FIXME: Avoid calculating permissions for every poll request

  private List<PermissionXO> calculatePermissions(final Subject subject) {
    log.debug("Calculating permissions");

    List<Permission> granted = new ArrayList<>();
    List<PermissionXO> result = new ArrayList<>();

    // find all privileges which we expose the UI, , which we can deconstruct and evaluate
    for (Privilege privilege : securitySystem.listPrivileges()) {
      // only WildcardPermission2 presently is supported due to toString() implementation
      if (privilege.getPermission() instanceof WildcardPermission2) {
        granted.add(privilege.getPermission());
      }
    }

    // determine which of the exposed privilege permissions the current subject is granted
    boolean[] boolResults = subject.isPermitted(granted);
    for (int i = 0; i < granted.size(); i++) {
      if (boolResults[i]) {
        PermissionXO entry = new PermissionXO();
        entry.setId(granted.get(i).toString());
        result.add(entry);
      }
    }

    // FIXME: Permissions must be sorted for state-hash calculation :-(
    Collections.sort(result, new Comparator<PermissionXO>()
    {
      @Override
      public int compare(final PermissionXO o1, final PermissionXO o2) {
        return o1.getId().compareTo(o2.getId());
      }
    });

    return result;
  }
}
