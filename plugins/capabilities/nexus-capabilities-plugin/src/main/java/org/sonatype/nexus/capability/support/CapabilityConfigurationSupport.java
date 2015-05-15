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
package org.sonatype.nexus.capability.support;

import java.net.URI;
import java.net.URISyntaxException;

import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;
import org.jetbrains.annotations.Nullable;

/**
 * Support for {@link Capability} configuration implementations.
 *
 * @since 2.7
 */
public abstract class CapabilityConfigurationSupport
    extends ComponentSupport
{
  protected boolean isEmpty(final String value) {
    return Strings2.isEmpty(value);
  }

  /**
   * Re-throws {@link URISyntaxException} as runtime exception.
   */
  protected URI parseUri(final String value) {
    try {
      return new URI(value);
    }
    catch (URISyntaxException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * If given value is null or empty, returns default.
   *
   * @see #parseUri(String)
   */
  protected URI parseUri(final String value, final @Nullable URI defaultValue) {
    if (isEmpty(value)) {
      return defaultValue;
    }
    return parseUri(value);
  }

  protected boolean parseBoolean(final String value, final boolean defaultValue) {
    if (!isEmpty(value)) {
      return Boolean.parseBoolean(value);
    }
    return defaultValue;
  }
}
