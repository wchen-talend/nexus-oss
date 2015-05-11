package org.sonatype.nexus.testsuite.nuget;

import com.sonatype.nexus.repository.nuget.internal.proxy.NugetProxyRecipe;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.config.Configuration;

import org.jetbrains.annotations.NotNull;

/*
* Support for Nuget proxy ITs
 */
public abstract class NugetProxyITSupport
    extends NugetITSupport
{
  @NotNull
  protected Configuration proxyConfig(final String name, final String remoteUrl) {
    final Configuration config = new Configuration();
    config.setRepositoryName(name);
    config.setRecipeName(NugetProxyRecipe.NAME);
    config.setOnline(true);

    final NestedAttributesMap proxy = config.attributes("proxy");
    proxy.set("remoteUrl", remoteUrl);
    proxy.set("artifactMaxAge", 5);

    return config;
  }
}
