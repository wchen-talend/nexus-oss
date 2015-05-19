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
package com.sonatype.nexus.ssl.model;

// FIXME: Find a better home for this

/**
 * Trust Store key.
 *
 * @since ssl 1.0
 */
public class TrustStoreKey
{

  public static final String HTTP_CTX_KEY = TrustStoreKey.class.getName();

  private final String value;

  public TrustStoreKey(final String value) {
    if (value == null) {
      throw new NullPointerException("Value cannot be null");
    }
    this.value = value;
  }

  public final String value() {
    return value;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TrustStoreKey)) {
      return false;
    }

    final TrustStoreKey that = (TrustStoreKey) o;

    if (!value.equals(that.value)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value();
  }

  protected static String checkNotNull(final String value, final String errorMessage) {
    if (value == null) {
      throw new NullPointerException(errorMessage);
    }
    return value;
  }

}
