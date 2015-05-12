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

package org.sonatype.nexus.repository.storage;

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityVersion;
import org.sonatype.nexus.orient.HexRecordIdObfuscator;
import org.sonatype.nexus.orient.PersistentDatabaseInstanceRule;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.search.SearchFacet;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.inject.util.Providers;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.entity.EntityHelper.id;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_NAME;

/**
 * Integration tests for {@link StorageFacetImpl}.
 */
public class StorageFacetImplIT
    extends TestSupport
{
  @Rule
  public PersistentDatabaseInstanceRule database = new PersistentDatabaseInstanceRule("test");

  protected StorageFacetImpl underTest;

  protected Repository testRepository1 = mock(Repository.class);

  protected Repository testRepository2 = mock(Repository.class);

  protected TestFormat testFormat = new TestFormat();

  private AssetEntityAdapter assetEntityAdapter;

  private class TestFormat
      extends Format
  {
    public TestFormat() {
      super("test");
    }
  }

  @Before
  public void setUp() throws Exception {
    BlobStoreManager mockBlobStoreManager = mock(BlobStoreManager.class);
    when(mockBlobStoreManager.get(anyString())).thenReturn(mock(BlobStore.class));
    BucketEntityAdapter bucketEntityAdapter = new BucketEntityAdapter();
    HexRecordIdObfuscator recordIdObfuscator = new HexRecordIdObfuscator();
    bucketEntityAdapter.installDependencies(recordIdObfuscator);
    ComponentEntityAdapter componentEntityAdapter = new ComponentEntityAdapter(bucketEntityAdapter);
    componentEntityAdapter.installDependencies(recordIdObfuscator);
    assetEntityAdapter = new AssetEntityAdapter(bucketEntityAdapter, componentEntityAdapter);
    assetEntityAdapter.installDependencies(recordIdObfuscator);
    underTest = new StorageFacetImpl(
        mockBlobStoreManager,
        Providers.of(database.getInstance()),
        bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter
    );
    underTest.installDependencies(mock(EventBus.class));

    StorageFacetImpl.Config config = new StorageFacetImpl.Config();
    ConfigurationFacet configurationFacet = mock(ConfigurationFacet.class);
    when(configurationFacet.readSection(
        any(Configuration.class),
        eq(StorageFacetImpl.CONFIG_KEY),
        eq(StorageFacetImpl.Config.class)))
        .thenReturn(config);

    when(testRepository1.getName()).thenReturn("test-repository-1");
    when(testRepository1.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
    when(testRepository1.facet(SearchFacet.class)).thenReturn(mock(SearchFacet.class));

    when(testRepository2.getName()).thenReturn("test-repository-2");
    when(testRepository2.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
    when(testRepository2.facet(SearchFacet.class)).thenReturn(mock(SearchFacet.class));

    underTest.attach(testRepository1);
    underTest.init();
    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    underTest.stop();
  }

  @Test
  public void initialState() {
    try (StorageTx tx = underTest.openTx()) {
      // We should have one bucket, which was auto-created for the repository during initialization
      checkSize(tx.browseBuckets(), 1);
    }
  }

  @Test
  public void startWithEmptyAttributes() {
    try (StorageTx tx = underTest.openTx()) {
      Asset asset = tx.createAsset(tx.getBucket(), testFormat);
      Component component = tx.createComponent(tx.getBucket(), testFormat);

      NestedAttributesMap assetAttributes = asset.attributes();
      assertThat(assetAttributes, is(notNullValue()));
      assertThat(assetAttributes.isEmpty(), is(true));

      NestedAttributesMap componentAttributes = component.attributes();
      assertThat(componentAttributes, is(notNullValue()));
      assertThat(componentAttributes.isEmpty(), is(true));
    }
  }

  @Test
  public void getAndSetAttributes() {
    EntityId docId;
    try (StorageTx tx = underTest.openTx()) {
      Asset asset = tx.createAsset(tx.getBucket(), testFormat);
      asset.name("asset");
      NestedAttributesMap map = asset.attributes();

      assertThat(map.isEmpty(), is(true));

      map.child("bag1").set("foo", "bar");
      map.child("bag2").set("baz", "qux");

      assertThat(map.isEmpty(), is(false));

      tx.saveAsset(asset);

      tx.commit();
      docId = asset.getEntityMetadata().getId();
    }

    try (StorageTx tx = underTest.openTx()) {
      NestedAttributesMap map = tx.findAsset(docId, tx.getBucket()).attributes();

      assertThat(map.size(), is(2));
      assertThat(map.child("bag1").size(), is(1));
      assertThat((String) map.child("bag1").get("foo"), is("bar"));
      assertThat(map.child("bag2").size(), is(1));
      assertThat((String) map.child("bag2").get("baz"), is("qux"));
    }
  }

  @Test
  public void findAssets() throws Exception {
    // Setup: add an asset in both repositories
    try (StorageTx tx = underTest.openTx()) {
      Asset asset1 = tx.createAsset(tx.getBucket(), testFormat);
      asset1.name("asset1");
      asset1.size(42L);
      tx.saveAsset(asset1);
      tx.commit();
    }

    underTest.attach(testRepository2);
    underTest.init();
    try (StorageTx tx = underTest.openTx()) {
      Asset asset2 = tx.createAsset(tx.getBucket(), testFormat);
      asset2.name("asset2");
      asset2.size(42L);
      tx.saveAsset(asset2);
      tx.commit();
    }

    // Queries
    try (StorageTx tx = underTest.openTx()) {

      // Find assets with name = "asset1"

      // ..in testRepository1, should yield 1 match
      checkSize(tx.findAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"),
          ImmutableSet.of(testRepository1), null), 1);
      assertThat(tx.countAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"),
          ImmutableSet.of(testRepository1), null), is(1L));
      // ...in testRepository2, should yield 0 matches
      checkSize(tx.findAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"),
          ImmutableSet.of(testRepository2), null), 0);
      assertThat(tx.countAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"),
          ImmutableSet.of(testRepository2), null), is(0L));
      // ..in testRepository1 or testRepository2, should yeild 1 match
      checkSize(tx.findAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"),
          ImmutableSet.of(testRepository1, testRepository2), null), 1);
      assertThat(tx.countAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"),
          ImmutableSet.of(testRepository1, testRepository2), null), is(1L));
      // ..in any repository should yeild 2 matches
      checkSize(tx.findAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"), null, null), 1);
      assertThat(tx.countAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"), null, null), is(1L));

      // Find assets with number = 42

      // ..in testRepository1, should yield 1 match
      checkSize(tx.findAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository1), null), 1);
      assertThat(tx.countAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository1), null), is(1L));
      // ..in testRepository2, should yield 1 match
      checkSize(tx.findAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository2), null), 1);
      assertThat(tx.countAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository2), null), is(1L));
      // ..in testRepository1 or testRepository2, should yield 2 matches
      checkSize(tx.findAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository1, testRepository2), null), 2);
      assertThat(tx.countAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository1, testRepository2), null), is(2L));
      // ..in any repository, should yield 2 matches
      checkSize(tx.findAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository1, testRepository2), null), 2);
      assertThat(tx.countAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository1, testRepository2), null), is(2L));

      // Find assets in any repository with name = "foo" or number = 42
      String whereClause = "name = :name or size = :number";
      Map<String, Object> parameters = ImmutableMap.of("name", (Object) "foo", "number", 42);

      // ..in ascending order by name with limit 1, should return asset1
      String suffix = "order by name limit 1";
      List<Asset> results = Lists.newArrayList(tx.findAssets(whereClause, parameters, null, suffix));
      checkSize(results, 1);
      assertThat((String) results.get(0).name(), is("asset1"));

      // ..in descending order by name with limit 1, should return asset2
      suffix = "order by name desc limit 1";
      results = Lists.newArrayList(tx.findAssets(whereClause, parameters, null, suffix));
      checkSize(results, 1);
      assertThat((String) results.get(0).name(), is("asset2"));
    }
  }

  @Test
  public void mapOfMaps() {
    Map<String, String> bag2 = ImmutableMap.of();

    // Transaction 1:
    // Create a new asset with property "attributes" that's a map of maps (stored as an embeddedmap)
    EntityId docId;
    try (StorageTx tx = underTest.openTx()) {
      Bucket bucket = tx.getBucket();
      Asset asset = tx.createAsset(bucket, testFormat);
      asset.name("asset");
      asset.attributes().child("bag1").set("foo", "bar");
      asset.attributes().child("bag2").set("baz", "qux");
      tx.saveAsset(asset);
      tx.commit();
      docId = asset.getEntityMetadata().getId();
    }

    // Transaction 2:
    // Get the asset and make sure it contains what we expect
    try (StorageTx tx = underTest.openTx()) {
      Bucket bucket = tx.getBucket();
      Asset asset = tx.findAsset(docId, bucket);
      assert asset != null;

      NestedAttributesMap outputMap = asset.attributes();

      assertThat(outputMap.size(), is(2));

      Map<String, String> outputBag1 = (Map<String, String>) outputMap.get("bag1");
      assertNotNull(outputBag1);
      assertThat(outputBag1.keySet().size(), is(1));
      assertThat(outputBag1.get("foo"), is("bar"));

      Map<String, String> outputBag2 = (Map<String, String>) outputMap.get("bag2");
      assertNotNull(outputBag2);
      assertThat(outputBag2.keySet().size(), is(1));
      assertThat(outputBag2.get("baz"), is("qux"));
    }

    // Transaction 3:
    // Make sure we can use dot notation to query for the asset by some aspect of the attributes
    try (StorageTx tx = underTest.openTx()) {
      Map<String, String> parameters = ImmutableMap.of("fooValue", "bar");
      String query = String.format("select from %s where attributes.bag1.foo = :fooValue", AssetEntityAdapter.DB_CLASS);

      Iterable<ODocument> docs = tx.getDb().command(new OCommandSQL(query)).execute(parameters);
      List<ODocument> list = Lists.newArrayList(docs);

      assertThat(list.size(), is(1));
      assertThat(assetEntityAdapter.encode(list.get(0).getIdentity()), is(docId));
    }
  }

  @Test
  public void roundTripTest() {
    EntityId asset1Id = null;
    EntityId asset2Id = null;
    EntityId componentId = null;

    try (StorageTx tx = underTest.openTx()) {
      // Verify initial state with browse
      Bucket bucket = tx.getBucket();

      checkSize(tx.browseBuckets(), 1);
      checkSize(tx.browseAssets(bucket), 0);
      checkSize(tx.browseComponents(bucket), 0);

      // Create an asset and component and verify state with browse and find
      Asset asset1 = tx.createAsset(bucket, testFormat);
      asset1.name("foo");
      tx.saveAsset(asset1);

      Component component = tx.createComponent(bucket, testFormat);
      component.name("bar");
      tx.saveComponent(component);

      Asset asset2 = tx.createAsset(bucket, component);
      asset2.name("asset2");
      tx.saveAsset(asset2);

      tx.commit();

      // In transaction mode, ORIDs are placeholders until commit, so IDs should be collected after commit
      asset1Id = id(asset1);
      asset2Id = id(asset2);
      componentId = id(component);
    }

    try (StorageTx tx = underTest.openTx()) {
      Bucket bucket = tx.getBucket();

      checkSize(tx.browseAssets(bucket), 2);
      checkSize(tx.browseComponents(bucket), 1);

      assertNotNull(tx.findAsset(asset1Id, bucket));
      assertNotNull(tx.findComponent(componentId, bucket));

      checkSize(tx.browseAssets(tx.findComponent(componentId, bucket)), 1);
      assertNotNull(tx.firstAsset(tx.findComponent(componentId, bucket)));
      assertNull(tx.findAsset(asset1Id, bucket).componentId());
      assertNotNull(tx.findAsset(asset2Id, bucket).componentId());

      assertNull(tx.findAssetWithProperty(P_NAME, "nomatch", bucket));
      assertNotNull(tx.findAssetWithProperty(P_NAME, "foo", bucket));

      assertNull(tx.findComponentWithProperty(P_NAME, "nomatch", bucket));
      assertNotNull(tx.findComponentWithProperty(P_NAME, "bar", bucket));

      // Delete both and make sure browse and find behave as expected
      tx.deleteAsset(tx.findAsset(asset1Id, bucket));
      tx.deleteComponent(tx.findComponent(componentId, bucket));

      checkSize(tx.browseAssets(bucket), 0);
      checkSize(tx.browseComponents(bucket), 0);
      assertNull(tx.findAsset(asset1Id, bucket));
      assertNull(tx.findComponent(componentId, bucket));

      // NOTE: It doesn't matter for this test, but you should commit when finished with one or more writes
      //       If you don't, your changes will be automatically rolled back.
      tx.commit();
    }
  }

  @Test
  public void componentAssetLinksAreDurable() {
    try (StorageTx tx = underTest.openTx()) {
      Bucket bucket = tx.getBucket();
      final Component component = tx.createComponent(bucket, testFormat).name("component");
      tx.saveComponent(component);

      final Asset asset = tx.createAsset(bucket, component).name("asset");
      tx.saveAsset(asset);

      tx.commit();
    }

    try (StorageTx tx = underTest.openTx()) {
      final Asset asset = tx.findAssetWithProperty("name", "asset", tx.getBucket());
      assertThat(asset, is(notNullValue()));

      final Component component = tx.findComponent(asset.componentId(), tx.getBucket());
      assertThat(component, is(notNullValue()));
      assertThat(component.name(), is("component"));
    }
  }

  @Test
  public void concurrentTransactionWithoutConflictTest() throws Exception {
    doConcurrentTransactionTest(false);
  }

  @Test
  public void concurrentTransactionWithConflictTest() throws Exception {
    doConcurrentTransactionTest(true);
  }

  private void doConcurrentTransactionTest(boolean simulateConflict) throws Exception {
    // setup:
    //   main thread: create a new asset and commit it.
    // test:
    //   main thread: start new transaction, and if simulating a conflict, read the asset
    //   aux thread: start new transaction, modify asset, and commit
    //   main thread: if not simulating a conflict, read the asset. then modify the asset, then commit it
    // expectation:
    //   if simulating a conflict: commit on main thread fails with OConcurrentModificationException
    //   if not simulating a conflict: modification made in main thread is persisted after the modification on aux

    // setup
    final EntityId assetId;
    EntityVersion firstVersion;
    try (StorageTx tx = underTest.openTx()) {
      Bucket bucket = tx.getBucket();
      Asset asset = tx.createAsset(bucket, testFormat);
      asset.name("asset");
      tx.saveAsset(asset);
      assetId = asset.getEntityMetadata().getId();
      tx.commit();
      firstVersion = asset.getEntityMetadata().getVersion();
    }

    // test
    // 1. start a tx (mainTx) in the main thread
    try (StorageTx mainTx = underTest.openTx()) {
      Bucket bucket = mainTx.getBucket();
      Asset asset = null;

      if (simulateConflict) {
        // cause a conflict to occur later by reading the asset before the other tx starts
        // (this causes the MVCC version comparison at commit-time to fail)
        asset = checkNotNull(mainTx.findAsset(assetId, bucket));
      }

      // 2. modify and commit the asset in a separate tx (auxTx) in another thread
      Thread auxThread = new Thread()
      {
        @Override
        public void run() {
          try (StorageTx auxTx = underTest.openTx()) {
            Bucket bucket = auxTx.getBucket();
            Asset asset = checkNotNull(auxTx.findAsset(assetId, bucket));
            asset.name("firstValue");
            auxTx.saveAsset(asset);
            auxTx.commit();
          }
        }
      };
      auxThread.start();
      auxThread.join();

      // 3. modify and commit the asset in mainTx, in the main thread
      if (!simulateConflict) {
        // only read the asset we propose to change *after* the other transaction completes
        asset = checkNotNull(mainTx.findAsset(assetId, bucket));
      }
      asset.name("secondValue");
      mainTx.saveAsset(asset);
      mainTx.commit(); // if we're simulating a conflict, this call should throw OConcurrentModificationException
      assertThat(simulateConflict, is(false));
    }
    catch (OConcurrentModificationException e) {
      assertThat(simulateConflict, is(true));
      return;
    }

    // not simulating a conflict; verify the expected state
    try (StorageTx tx = underTest.openTx()) {
      Bucket bucket = tx.getBucket();
      Asset asset = checkNotNull(tx.findAsset(assetId, bucket));

      String name = asset.name();
      EntityVersion finalVersion = asset.getEntityMetadata().getVersion();

      assertThat(name, is("secondValue"));
      assertThat(finalVersion, is(new EntityVersion(String.valueOf(Integer.valueOf(firstVersion.toString()) + 2))));
    }
  }

  @Test
  public void noDuplicateComponent() throws Exception {
    createComponent(null, "name", null);
    createComponent("group", "name", null);
    createComponent(null, "name", "1");
    createComponent("group", "name", "1");
  }

  @Test(expected = ORecordDuplicatedException.class)
  public void duplicateComponentName() throws Exception {
    createComponent(null, "name", null);
    createComponent(null, "name", null);
  }

  @Test(expected = ORecordDuplicatedException.class)
  public void duplicateComponentGroupName() throws Exception {
    createComponent("group", "name", null);
    createComponent("group", "name", null);
  }

  @Test(expected = ORecordDuplicatedException.class)
  public void duplicateComponentNameVersion() throws Exception {
    createComponent(null, "name", "1");
    createComponent(null, "name", "1");
  }

  @Test(expected = ORecordDuplicatedException.class)
  public void duplicateComponentGroupNameVersion() throws Exception {
    createComponent("group", "name", "1");
    createComponent("group", "name", "1");
  }

  private Component createComponent(final String group, final String name, final String version) throws Exception {
    try (StorageTx tx = underTest.openTx()) {
      Bucket bucket = tx.getBucket();
      Component component = tx.createComponent(bucket, testFormat)
          .group(group)
          .name(name)
          .version(version);
      tx.saveComponent(component);
      tx.commit();
      return component;
    }
  }

  private Asset createAsset(final Component component, final String name) throws Exception {
    try (StorageTx tx = underTest.openTx()) {
      Bucket bucket = tx.getBucket();
      Asset asset;
      if (component != null) {
        asset = tx.createAsset(bucket, component);
      }
      else {
        asset = tx.createAsset(bucket, testFormat);
      }
      asset.name(name);
      tx.saveAsset(asset);
      tx.commit();
      return asset;
    }
  }

  @Test
  public void noDuplicateAsset() throws Exception {
    Component component = createComponent("group", "name", "1");
    createAsset(component, "name");
    createAsset(null, "name");
  }


  @Test(expected = ORecordDuplicatedException.class)
  public void duplicateAssetComponentName() throws Exception {
    Component component = createComponent("group", "name", "1");
    createAsset(component, "name");
    createAsset(component, "name");
  }

  @Test(expected = ORecordDuplicatedException.class)
  public void duplicateAssetName() throws Exception {
    createAsset(null, "name");
    createAsset(null, "name");
  }

  private void checkSize(Iterable iterable, int expectedSize) {
    assertThat(Iterators.size(iterable.iterator()), is(expectedSize));
  }

  @Test
  public void repeatedAssetModificationsAreSaved() throws Exception {
    createComponent("testGroup", "testName", "testVersion");
    try (StorageTx tx = underTest.openTx()) {
      final Component component = tx.findComponentWithProperty("version", "testVersion", tx.getBucket());
      final Asset asset = tx.createAsset(tx.getBucket(), component).name("asset");

      final NestedAttributesMap attributes = asset.formatAttributes();
      attributes.set("attribute1", "original");
      tx.saveAsset(asset);

      final Iterable<Asset> assets = tx.browseAssets(component);
      final Asset reloadedAsset = assets.iterator().next();

      reloadedAsset.formatAttributes().set("attribute2", "alternate");
      tx.saveAsset(reloadedAsset);

      tx.commit();
    }

    try (StorageTx tx = underTest.openTx()) {
      final Component component = tx.findComponentWithProperty("version", "testVersion", tx.getBucket());
      final Iterable<Asset> assets = tx.browseAssets(component);
      final Asset asset = assets.iterator().next();

      assertThat(asset.formatAttributes().get("attribute1", String.class), equalTo("original"));
      assertThat(asset.formatAttributes().get("attribute2", String.class), equalTo("alternate"));
    }
  }

  @Test
  public void transactionsRollBackWhenRequired() throws Exception {
    try (StorageTx tx = underTest.openTx()) {
      final Component component = tx.createComponent(tx.getBucket(), testFormat)
          .group("myGroup")
          .version("0.9")
          .name("myComponent");
      tx.saveComponent(component);
      tx.rollback();
    }

    try (StorageTx tx = underTest.openTx()) {
      final Component component = tx.findComponentWithProperty("group", "myGroup", tx.getBucket());
      assertThat(component, is(nullValue()));
    }
  }

  @Test
  public void transactionsRollBackOnException() throws Exception {
    try {
      try (StorageTx tx = underTest.openTx()) {
        final Component component = tx.createComponent(tx.getBucket(), testFormat)
            .group("myGroup")
            .version("0.9")
            .name("myComponent");
        tx.saveComponent(component);
        throw new IllegalStateException();
      }
    }
    catch (IllegalStateException ignored) {
    }

    try (StorageTx tx = underTest.openTx()) {
      final Component component = tx.findComponentWithProperty("group", "myGroup", tx.getBucket());
      assertThat(component, is(nullValue()));
    }
  }

  @Test
  public void transactionContentIsSaved() throws Exception {
    try (StorageTx tx = underTest.openTx()) {
      final Component component = tx.createComponent(tx.getBucket(), testFormat)
          .group("myGroup")
          .version("0.9")
          .name("myComponent");
      tx.saveComponent(component);
      tx.commit();
    }

    try (StorageTx tx = underTest.openTx()) {
      final Iterable<Component> components = tx.browseComponents(tx.getBucket());
      final Component component = tx.findComponentWithProperty("group", "myGroup", tx.getBucket());
      assertThat(component, is(notNullValue()));
      assertThat(component.group(), is("myGroup"));
    }
  }

  @Test
  public void entityIdCanBeUsedInLaterTransactions() throws Exception {
    EntityId componentId;

    try (StorageTx tx = underTest.openTx()) {
      final Component component = tx.createComponent(tx.getBucket(), testFormat).name("component");
      tx.saveComponent(component);

      componentId = id(component);

      tx.commit();
    }

    try (StorageTx tx = underTest.openTx()) {
      final Component component = tx.findComponent(componentId, tx.getBucket());
      assertThat("component", component, is(notNullValue()));
      assertThat(component.name(), is("component"));
    }
  }

  @Test
  public void entityIdCanBeReferencedBeforeCommit() throws Exception {
    EntityId componentId;
    EntityId assetId;

    try (StorageTx tx = underTest.openTx()) {
      final Component component = tx.createComponent(tx.getBucket(), testFormat).name("component");
      tx.saveComponent(component);

      // Implicitly reference the component's entity id
      final Asset asset = tx.createAsset(tx.getBucket(), component).name("hello");
      tx.saveAsset(asset);

      tx.commit();
      componentId = id(component);
      assetId = id(asset);
    }

    try (StorageTx tx = underTest.openTx()) {
      final Component component = tx.findComponent(componentId, tx.getBucket());
      assertThat("component", component, is(notNullValue()));
      assertThat(component.name(), is("component"));

      final Asset asset = tx.findAsset(assetId, tx.getBucket());
      assertThat("asset", asset, is(notNullValue()));
      assertThat(asset.name(), is("hello"));
    }
  }

  @Test
  public void dependentQueryFromUncommittedComponentDoesNotThrowException() throws Exception {
    try (StorageTx tx = underTest.openTx()) {
      final Component component = tx.createComponent(tx.getBucket(), testFormat).name("component");
      tx.saveComponent(component);

      // Correct use of attached entity ids prevent an exception being thrown by this line
      tx.browseAssets(component);
    }
  }
}
