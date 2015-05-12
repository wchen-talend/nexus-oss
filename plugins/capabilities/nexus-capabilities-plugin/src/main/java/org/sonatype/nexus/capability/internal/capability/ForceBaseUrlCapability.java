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
package org.sonatype.nexus.capability.internal.capability;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.capability.support.CapabilitySupport;
import org.sonatype.nexus.capability.support.WithoutConfiguration;
import org.sonatype.nexus.common.app.BaseUrlManager;
import org.sonatype.nexus.common.app.SystemState;
import org.sonatype.nexus.common.app.SystemStatus;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.capability.support.WithoutConfiguration.WITHOUT_CONFIGURATION;

/**
 * Force Base URL capability.
 *
 * @since 3.0
 */
@Named(ForceBaseUrlDescriptor.TYPE_ID)
public class ForceBaseUrlCapability
    extends CapabilitySupport<WithoutConfiguration>
{
  private final BaseUrlManager baseUrlManager;

  private final Provider<SystemStatus> systemStatusProvider;

  @Inject
  public ForceBaseUrlCapability(final BaseUrlManager baseUrlManager,
                                final Provider<SystemStatus> systemStatusProvider)
  {
    this.baseUrlManager = checkNotNull(baseUrlManager);
    this.systemStatusProvider = checkNotNull(systemStatusProvider);
  }

  @Override
  protected WithoutConfiguration createConfig(final Map<String, String> properties) {
    return WITHOUT_CONFIGURATION;
  }

  @Override
  protected void onActivate(final WithoutConfiguration config) throws Exception {
    if (!baseUrlManager.isForce()) {
      baseUrlManager.setForce(true);
    }
  }

  @Override
  protected void onPassivate(final WithoutConfiguration config) throws Exception {
    if (SystemState.STOPPING != systemStatusProvider.get().getState()) {
      baseUrlManager.setForce(false);
    }
  }
}
