/*
 * Copyright (c) 2008-2015 Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package com.sonatype.nexus.repository.nuget.internal.odata;

import java.util.Map;

/**
 * Represents a consumer of OData.
 */
public interface ODataConsumer
{
  /**
   * Consumes the given OData (in key-value form).
   *
   * @param data Key-value data
   */
  void consume(Map<String, String> data);
}
