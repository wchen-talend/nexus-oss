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
package org.sonatype.nexus.quartz.internal.capability;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.capability.Validator;
import org.sonatype.nexus.capability.support.CapabilityDescriptorSupport;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.quartz.internal.QuartzConstants;
import org.sonatype.sisu.goodies.i18n.I18N;
import org.sonatype.sisu.goodies.i18n.MessageBundle;

import static org.sonatype.nexus.capability.CapabilityType.capabilityType;
import static org.sonatype.nexus.capability.Tag.categoryTag;
import static org.sonatype.nexus.capability.Tag.tags;
import static org.sonatype.nexus.quartz.internal.QuartzConstants.CAPABILITY_CATEGORY_TAG;

/**
 * {@link SchedulerCapability} descriptor.
 *
 * @since 3.0
 */
@Named(QuartzConstants.CAPABILITY_ID)
@Singleton
public class SchedulerCapabilityDescriptor
    extends CapabilityDescriptorSupport
    implements Taggable
{
  public static final CapabilityType TYPE = capabilityType(QuartzConstants.CAPABILITY_ID);

  private static interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Quartz: Scheduler")
    String name();

    @DefaultMessage("Active")
    String activeLabel();

    @DefaultMessage("If active, jobs will be executed as scheduled. If not active, no jobs will be executed, scheduler is in \'stand-by\' mode.")
    String activeHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final FormField active;

  public SchedulerCapabilityDescriptor() {
    this.active = new CheckboxFormField(
        SchedulerCapabilityConfiguration.ACTIVE,
        messages.activeLabel(),
        messages.activeHelp(),
        FormField.OPTIONAL // Bug? Checkbox not sent when unchecked
    );
  }

  @Override
  public CapabilityType type() {
    return TYPE;
  }

  @Override
  public String name() {
    return messages.name();
  }

  @Override
  public List<FormField> formFields() {
    return Arrays.asList(
        active
    );
  }

  @Override
  public Validator validator() {
    // Allow only one capability of this type
    return validators().capability().uniquePer(TYPE);
  }

  @Override
  protected String renderAbout() throws Exception {
    return render(QuartzConstants.CAPABILITY_ID + "-about.vm");
  }

  @Override
  public Set<Tag> getTags() {
    return tags(categoryTag(CAPABILITY_CATEGORY_TAG));
  }

}
