package com.sonatype.nexus.repository.nuget.internal;

import java.io.InputStream;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.repository.search.ComponentMetadataFactory;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;

import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Tests that nuget gallery 'get' behaves correctly.
 */
public class NugetGalleryFacetImplGetTest
{
  @Test
  public void testPayloadMadeFromBlob() throws Exception {
    final NugetGalleryFacetImpl galleryFacet = spy(new NugetGalleryFacetImpl(mock(ComponentMetadataFactory.class)));

    final String contentType = "application/zip";
    final long size = 2000000L;

    final String version = "2.1.1";
    final String packageId = "jQuery";

    final BlobRef blobRef = new BlobRef("a", "b", "c");
    final Blob blob = mock(Blob.class);
    final BlobMetrics blobMetrics = mock(BlobMetrics.class);
    when(blob.getMetrics()).thenReturn(blobMetrics);
    when(blobMetrics.getContentSize()).thenReturn(size);
    final InputStream blobStream = mock(InputStream.class);

    final OrientVertex asset = mock(OrientVertex.class);
    final OrientVertex component = mock(OrientVertex.class);

    final StorageTx tx = mock(StorageTx.class);
    doReturn(tx).when(galleryFacet).openStorageTx();
    doReturn(component).when(galleryFacet).findComponent(tx, packageId, version);
    doReturn(asset).when(galleryFacet).requireAsset(component);
    when(asset.getProperty(StorageFacet.P_CONTENT_TYPE)).thenReturn(contentType);
    when(asset.getProperty(StorageFacet.P_BLOB_REF)).thenReturn(blobRef.toString());
    when(tx.getBlob(eq(blobRef))).thenReturn(blob);
    when(blob.getInputStream()).thenReturn(blobStream);

    final Payload payload = galleryFacet.get(packageId, version);

    assertTrue(payload instanceof StreamPayload);
    StreamPayload streamPayload = (StreamPayload) payload;

    assertThat(streamPayload.openInputStream(), is(blobStream));
    assertThat(streamPayload.getSize(), is(size));
    assertThat(streamPayload.getContentType(), is(contentType));
  }
}
