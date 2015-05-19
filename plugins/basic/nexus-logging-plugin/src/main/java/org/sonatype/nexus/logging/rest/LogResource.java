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
package org.sonatype.nexus.logging.rest;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.log.LogManager;
import org.sonatype.siesta.Resource;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Log REST resource.
 *
 * @since 2.7
 */
@Named
@Singleton
@Path(LogResource.RESOURCE_URI)
public class LogResource
    extends ComponentSupport
    implements Resource
{
  public static final String RESOURCE_URI = "/logging/log";

  private final LogManager logManager;

  @Inject
  public LogResource(final LogManager logManager) {
    this.logManager = checkNotNull(logManager);
  }

  /**
   * Downloads a part of nexus.log (specified by fromByte/bytesCount) or full nexus.log (if fromByte/bytesCount are
   * null).
   *
   * @param fromByte   starting position
   * @param bytesCount number of bytes
   * @return part or full nexus.log
   * @throws Exception If getting log fails
   */
  @GET
  @Produces({TEXT_PLAIN})
  @RequiresPermissions("nexus:logging:read")
  public Response get(final @QueryParam("fromByte") Long fromByte,
                      final @QueryParam("bytesCount") Long bytesCount)
      throws Exception
  {
    Long from = fromByte;
    if (from == null || from < 0) {
      from = 0L;
    }
    Long count = bytesCount;
    if (count == null) {
      count = Long.MAX_VALUE;
    }
    InputStream log = logManager.getLogFileStream("nexus.log", from, count);
    if (log == null) {
      throw new NotFoundException("nexus.log not found");
    }
    return Response.ok(log)
        .header(CONTENT_DISPOSITION, "attachment; filename=\"nexus.log\"")
        .build();
  }
}