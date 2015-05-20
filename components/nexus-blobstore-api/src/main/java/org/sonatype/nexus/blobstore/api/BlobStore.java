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
package org.sonatype.nexus.blobstore.api;

import java.io.InputStream;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.AutoClosableIterable;
import org.sonatype.sisu.goodies.lifecycle.Lifecycle;

/**
 * A generic storage bin for binary objects of all sizes.
 *
 * In general, most methods can throw {@link BlobStoreException} for conditions such as network connectivity problems,
 * or file IO issues, blob store misconfiguration, or internal corruption.
 *
 * @since 3.0
 */
public interface BlobStore
    extends Lifecycle
{
  /**
   * An identifying name for disaster recovery purposes (which isn't required to be strictly unique)
   */
  String BLOB_NAME_HEADER = "BlobStore.blob-name";

  /**
   * Type of the content for disaster recovery purposes.
   */
  String CONTENT_TYPE_HEADER = "BlobStore.content-type";

  /**
   * Audit information (e.g. the name of a principal that created the blob)
   */
  String CREATED_BY_HEADER = "BlobStore.created-by";

  /**
   * Creates a new blob. The header map must contain at least two keys:
   *
   * <ul>
   * <li>{@link #BLOB_NAME_HEADER}</li>
   * <li>{@link #CREATED_BY_HEADER}</li>
   * </ul>
   *
   * @throws BlobStoreException       (or a subclass) if the input stream can't be read correctly
   * @throws IllegalArgumentException if mandatory headers are missing
   */
  Blob create(InputStream blobData, Map<String, String> headers);

  /**
   * Returns the corresponding {@link Blob}, or {@code null} if the  blob does not exist or has been {@link #delete
   * deleted}.
   */
  @Nullable
  Blob get(BlobId blobId);

  /**
   * Removes a blob from the blob store.  This may not immediately delete the blob from the underlying storage
   * mechanism, but will make it immediately unavailable to future calls to {@link BlobStore#get(BlobId)}.
   *
   * @return {@code true} if the blob has been deleted, {@code false} if no blob was found by that ID.
   */
  boolean delete(BlobId blobId);

  /**
   * Removes a blob from the blob store immediately, disregarding any locking or concurrent access by other threads.
   * This should be considered exceptional (e.g. administrative) usage.
   *
   * @return {@code true} if the blob has been deleted, {@code false} if no blob was found by that ID.
   */
  boolean deleteHard(BlobId blobId);

  /**
   * Provides metrics about the BlobStore's usage.
   */
  BlobStoreMetrics getMetrics();

  /**
   * Installs a listener to receive blob store events. Subsequent calls replace the listener.
   */
  void setBlobStoreListener(@Nullable BlobStoreListener listener);

  /**
   * Returns whatever BlobStoreListener has been installed, or {@code null}.
   */
  @Nullable
  BlobStoreListener getBlobStoreListener();

  /**
   * Perform garbage collection, purging blobs marked for deletion or whatever other periodic, implementation-specific
   * tasks need doing.
   */
  void compact();

  /**
   * Returns the configuration entity for the BlobStore.
   */
  BlobStoreConfiguration getBlobStoreConfiguration();

  /**
   * Initialize the BlobStore.
   */
  void init(BlobStoreConfiguration configuration) throws Exception;

  /**
   * Returns an Iterable of BlobIds.
   *
   * @return Iterable handle must be closed when finished using it.
   */
  AutoClosableIterable<BlobId> iterator();
}
