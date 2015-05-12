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
package org.sonatype.nexus.atlas.internal.customizers

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.log.LogManager
import org.sonatype.nexus.supportzip.FileContentSourceSupport
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport
import org.sonatype.nexus.supportzip.SupportBundle
import org.sonatype.nexus.supportzip.SupportBundleCustomizer
import org.sonatype.sisu.goodies.common.ComponentSupport

import static com.google.common.base.Preconditions.checkNotNull
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.DEFAULT
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.LOW
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.LOG

/**
 * Adds log files to support bundle.
 *
 * @since 2.7
 */
@Named
@Singleton
class LogCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private final LogManager logManager

  private final ApplicationDirectories applicationDirectories

  @Inject
  LogCustomizer(final LogManager logManager,
                final ApplicationDirectories applicationDirectories)
  {
    this.logManager = checkNotNull(logManager)
    this.applicationDirectories = checkNotNull(applicationDirectories)
  }

  @Override
  void customize(final SupportBundle supportBundle) {
    // add source for nexus.log
    supportBundle << new GeneratedContentSourceSupport(LOG, 'log/nexus.log', LOW) {
      @Override
      protected void generate(final File file) {
        def log = logManager.getLogFileStream('nexus.log', 0, Long.MAX_VALUE)
        log.withStream { input ->
          file.withOutputStream { output ->
            output << input
          }
        }
      }
    }

    // helper to include a file
    def maybeIncludeFile = { File file, String prefix, Priority priority = DEFAULT ->
      if (file.exists()) {
        log.debug 'Including file: {}', file
        supportBundle << new FileContentSourceSupport(LOG, "$prefix/${file.name}", file, priority)
      }
      else {
        log.debug 'Skipping non-existent file: {}', file
      }
    }

    // include karaf.log
    if (System.properties['karaf.log']) {
      maybeIncludeFile new File(System.getProperty('karaf.log')), 'log', LOW
    }

    // include request.log
    maybeIncludeFile new File(applicationDirectories.workDirectory, 'logs/request.log'), 'log', LOW
  }
}
