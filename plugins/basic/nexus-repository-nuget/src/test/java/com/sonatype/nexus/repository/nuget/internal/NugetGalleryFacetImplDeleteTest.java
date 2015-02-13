package com.sonatype.nexus.repository.nuget.internal;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies component deletion from a nuget gallery facet.
 */
public class NugetGalleryFacetImplDeleteTest
{
  @Test
  public void deleteRemovesComponentAssetAndBlob() throws Exception {
    final String packageId = "screwdriver";
    final String version = "0.1.1";

    final NugetGalleryFacetImpl galleryFacet = spy(new NugetGalleryFacetImpl());

    final StorageTx tx = mock(StorageTx.class);
    doReturn(tx).when(galleryFacet).openStorageTx();

    final OrientVertex component = mock(OrientVertex.class);
    final OrientVertex asset = mock(OrientVertex.class);
    final BlobRef blobRef = new BlobRef("local", "default", "a34af31");

    // Wire the mock vertices together: component has asset, asset has blobRef
    doReturn(component).when(galleryFacet).findComponent(tx, packageId, version);
    when(tx.findAssets(eq(component))).thenReturn(asList(asset));
    when(asset.getProperty(eq(StorageFacet.P_BLOB_REF))).thenReturn(blobRef.toString());

    galleryFacet.delete(packageId, version);

    // Verify that everything got deleted
    verify(tx).deleteVertex(component);
    verify(tx).deleteVertex(asset);
    verify(tx).deleteBlob(eq(blobRef));
  }
}
