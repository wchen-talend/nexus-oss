package com.sonatype.nexus.repository.nuget.internal.odata;

import java.net.URI;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URIUtils;
import org.junit.Test;

/**
 * @since 3.0
 */
public class DeleteMeTest
{
  @Test
  public void foo() throws Exception{

    final URI repoUri = new URI("http://www.nuget.org/api/v2");

    final URI queryUri = new URI("count?foo=a&b=c");

    final URI resolve = repoUri.resolve(queryUri);



    System.err.println(resolve);
  }

  @Test
  public void foo2() throws Exception{
    URIBuilder n = new URIBuilder();
    n.setPath("Packages()/$count");
    n.addParameter("jody","&%@");

    System.err.println(n.build());
  }

}
