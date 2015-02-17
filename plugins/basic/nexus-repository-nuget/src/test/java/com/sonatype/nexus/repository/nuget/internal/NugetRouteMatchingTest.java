package com.sonatype.nexus.repository.nuget.internal;

import org.sonatype.nexus.repository.view.matchers.token.TokenParser;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class NugetRouteMatchingTest
{
  @Test
  public void feedRoute() {
    final TokenParser tokenParser = new TokenParser(NugetHostedRecipe.FEED_PATTERN);

    String search = "/Search()";
    assertThat(tokenParser.parse(search), is(Matchers.notNullValue()));
  }

  @Test
  public void feedCountRoute(){
    final TokenParser tokenParser = new TokenParser(NugetHostedRecipe.FEED_COUNT_PATTERN);

    String search = "/Search()/$count";
    assertThat(tokenParser.parse(search), is(Matchers.notNullValue()));
  }
}