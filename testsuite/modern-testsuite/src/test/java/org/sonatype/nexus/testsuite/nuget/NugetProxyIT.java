package org.sonatype.nexus.testsuite.nuget;

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.tests.http.server.fluent.Server;

import org.eclipse.jetty.http.PathMap;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
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

  protected Server proxyServer;

  protected NugetClient nuget;

  @Before
  public void createProxyRepo()
      throws Exception
  {
    PathMap.setPathSpecSeparators(":");

    proxyServer = Server.withPort(0)
        .serve("/*").withBehaviours(error(200))

        .serve("/nuget/Packages/$count")
        .withBehaviours(file(resolveTestFile("proxy-count.txt")))

        .serve("/nuget/Search()/$count")
        .withBehaviours(file(resolveTestFile("search-count.txt")))

            // TODO: This is one argument for making NugetGallery.count take a feed name instead of a path
            //.serve("/nuget/Packages()/$count")
            //.withBehaviours(file(resolveTestFile("proxy-count.txt")))
            //
            //.serve("/nuget/Packages(Id='jQuery',Version='1.7.1')")
            //.withBehaviours(file(resolveTestFile("proxy-entry.xml")))
            //
            //.serve("/nuget/Packages")
            //.withBehaviours(file(resolveTestFile("proxy-response.xml")))

        .serve("/nuget/Search()")
        .withBehaviours(file(resolveTestFile("proxy-search.xml")))
        .serve("/nuget/Search")
        .withBehaviours(file(resolveTestFile("proxy-search.xml")))

        .start();

    final String remoteStorageUrl = proxyServer.getUrl().toExternalForm() + "/nuget";

    final Repository proxyRepo = createRepository(proxyConfig(PROXY_REPO_NAME, remoteStorageUrl));

    nuget = nugetClient(proxyRepo);
  }

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
    assertThat("count", count, is(32048));

    String feed = nuget.feedXml(VISUAL_STUDIO_INITIAL_FEED_QUERY);
    final List<Map<String, String>> entries = parseFeedXml(feed);

    final Map<String, String> jQuery = findById(entries, "jQuery");
    assertThat(jQuery, is(Matchers.notNullValue()));

  }

  private Map<String, String> findById(final List<Map<String, String>> entries, final String id) {
    for (Map<String, String> entry : entries) {
      if (id.equals(entry.get("ID"))) {
        return entry;
      }
    }
    return null;
  }

}


