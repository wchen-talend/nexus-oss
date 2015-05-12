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
package org.sonatype.nexus.wonderland.internal

import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicLong

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.wonderland.AuthTicketService
import org.sonatype.nexus.wonderland.DownloadService
import org.sonatype.sisu.goodies.common.ComponentSupport

/**
 * Default {@link DownloadService}.
 *
 * @since 2.8
 */
@Named
@Singleton
class DownloadServiceImpl
extends ComponentSupport
implements DownloadService
{

  /**
   * Counter used to generate unique names.
   */
  private static final AtomicLong counter = new AtomicLong()

  /**
   * Directory where files to be downloaded are stored.
   */
  private final File downloadDir

  private final AuthTicketService authTickets

  @Inject
  DownloadServiceImpl(final ApplicationDirectories applicationDirectories,
                      final AuthTicketService authTicketService)
  {
    assert applicationDirectories
    assert authTicketService

    // resolve where files to be downloaded will be stored
    downloadDir = applicationDirectories.getWorkDirectory('downloads')
    log.info 'Downloads directory: {}', downloadDir

    this.authTickets = authTicketService
  }

  @Override
  File getDirectory() {
    return downloadDir
  }

  @Override
  File get(String fileName, String authTicket) {
    log.info 'Download: {}', fileName

    if (!authTickets.redeemTicket(authTicket)) {
      throw new IllegalAccessException('Invalid authentication ticket')
    }

    def file = new File(downloadDir, fileName)

    // ensure we do not leak references outside of the downloads directory, only direct children can be served
    assert file.parentFile == downloadDir

    if (!file.exists() && file.isFile()) {
      log.warn 'File {} not found in download directory (or is not a file)', file
      return null
    }
    return file
  }

  @Override
  File move(File source, String name) {
    def target = new File(downloadDir, name)
    // ensure we only create files in downloads directory
    assert target.parentFile == downloadDir
    Files.move(source.toPath(), target.toPath())
    log.debug 'Moved {} to {}', source, target
    return target
  }

  @Override
  String uniqueName(String prefix) {
    return prefix + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + "-" + counter.incrementAndGet()
  }

}