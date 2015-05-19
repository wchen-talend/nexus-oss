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
package org.sonatype.nexus.atlas.ui

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.constraints.NotNull

import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.supportzip.SupportZipGenerator
import org.sonatype.nexus.validation.Validate

import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions

/**
 * Support ZIP {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'atlas_SupportZip')
class SupportZipComponent
    extends DirectComponentSupport
{
  @Inject
  SupportZipGenerator supportZipGenerator

  /**
   * Create a support ZIP file.
   * @return a created ZIP file info
   */
  @DirectMethod
  @RequiresAuthentication
  @RequiresPermissions('nexus:atlas:create')
  @Validate
  SupportZipXO create(final @NotNull SupportZipGenerator.Request request) {
    def result = supportZipGenerator.generate(request)
    return new SupportZipXO(
        file: result.file.canonicalPath,
        name: result.file.name,
        size: result.file.length(),
        truncated: result.truncated
    )
  }
}
