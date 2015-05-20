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
package org.sonatype.nexus.repository.maven.internal

import javax.inject.Inject
import javax.inject.Provider

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.maven.MavenPathParser
import org.sonatype.nexus.repository.partial.PartialFetchHandler
import org.sonatype.nexus.repository.security.SecurityFacet
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.storage.StorageFacetImpl
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.ExceptionHandler
import org.sonatype.nexus.repository.view.Route.Builder
import org.sonatype.nexus.repository.view.handlers.TimingHandler

/**
 * Maven recipe support.
 *
 * @since 3.0
 */
abstract class MavenRecipeSupport
    extends RecipeSupport
{
  @Inject
  Provider<ConfigurableViewFacet> viewFacet

  @Inject
  Provider<StorageFacetImpl> storageFacet

  @Inject
  Provider<MavenFacetImpl> mavenFacet

  @Inject
  ExceptionHandler exceptionHandler

  @Inject
  TimingHandler timingHandler

  @Inject
  SecurityHandler securityHandler

  @Inject
  PartialFetchHandler partialFetchHandler

  @Inject
  MavenHeadersHandler mavenHeadersHandler

  final MavenPathParser mavenPathParser

  final Provider<SecurityFacet> securityFacet

  MavenRecipeSupport(Type type, Format format, MavenPathParser mavenPathParser, Provider<SecurityFacet> securityFacet) {
    super(type, format)
    this.mavenPathParser = mavenPathParser
    this.securityFacet = securityFacet
  }

  Builder newArtifactRouteBuilder() {
    return new Builder()
        .matcher(new MavenArtifactMatcher(mavenPathParser))
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(exceptionHandler)
  }

  Builder newMetadataRouteBuilder() {
    return new Builder()
        .matcher(new MavenMetadataMatcher(mavenPathParser))
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(exceptionHandler)
  }
}
