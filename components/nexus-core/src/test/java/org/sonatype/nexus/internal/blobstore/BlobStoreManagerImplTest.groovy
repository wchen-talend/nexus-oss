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
package org.sonatype.nexus.internal.blobstore

import javax.inject.Provider

import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreConfigurationStore
import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.sisu.litmus.testsupport.TestSupport

import com.google.common.collect.Lists
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock

import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.spy
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

/**
 * Tests for {@link BlobStoreManagerImpl}.
 */
class BlobStoreManagerImplTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder()

  @Mock
  ApplicationDirectories directories

  @Mock
  BlobStoreConfigurationStore store
  
  @Mock
  Provider<BlobStore> provider

  BlobStoreManagerImpl underTest

  @Before
  void setup() {
    when(directories.getWorkDirectory(anyString())).thenReturn(temporaryFolder.root)
    underTest = spy(new BlobStoreManagerImpl(directories, store, [test: provider, File: provider]))
  }

  @Test
  void 'Can start with nothing configured'() {
    when(store.list()).thenReturn(Lists.newArrayList())
    underTest.doStart()
    assert !underTest.browse()
  }

  @Test
  void 'Can start with existing configuration'() {
    BlobStore blobStore = mock(BlobStore)
    when(provider.get()).thenReturn(blobStore)
    when(store.list()).thenReturn(Lists.newArrayList(createConfig('test')))

    underTest.doStart()

    assert underTest.browse().toList() == [blobStore]
  }

  @Test
  void 'Can create a BlobStore'() {
    BlobStore blobStore = mock(BlobStore)
    when(provider.get()).thenReturn(blobStore)
    BlobStoreConfiguration configuration = createConfig('test')
    
    BlobStore createdBlobStore = underTest.create(configuration)

    assert createdBlobStore == blobStore
    verify(store).create(configuration)
    verify(blobStore).start()
    assert underTest.browse().toList() == [blobStore]
    assert underTest.get('test') == blobStore
  }

  @Test
  void 'Can delete an existing BlobStore'() {
    BlobStoreConfiguration configuration = createConfig('test')
    BlobStore blobStore = mock(BlobStore)
    doReturn(blobStore).when(underTest).blobStore('test')
    when(store.list()).thenReturn(Lists.newArrayList(configuration));
    
    underTest.delete(configuration)
    
    verify(blobStore).stop();
    verify(store).delete(configuration);
  }
  
  @Test
  void 'BlobStores will be eagerly created if not already configured'() {
    BlobStore blobStore = mock(BlobStore)
    when(provider.get()).thenReturn(blobStore)
    
    BlobStore autoCreatedBlobStore = underTest.get('test')
    
    verify(blobStore).start()
    verify(store).create(any(BlobStoreConfiguration))
    assert blobStore == autoCreatedBlobStore
  }

  @Test
  void 'Can delete an existing BlobStore by name'() {
    BlobStoreConfiguration configuration = createConfig('test')
    BlobStore blobStore = mock(BlobStore)
    doReturn(blobStore).when(underTest).blobStore('test')
    when(store.list()).thenReturn([configuration])
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration)
    
    underTest.delete(configuration.getName())
    
    verify(blobStore).stop()
    verify(store).delete(configuration)
  }

  @Test
  void 'All BlobStores are stopped with the manager is stopped'() {
    BlobStore blobStore = mock(BlobStore)
    when(provider.get()).thenReturn(blobStore)
    BlobStoreConfiguration configuration = createConfig('test')
    underTest.create(configuration)
    
    underTest.stop()
    
    verify(blobStore).stop()
  }

  private BlobStoreConfiguration createConfig(name = 'foo', attributes = [file:[path:'baz']]) {
    def entity = new BlobStoreConfiguration(
        name: name,
        type: 'test',
        attributes: attributes
    )
    return entity
  }
}
