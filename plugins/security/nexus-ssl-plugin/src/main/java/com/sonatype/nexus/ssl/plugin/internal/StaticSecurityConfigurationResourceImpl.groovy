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
package com.sonatype.nexus.ssl.plugin.internal

import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.security.config.CPrivilege
import org.sonatype.nexus.security.config.MemorySecurityConfiguration
import org.sonatype.nexus.security.config.StaticSecurityConfigurationResource

/**
 * SSL plugin static security resource.
 *
 * @since 3.0
 */
@Named
@Singleton
class StaticSecurityConfigurationResourceImpl
    implements StaticSecurityConfigurationResource
{
  @Override
  MemorySecurityConfiguration getConfiguration() {
    return new MemorySecurityConfiguration(
        privileges: [
            new CPrivilege(
                id: 'ssl-truststore-all',
                type: 'application',
                properties: [
                    domain : 'ssl-truststore',
                    actions: '*',
                ]
            ),
            new CPrivilege(
                id: 'ssl-truststore-read',
                type: 'application',
                properties: [
                    domain : 'ssl-truststore',
                    actions: 'read',
                ]
            ),
            new CPrivilege(
                id: 'ssl-truststore-create',
                type: 'application',
                properties: [
                    domain : 'ssl-truststore',
                    actions: 'create,read'
                ]
            ),
            new CPrivilege(
                id: 'ssl-truststore-update',
                type: 'application',
                properties: [
                    domain : 'ssl-truststore',
                    actions: 'update,read'
                ]
            ),
            new CPrivilege(
                id: 'ssl-truststore-delete',
                type: 'application',
                properties: [
                    domain : 'ssl-truststore',
                    actions: 'delete,read'
                ]
            )
        ]
    )
  }
}

