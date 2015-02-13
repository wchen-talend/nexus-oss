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
package com.sonatype.nexus.repository.nuget.internal;

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A handler for getting and putting NuGet packages.
 *
 * @since 3.0
 */
public class NugetItemHandler
    extends AbstractNugetHandler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    final Request request = context.getRequest();
    final String action = request.getAction();
    try {
      switch (action) {
        case HttpMethods.GET:
          checkArgument(false, "not implemented");
        default:
          return HttpResponses.methodNotAllowed(action, HttpMethods.GET /* TODO: , HttpMethods.GET */);
      }
    }
    catch (Exception e) {
      return convertToXmlError(e);
    }
  }
}
