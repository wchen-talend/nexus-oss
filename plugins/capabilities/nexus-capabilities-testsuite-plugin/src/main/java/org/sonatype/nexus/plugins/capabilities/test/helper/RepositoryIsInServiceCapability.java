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
package org.sonatype.nexus.plugins.capabilities.test.helper;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.capability.support.condition.Conditions;
import org.sonatype.nexus.capability.support.condition.RepositoryConditions.RepositoryName;

import static org.sonatype.nexus.capability.Tag.repositoryTag;
import static org.sonatype.nexus.capability.Tag.tags;
import static org.sonatype.nexus.plugins.capabilities.test.helper.MessageCapabilityDescriptor.REPOSITORY;

@Named(RepositoryIsInServiceCapabilityDescriptor.TYPE_ID)
public class RepositoryIsInServiceCapability
    extends TestCapability
    implements Capability, Taggable
{

  private final Conditions conditions;

  @Inject
  public RepositoryIsInServiceCapability(final Conditions conditions) {
    this.conditions = conditions;
  }

  @Override
  public Condition activationCondition() {
    return conditions.logical().and(
        conditions.repository().repositoryIsOnline(new RepositoryName()
        {
          @Override
          public String get() {
            return context().properties().get(REPOSITORY);
          }
        }),
        conditions.capabilities().passivateCapabilityDuringUpdate()
    );
  }

  @Override
  public Condition validityCondition() {
    return conditions.repository().repositoryExists(new RepositoryName()
    {
      @Override
      public String get() {
        return context().properties().get(REPOSITORY);
      }
    }
    );
  }

  @Override
  public Set<Tag> getTags() {
    return tags(repositoryTag(context().properties().get(REPOSITORY)));
  }

}
