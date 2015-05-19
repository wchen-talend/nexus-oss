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
package org.sonatype.nexus.security.role;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link RolesExist} validator.
 *
 * @since 3.0
 */
@Named
public class RolesExistValidator
    extends ConstraintValidatorSupport<RolesExist, Collection<?>> // Collection<String> expected
{
  private final SecuritySystem securitySystem;

  @Inject
  public RolesExistValidator(final SecuritySystem securitySystem) {
    this.securitySystem = checkNotNull(securitySystem);
  }

  @Override
  public boolean isValid(final Collection<?> value, final ConstraintValidatorContext context) {
    log.trace("Validating roles exist: {}", value);
    Set<String> ids = new HashSet<>();
    // TODO: sort out if we should limit to DEFAULT_SOURCE?
    for (Role role : securitySystem.listRoles()) {
      ids.add(role.getRoleId());
    }

    List<Object> missing = new LinkedList<>();
    for (Object item : value) {
      if (!ids.contains(item)) {
        missing.add(item);
      }
    }
    if (missing.isEmpty()) {
      return true;
    }

    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate("Missing roles: " + missing)
        .addConstraintViolation();
    return false;
  }
}
