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
package com.sonatype.nexus.repository.nuget.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.sonatype.nexus.repository.nuget.internal.odata.ODataConsumer;
import com.sonatype.nexus.repository.nuget.internal.odata.ODataUtils;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.search.ComponentMetadataFactory;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import org.apache.http.client.utils.URIBuilder;

import static com.google.common.base.Objects.equal;
import static java.lang.Math.min;

/**
 * @since 3.0
 */
public class NugetProxyGalleryFacet
    extends NugetGalleryFacetImpl
{
  private static final int TWO_PAGES = 2 * ODataUtils.PAGE_SIZE;

  private static final Pattern COUNT_REGEX = Pattern.compile("<m:count>(\\d*)</m:count>");

  private final NugetFeedFetcher fetcher;

  private Cache<QueryCacheKey, Integer> cache;

  public NugetProxyGalleryFacet(
      final ComponentMetadataFactory componentMetadataFactory, final NugetFeedFetcher fetcher)
  {
    super(componentMetadataFactory);
    this.fetcher = fetcher;
  }

  @Override
  protected void doConfigure() throws Exception {
    super.doConfigure();

    // TODO: Read the cache settings from configuration
    int cacheSize = 300;
    int cacheItemMaxAge = 3600;

    cache = CacheBuilder.newBuilder()
        .maximumSize(cacheSize)
        .expireAfterWrite(cacheItemMaxAge, TimeUnit.SECONDS)
        .build();
  }

  @Override
  public String feed(final String base, final String operation, final Map<String, String> query) {
    final Integer top = asInteger(query.get("$top"));
    final Integer skip = asInteger(query.get("$skip"));

    Map<String, String> remoteQuery = Maps.newHashMap(query);

    final boolean searching = "Search".equals(operation);
    if (searching && top != null && skip != null) {
      // If we're searching, then instead of passing on the request for whatever page of results the client is looking
      // for, just get the first two pages of results right away. This makes sure our metadata is sufficiently full of
      // results before we query it, which is important since we use a different search algorithm than nuget.org.
      remoteQuery.put("$top", "" + TWO_PAGES);
      remoteQuery.put("$skip", "0");
    }
    else if (top == null || top > TWO_PAGES) {
      // limit any unbounded queries to twice our local page size
      // limit any excessive queries to twice our local page size
      remoteQuery.put("$top", "" + TWO_PAGES);
    }

    final int remoteCount = passQueryToRemoteRepo(nugetQuery(operation, query), new FeedLoader(fetcher, this));

    final String feedXml = super.feed(base, operation, query);

    // Work out the number of results we should report to the client.
    // Note that nuget.org itself occasionally reports nonsensical results.
    final int localCount = parseCount(feedXml);

    int reportedCount = CountReportingPolicy.determineReportedCount(remoteCount, localCount, top, skip);
    if (searching) {
      // If we're searching, cap results at 40 like nuget.org.
      reportedCount = min(ODataUtils.PAGE_SIZE, reportedCount);
    }
    // TODO: refactor the 'feed' method so that XML generation is an outer step that uses a data structure
    // then we can invoke the inner thing, tweak 'inline count' and go, instead of search/replacing
    return feedXml.replaceFirst("<m:count>\\d*</m:count>", "<m:count>" + reportedCount + "</m:count>");
  }


  @Override
  public int count(final String path, final Map<String, String> parameters) {
    // How many artifacts are available
    return passQueryToRemoteRepo(nugetQuery(path, parameters), new CountFetcher(fetcher));
  }

  @Override
  public String entry(final String base, final String id, final String version) {
    return super.entry(base, id, version);
  }

  /**
   * Returns a relative URI for the NuGet query, properly encoding the query parameters.
   * e.g. {@code "Search()/$count?filter=whatever"}
   */
  private URI nugetQuery(String operation, Map<String, String> queryParams) {
    try {
      URIBuilder b = new URIBuilder();
      b.setPath(operation);

      for (Entry<String, String> param : queryParams.entrySet()) {
        b.addParameter(param.getKey(), param.getValue());
      }
      return b.build();
    }
    catch (URISyntaxException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Determines which of the repository IDs correspond to remote proxies, and queries (or populates) the count cache
   * using the supplied {@link RemoteQueryFactory}.
   *
   * @return the package count from the remote repo
   */
  private int passQueryToRemoteRepo(final URI path, final RemoteQueryFactory creator)
  {
    final Repository repo = getRepository();

    try {
      // TODO: Determine if we should talk to the remote based on its status

      final QueryCacheKey key = new QueryCacheKey(repo.getName(), path);
      return cache.get(key, creator.createValueLoader(repo, path));
    }
    catch (Exception e) {
      log.warn("{} attempting to contact proxied repository {}.", e.getClass().getSimpleName(), repo.getName());
      return 0;
    }
  }

  @Nullable
  private Integer asInteger(String value) {
    if (value == null) {
      return null;
    }
    try {
      return Integer.parseInt(value);
    }
    catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Return the <m:count> from an XML feed, or 0 if it's not present/fails to parse.
   */
  private int parseCount(final String feed) {
    final Matcher matcher = COUNT_REGEX.matcher(feed);
    if (!matcher.find()) {
      return 0;
    }

    final Integer integer = asInteger(matcher.group(1));
    return integer == null ? 0 : integer;
  }


  /**
   * A factory to create {@link Callable}s to populate the count cache.
   */
  private static abstract class RemoteQueryFactory
  {
    protected final NugetFeedFetcher fetcher;

    protected RemoteQueryFactory(final NugetFeedFetcher fetcher) {
      this.fetcher = fetcher;
    }

    /**
     * Create a value loader to populate the query/count cache in the case where there's no value currently cached.
     */
    public abstract Callable<Integer> createValueLoader(final Repository remote, final URI path);

    /**
     * Guava caches do not allow nulls, see {@link Cache#get}.
     */
    protected Integer nullToZero(Integer input) {return input == null ? 0 : input;}
  }

  /**
   * Queries the remote repository for feed information, storing entries in the supplied nuget gallery.
   */
  private static class FeedLoader
      extends RemoteQueryFactory
  {
    private final NugetWritableGallery gallery;

    private FeedLoader(final NugetFeedFetcher fetcher, final NugetWritableGallery gallery) {
      super(fetcher);
      this.gallery = gallery;
    }

    public Callable<Integer> createValueLoader(final Repository remote, final URI nugetQuery)
    {
      return new Callable<Integer>()
      {
        @Override
        public Integer call() throws Exception {
          Integer remoteCount = fetcher.cachePackageFeed(remote, nugetQuery, 2, true, new ODataConsumer()
          {
            @Override
            public void consume(final Map<String, String> data) {
              gallery.putMetadata(data);
            }
          });

          return nullToZero(remoteCount);
        }
      };
    }
  }

  /**
   * Queries remote repositories for a simple count of entries.
   */
  private static class CountFetcher
      extends RemoteQueryFactory
  {
    private CountFetcher(final NugetFeedFetcher fetcher) {
      super(fetcher);
    }

    public Callable<Integer> createValueLoader(final Repository remote, final URI nugetQuery)
    {
      return new Callable<Integer>()
      {
        @Override
        public Integer call() throws Exception {
          return nullToZero(fetcher.getCount(remote, nugetQuery));
        }
      };
    }
  }

  private static class QueryCacheKey
  {
    final String repoId;

    final URI path;

    private QueryCacheKey(final String repoId, final URI path) {
      this.repoId = repoId;
      this.path = path;
    }

    @Override
    public boolean equals(final Object o) {

      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      QueryCacheKey that = (QueryCacheKey) o;

      if (!equal(repoId, that.repoId)) {
        return false;
      }
      if (!equal(path, that.path)) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      return Objects.hash(path, repoId);
    }
  }
}
