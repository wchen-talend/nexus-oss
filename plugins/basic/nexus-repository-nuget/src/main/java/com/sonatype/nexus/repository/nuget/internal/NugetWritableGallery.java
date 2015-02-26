package com.sonatype.nexus.repository.nuget.internal;

import java.util.Map;

/**
 * @since 3.0
 */
public interface NugetWritableGallery
{
  void putMetadata(Map<String, String> metadata);
}
