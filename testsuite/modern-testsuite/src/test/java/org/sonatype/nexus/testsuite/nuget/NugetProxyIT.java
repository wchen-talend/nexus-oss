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
package org.sonatype.nexus.testsuite.nuget;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.sonatype.nexus.repository.nuget.internal.NugetProperties;
import com.sonatype.nexus.repository.nuget.odata.ODataUtils;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.tests.http.server.fluent.Server;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.eclipse.jetty.http.PathMap;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static java.lang.Integer.parseInt;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.sonatype.tests.http.server.fluent.Behaviours.error;
import static org.sonatype.tests.http.server.fluent.Behaviours.file;

/**
 * Tests for Nuget proxy repositories.
 */
@ExamReactorStrategy(PerClass.class)
public class NugetProxyIT
    extends NugetProxyITSupport
{
  public static final String PROXY_REPO_NAME = "nuget-test-proxy";

  public static final String FEED_ORDERED_BY_HASH =
      "Search()?$filter=IsLatestVersion&$orderby=PackageHash%20asc,Id&$skip=0&$top=80&searchTerm=''&targetFramework='net45'&includePrerelease=false";

  public static final String TOP_100_RESULTS =
      "Search()?$filter=IsLatestVersion&$skip=0&$top=100&searchTerm=''&targetFramework='net45'&includePrerelease=false";

  protected Server proxyServer;

  protected NugetClient nuget;

  @Before
  public void createProxyRepo()
      throws Exception
  {
    PathMap.setPathSpecSeparators(":");

    proxyServer = Server.withPort(0)
        .serve("/*").withBehaviours(error(200))

        .serve("/nuget/Search()/$count")
        .withBehaviours(file(resolveTestFile("search-count.txt")))

            // Searches return the top 80 nuget.org results for 'jquery'
        .serve("/nuget/Search()")
        .withBehaviours(file(resolveTestFile("proxy-search-jquery-top-80.xml")))
        .serve("/nuget/Search")
        .withBehaviours(file(resolveTestFile("proxy-search-jquery-top-80.xml")))

        .start();

    final String remoteStorageUrl = proxyServer.getUrl().toExternalForm() + "/nuget";

    final Repository proxyRepo = createRepository(proxyConfig(PROXY_REPO_NAME, remoteStorageUrl));

    nuget = nugetClient(proxyRepo);
  }


// TODO TODO
  // replace the nuget.org references throughout all the test content!

  @After
  public void stopProxyServer()
      throws Exception
  {
    if (proxyServer != null) {
      proxyServer.stop();
    }
  }

  /**
   * Simple smoke test to ensure a proxy repo is actually reachable.
   */
  @Test
  public void proxyRepositoryIsAvailable() throws Exception {
    final String repositoryMetadata = nuget.getRepositoryMetadata();
    assertThat(repositoryMetadata, is(notNullValue()));
    assertThat(repositoryMetadata, containsString("<Schema Namespace=\"NuGetGallery\""));
  }

  /**
   * Visual Studio's default count and search queries.
   */
  @Test
  public void visualStudioInitializationQueries() throws Exception {
    int count = nuget.count(VISUAL_STUDIO_INITIAL_COUNT_QUERY);

    // Ensure the count reflects what is remotely available
    assertThat("count", count, is(32048));

    String feed = nuget.feedXml(VISUAL_STUDIO_INITIAL_FEED_QUERY);
    final List<Map<String, String>> entries = parseFeedXml(feed);

    assertThat(entries.size(), is(greaterThan(0)));

    final Map<String, String> jQuery = findById(entries, "jQuery");
    assertThat(jQuery, is(Matchers.notNullValue()));
  }

  /**
   * Results should arrive in the sort order we've specified.
   */
  @Test
  public void sortOrderIsRespected() throws Exception {
    final List<Map<String, String>> entries = parseFeedXml(nuget.feedXml(FEED_ORDERED_BY_HASH));

    List<String> receivedHashes = new ArrayList<>();

    for (Map<String, String> entry : entries) {
      final String hash = entry.get(NugetProperties.PACKAGE_HASH);
      receivedHashes.add(hash);
    }

    assertThat(receivedHashes, is(sorted(receivedHashes)));
  }

  /**
   * Ensuring the default sort order overrides database insertion order.
   */
  @Test
  public void repoImposesDefaultSortOrder() throws Exception {
    // Prime the proxy repo with a weird sort order
    final List<Map<String, String>> hashOrderedEntries = parseFeedXml(nuget.feedXml(FEED_ORDERED_BY_HASH));

    // Now ensure that an unspecified sort order results in default ordering (descending download count)
    final List<Map<String, String>> entries = parseFeedXml(nuget.feedXml(VISUAL_STUDIO_INITIAL_FEED_QUERY));

    List<Integer> downloadCounts = new ArrayList<>();
    for (Map<String, String> entry : entries) {
      downloadCounts.add(parseInt(entry.get(NugetProperties.DOWNLOAD_COUNT)));
    }

    final List<Integer> sorted = sortAscending(downloadCounts);
    assertThat(downloadCounts, is(sorted));
  }

  /**
   * Ensure searches cache more results than required, and still serve page 2+ when the remote is unavailable.
   */
  @Test
  public void browsingFeedsWithRemoteRepositoryUnavailable() throws Exception {

    // Warm up the proxy cache with a request while the proxy server is available
    nuget.vsSearchCount("jQuery");
    nuget.vsSearchFeedXml("jQuery");

    // Now the remote becomes unavailable
    proxyServer.stop();

    final String feed = nuget.feedXml(VISUAL_STUDIO_INITIAL_FEED_QUERY);

    final List<Map<String, String>> entries = parseFeedXml(feed);
    // Entries should still be served
    assertThat(entries.size(), is(VS_DEFAULT_PAGE_REQUEST_SIZE));
  }

  @Test
  public void feedsArePaginated() throws Exception {
    final String feed = nuget.feedXml(TOP_100_RESULTS);

    final String nextPageUrl = parseNextPageUrl(feed);

    assertThat(nextPageUrl, is(notNullValue()));

    final String pageTwo = nuget.feedXml(nextPageUrl);
    final List<Map<String, String>> entriesPageTwo = parseFeedXml(pageTwo);

    assertThat(entriesPageTwo.size(), is(ODataUtils.PAGE_SIZE));
  }

  @Test
  public void inlineCountIsProvided() throws Exception {
    // Ensure the server responds with an inline count, which it will do if we specify the $inlinecount=allpages option
    proxyServer.serve("/nuget/Search()").withBehaviours(file(resolveTestFile("packages-with-inline-count.xml")));
    proxyServer.serve("/nuget/Search").withBehaviours(file(resolveTestFile("packages-with-inline-count.xml")));

    // Request an inline count
    String feed = nuget.feedXml(VISUAL_STUDIO_INITIAL_FEED_QUERY + "&$inlinecount=allpages");
    final Integer integer = parseInlineCount(feed);

    assertThat(integer, is(Matchers.notNullValue()));
    // TODO: Packages() feeds should actually report the full result set size, not just the capped size
    assertThat(integer, is(greaterThanOrEqualTo(40)));
  }

  private Map<String, String> findById(final List<Map<String, String>> entries, final String id) {
    for (Map<String, String> entry : entries) {
      if (id.equals(entry.get(NugetProperties.ID))) {
        return entry;
      }
    }
    return null;
  }

  @NotNull
  private List<Integer> sortAscending(final List<Integer> downloadCounts) {
    final List<Integer> sorted = sorted(downloadCounts);
    Collections.reverse(sorted);
    return sorted;
  }

  @NotNull
  private <T> List<T> sorted(final List<T> unsorted)
  {
    return new ArrayList<T>(new TreeSet<T>(unsorted));
  }
}


