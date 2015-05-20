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
package org.sonatype.nexus.repository.view;

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import static org.sonatype.nexus.repository.http.HttpMethods.PUT;

/**
 * A format-neutral error handler for some exceptions. These exceptions are meant to signal some response directly
 * mappable onto a HTTP response, usually some 4xx error code.
 *
 * @since 3.0
 */
public class ExceptionHandler
    extends ComponentSupport
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    try {
      return context.proceed();
    }
    catch (IllegalOperationException e) {
      log.debug("Illegal operation: {} {}", context.getRequest().getAction(), context.getRequest().getPath(), e);
      return HttpResponses.badRequest(e.getMessage());
    }
    catch (InvalidContentException e) {
      log.debug("Invalid content: {} {}", context.getRequest().getAction(), context.getRequest().getPath(), e);
      if (PUT.equals(context.getRequest().getAction())) {
        return HttpResponses.badRequest(e.getMessage());
      }
      else {
        return HttpResponses.notFound(e.getMessage());
      }
    }
  }
}
