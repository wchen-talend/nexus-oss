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
package org.sonatype.nexus.ldap.internal.persist.orient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.NexusStartedEvent;
import org.sonatype.nexus.common.app.NexusStoppedEvent;
import org.sonatype.nexus.common.event.EventSubscriber;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages the {@link OrientDBLdapConfigurationSource} lifecycle.
 *
 * @since 3.0
 */
@Singleton
@Named
public class OrientDBLdapConfigurationSourceLifecycle
    implements EventSubscriber
{
  private final OrientDBLdapConfigurationSource orientDBLdapConfigurationSource;

  @Inject
  public OrientDBLdapConfigurationSourceLifecycle(final OrientDBLdapConfigurationSource orientDBLdapConfigurationSource)
  {
    this.orientDBLdapConfigurationSource = checkNotNull(orientDBLdapConfigurationSource);
  }

  @Subscribe
  public void on(final NexusStartedEvent evt) throws Exception {
    orientDBLdapConfigurationSource.start();
  }

  @Subscribe
  public void on(final NexusStoppedEvent evt) throws Exception {
    orientDBLdapConfigurationSource.stop();
  }
}