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

package org.sonatype.nexus.repository.maven.internal

import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.manager.DefaultRepositoriesContributor
import org.sonatype.nexus.repository.maven.internal.maven2.Maven2GroupRecipe
import org.sonatype.nexus.repository.maven.internal.maven2.Maven2HostedRecipe
import org.sonatype.nexus.repository.maven.internal.maven2.Maven2ProxyRecipe
import org.sonatype.nexus.repository.maven.policy.VersionPolicy
import org.sonatype.nexus.repository.storage.WritePolicy

/**
 * Provide default hosted and proxy repositories for Maven.
 *
 * @since 3.0
 */
@Named
@Singleton
class MavenDefaultRepositoriesContributor
    implements DefaultRepositoriesContributor
{
  static final String DEFAULT_RELEASE_REPO = 'maven-releases'

  static final String DEFAULT_SNAPSHOT_REPO = 'maven-snapshots'

  static final String DEFAULT_CENTRAL_REPO = 'maven-central'

  static final String DEFAULT_PUBLIC_REPO = 'maven-public'

  @Override
  List<Configuration> getRepositoryConfigurations() {
    return [
        new Configuration(repositoryName: DEFAULT_RELEASE_REPO, recipeName: Maven2HostedRecipe.NAME, online: true, attributes:
            [
                maven  : [
                    versionPolicy              : VersionPolicy.RELEASE.toString(),
                    strictContentTypeValidation: false
                ],
                storage: [
                    writePolicy: WritePolicy.ALLOW_ONCE.toString()
                ]
            ]
        ),
        new Configuration(repositoryName: DEFAULT_SNAPSHOT_REPO, recipeName: Maven2HostedRecipe.NAME, online: true, attributes:
            [
                maven  : [
                    versionPolicy              : VersionPolicy.SNAPSHOT.toString(),
                    strictContentTypeValidation: false
                ],
                storage: [
                    writePolicy: WritePolicy.ALLOW.toString()
                ]
            ]
        ),
        new Configuration(repositoryName: DEFAULT_CENTRAL_REPO, recipeName: Maven2ProxyRecipe.NAME, online: true, attributes:
            [
                maven     : [
                    versionPolicy              : VersionPolicy.RELEASE.toString(),
                    strictContentTypeValidation: false
                ],
                proxy     : [
                    remoteUrl     : 'https://repo1.maven.org/maven2/',
                    artifactMaxAge: 3600
                ]
            ]
        ),
        new Configuration(repositoryName: DEFAULT_PUBLIC_REPO, recipeName: Maven2GroupRecipe.NAME, online: true, attributes:
            [
                maven  : [
                    versionPolicy              : VersionPolicy.MIXED.toString(),
                    strictContentTypeValidation: false
                ],
                group  : [
                    memberNames: [DEFAULT_RELEASE_REPO, DEFAULT_SNAPSHOT_REPO, DEFAULT_CENTRAL_REPO]
                ]
            ]
        )
    ]
  }
}
