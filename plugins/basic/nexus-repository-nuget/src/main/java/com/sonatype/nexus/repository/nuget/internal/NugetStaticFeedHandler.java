package com.sonatype.nexus.repository.nuget.internal;

import javax.annotation.Nonnull;

import com.sonatype.nexus.repository.nuget.internal.odata.ODataFeedUtils;

import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;

/**
 * @since 3.0
 */
public class NugetStaticFeedHandler
    extends AbstractNugetHandler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    final String requestUrl = context.getRequest().getRequestUrl();
    final String path = context.getRequest().getPath();
    final String repositoryBase = requestUrl.substring(0, requestUrl.length() - path.length());

    switch (path) {
      case "/":
        return xmlResponse(200, ODataFeedUtils.root(repositoryBase));
      case "/$metadata":
        return xmlResponse(200, ODataFeedUtils.metadata());
      default:
        throw new IllegalStateException();
    }
  }
}
