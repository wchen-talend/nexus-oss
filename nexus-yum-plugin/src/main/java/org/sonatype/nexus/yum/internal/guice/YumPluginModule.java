/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.yum.internal.guice;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.inject.Named;

import org.sonatype.nexus.yum.Yum;
import org.sonatype.nexus.yum.internal.YumFactory;
import org.sonatype.nexus.yum.internal.YumImpl;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

@Named
public class YumPluginModule
    extends AbstractModule
{

    private static final int POOL_SIZE = 10;

    @Override
    protected void configure()
    {
        bind( ScheduledThreadPoolExecutor.class ).toInstance( new ScheduledThreadPoolExecutor( POOL_SIZE ) );
        install( new FactoryModuleBuilder().implement( Yum.class, YumImpl.class ).build( YumFactory.class ) );
    }

}
