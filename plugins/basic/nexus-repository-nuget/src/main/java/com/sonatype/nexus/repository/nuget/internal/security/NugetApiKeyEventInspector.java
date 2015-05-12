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
package com.sonatype.nexus.repository.nuget.internal.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventSubscriber;
import org.sonatype.nexus.security.UserPrincipalsExpired;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.apache.shiro.subject.SimplePrincipalCollection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Ensures that API keys for deleted users are removed.
 */
@Named
@Singleton
final class NugetApiKeyEventInspector
    implements EventSubscriber
{
  private final NugetApiKeyStore keyStore;

  @Inject
  public NugetApiKeyEventInspector(final NugetApiKeyStore keyStore) {
    this.keyStore = checkNotNull(keyStore);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void inspect(final UserPrincipalsExpired expiredEvent) {
    final String userId = expiredEvent.getUserId();
    if (userId != null) {
      keyStore.deleteApiKey(new SimplePrincipalCollection(userId, expiredEvent.getSource()));
    }
    else {
      keyStore.purgeApiKeys();
    }
  }
}
