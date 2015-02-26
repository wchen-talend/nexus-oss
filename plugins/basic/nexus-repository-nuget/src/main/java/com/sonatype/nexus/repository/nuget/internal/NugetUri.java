package com.sonatype.nexus.repository.nuget.internal;

import java.util.Map;

/**
 * @since 3.0
 */
public class NugetUri
{
  private final String operation;
  private final Map<String,String> queryParameters;

  public NugetUri(final String operation, final Map<String, String> queryParameters) {
    this.operation = operation;
    this.queryParameters = queryParameters;
  }
}
