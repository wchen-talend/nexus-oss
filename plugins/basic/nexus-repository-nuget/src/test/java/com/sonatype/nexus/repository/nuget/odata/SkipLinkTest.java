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
package com.sonatype.nexus.repository.nuget.odata;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.nexus.repository.nuget.internal.ComponentQuery;
import com.sonatype.nexus.repository.nuget.internal.NugetProperties;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @since 3.0
 */
public class SkipLinkTest
    extends TestSupport
{
  @Test
  public void roundTripSkipLinkParsing() throws Exception {
    // A query comes in, hypothetically having enough results that we need to paginate
    final HashMap<String, String> odataQuery = new HashMap<String, String>();
    odataQuery.put("$filter", "IsAbsoluteLatestVersion");
    odataQuery.put("$skip", "0");
    odataQuery.put("$top", "60");
    odataQuery.put("searchTerm", "jQuery");
    odataQuery.put("targetFramework", "'net45'");
    odataQuery.put("includePrerelease", "true");
    odataQuery.put("$orderBy", "downloadcount asc");

    // The final entry in the feed is used to generate a 'skip link', the URL for the next page
    final HashMap<String, Object> entry = hypotheticalFinalEntry();

    // Generate a skip link based on this entry
    final String skipLink = ODataFeedUtils.skipLinkQueryString(entry, odataQuery);

    // Parse the link into odata parameters
    Map<String, String> skipOdataQuery = parseLink("http://localhost/Search()?" + skipLink);

    // Now create an Orient component query from
    final ComponentQuery componentQuery = ODataUtils.query(skipOdataQuery, false);
  }

  @NotNull
  private HashMap<String, Object> hypotheticalFinalEntry() {
    final HashMap<String, Object> entry = new HashMap<String, Object>();

    entry.put(NugetProperties.P_ID, "jQuery");
    entry.put(NugetProperties.P_VERSION, "1.3.3");
    entry.put(NugetProperties.P_DOWNLOAD_COUNT, "2434");

    return entry;
  }

  @NotNull
  private HashMap<String, String> parseLink(final String url) throws Exception {
    List<NameValuePair> params = URLEncodedUtils.parse(new URI(url), "UTF-8");

    final HashMap<String, String> parsed = new HashMap<String, String>();
    for (NameValuePair param : params) {
      parsed.put(param.getName(), param.getValue());
    }

    return parsed;
  }
}

