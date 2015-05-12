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
package org.sonatype.nexus.pax.exam;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.event.EventSubscriberHost;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.sisu.litmus.testsupport.junit.TestDataRule;
import org.sonatype.sisu.litmus.testsupport.junit.TestIndexRule;
import org.sonatype.sisu.litmus.testsupport.port.PortRegistry;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;
import org.ops4j.pax.exam.options.MavenUrlReference;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOptions;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

/**
 * Provides support for testing Nexus with Pax-Exam, test-cases can inject any component from the distribution. <br>
 * <br>
 * Extend this class and choose the base distribution (and any optional plugins) that you want to test against:
 * 
 * <pre>
 * &#064;Configuration
 * public static Option[] config() {
 *   return options(
 *       nexusDistribution(&quot;org.sonatype.nexus.assemblies&quot;, &quot;nexus-base-template&quot;),
 *       nexusPlugin(&quot;org.sonatype.nexus.plugins&quot;, &quot;nexus-repository-raw&quot;)
 *   );
 * }
 * </pre>
 * 
 * @since 3.0
 */
@RunWith(PaxExam.class)
public abstract class NexusPaxExamSupport
{
  public static final String BASEDIR = new File(System.getProperty("basedir", "")).getAbsolutePath();

  public static final String NEXUS_PAX_EXAM_TIMEOUT_KEY = "nexus.pax.exam.timeout";

  public static final int NEXUS_PAX_EXAM_TIMEOUT_DEFAULT = 300000;

  // -------------------------------------------------------------------------

  @Rule
  public final TestDataRule testData = new TestDataRule(resolveBaseFile("src/test/it-resources"));

  @Rule
  public final TestIndexRule testIndex = new TestIndexRule(resolveBaseFile("target/it-reports"),
      resolveBaseFile("target/it-data"));

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  public static final PortRegistry portRegistry = new PortRegistry();

  @Inject
  @Named("http://127.0.0.1:${application-port}${nexus-context-path}")
  protected URL nexusUrl;

  @Inject
  @Named("https://127.0.0.1:${application-port-ssl}${nexus-context-path}")
  protected URL nexusSecureUrl;

  @Inject
  protected ApplicationDirectories applicationDirectories;

  @Inject
  protected EventSubscriberHost eventSubscriberHost;

  @Inject
  protected TaskScheduler taskScheduler;

  // -------------------------------------------------------------------------

  /**
   * Resolves path against the basedir of the surrounding Maven project.
   */
  public static File resolveBaseFile(final String path) {
    return Paths.get(BASEDIR, path).toFile();
  }

  /**
   * Resolves path by searching the it-resources of the Maven project.
   * 
   * @see TestDataRule#resolveFile(String)
   */
  public File resolveTestFile(final String path) {
    return testData.resolveFile(path);
  }

  /**
   * Resolves path against the Nexus application directory.
   */
  public File resolveAppFile(final String path) {
    return new File(applicationDirectories.getAppDirectory(), path);
  }

  /**
   * Resolves path against the Nexus work directory.
   */
  public File resolveWorkFile(final String path) {
    return new File(applicationDirectories.getWorkDirectory(), path);
  }

  /**
   * Resolves path against the Nexus temp directory.
   */
  public File resolveTempFile(final String path) {
    return new File(applicationDirectories.getTemporaryDirectory(), path);
  }

  /**
   * Resolves path against the given URL.
   */
  public static URL resolveUrl(final URL url, final String path) {
    try {
      return URI.create(url + "/" + path).normalize().toURL();
    }
    catch (final Exception e) {
      throw Throwables.propagate(e);
    }
  }

  // -------------------------------------------------------------------------

  /**
   * Periodically polls function until it returns {@code true} or 30 seconds have elapsed.
   * 
   * @throws InterruptedException if the thread is interrupted or the timeout exceeded
   */
  public static void waitFor(final Callable<Boolean> function) throws InterruptedException {
    waitFor(function, 30000);
  }

  /**
   * Periodically polls function until it returns {@code true} or the timeout has elapsed.
   * 
   * @throws InterruptedException if the thread is interrupted or the timeout exceeded
   */
  public static void waitFor(final Callable<Boolean> function, final long millis) throws InterruptedException {
    Thread.yield();
    for (int i = 0; i < millis / 100; i++) {
      try {
        if (Boolean.TRUE.equals(function.call())) {
          return; // success
        }
        Thread.sleep(100);
      }
      catch (final InterruptedException e) {
        throw e; // cancelled
      }
      catch (final Exception e) {
        Thread.sleep(100);
      }
    }
    throw new InterruptedException();
  }

  /**
   * @return Function that returns {@code true} when there's a response from the URL; otherwise {@code false}
   */
  public static Callable<Boolean> responseFrom(final URL url) {
    return new Callable<Boolean>()
    {
      public Boolean call() throws Exception {
        HttpURLConnection conn = null;
        try {
          conn = (HttpURLConnection) url.openConnection();
          conn.setRequestMethod("HEAD");
          conn.connect();
          return true;
        }
        finally {
          if (conn != null) {
            conn.disconnect();
          }
        }
      }
    };
  }

  /**
   * @return Function that returns {@code true} when the event system is calm; otherwise {@code false}
   */
  protected Callable<Boolean> calmPeriod() {
    return new Callable<Boolean>()
    {
      public Boolean call() throws Exception {
        return eventSubscriberHost.isCalmPeriod();
      }
    };
  }

  /**
   * @return Function that returns {@code true} when all tasks have stopped; otherwise {@code false}
   */
  protected Callable<Boolean> tasksDone() {
    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
    return new Callable<Boolean>()
    {
      public Boolean call() throws Exception {
        return taskScheduler.getRunningTaskCount() == 0;
      }
    };
  }

  // -------------------------------------------------------------------------

  /**
   * @return Pax-Exam option to install a Nexus distribution based on groupId and artifactId
   */
  public static Option nexusDistribution(final String groupId, final String artifactId) {
    return nexusDistribution(maven(groupId, artifactId).versionAsInProject().type("zip"));
  }

  /**
   * @return Pax-Exam option to install a Nexus distribution based on groupId, artifactId and classifier
   */
  public static Option nexusDistribution(final String groupId, final String artifactId, final String classifier) {
    return nexusDistribution(maven(groupId, artifactId).classifier(classifier).versionAsInProject().type("zip"));
  }

  /**
   * @return Pax-Exam option to install a Nexus distribution from the given framework zip
   */
  public static Option nexusDistribution(final MavenUrlReference frameworkZip) {

    // support explicit CI setting as well as automatic detection
    String localRepo = System.getProperty("maven.repo.local", "");
    if (localRepo.length() > 0) {
      // pass on explicit setting to Pax-URL (otherwise it uses wrong value)
      System.setProperty("org.ops4j.pax.url.mvn.localRepository", localRepo);
    }
    else {
      // use placeholder in karaf config
      localRepo = "${maven.repo.local}";
    }

    // allow overriding the distribution under test from the command-line
    if (System.getProperty("it.nexus.bundle.groupId") != null) {
      frameworkZip.groupId(System.getProperty("it.nexus.bundle.groupId"));
    }
    if (System.getProperty("it.nexus.bundle.artifactId") != null) {
      frameworkZip.artifactId(System.getProperty("it.nexus.bundle.artifactId"));
    }
    if (System.getProperty("it.nexus.bundle.version") != null) {
      frameworkZip.version(System.getProperty("it.nexus.bundle.version"));
    }
    if (System.getProperty("it.nexus.bundle.classifier") != null) {
      frameworkZip.classifier(System.getProperty("it.nexus.bundle.classifier"));
    }

    boolean debugging = Boolean.parseBoolean(System.getProperty("it.debug"));

    return composite(

        vmOptions("-Xmx400m", "-XX:MaxPermSize=192m"), // taken from testsuite config

        systemTimeout(getExamTimeout()),

        propagateSystemProperty(NEXUS_PAX_EXAM_TIMEOUT_KEY),

        systemProperty("basedir").value(BASEDIR),

        karafDistributionConfiguration() //
            .karafVersion("3") //
            .frameworkUrl(frameworkZip) //
            .unpackDirectory(resolveBaseFile("target/it-data")) //
            .useDeployFolder(false), //

        when(debugging).useOptions(debugConfiguration()), // port 5005, suspend=y

        configureConsole().ignoreLocalConsole().ignoreRemoteShell(), // no need for console

        doNotModifyLogConfiguration(), // don't mess with our logging

        keepRuntimeFolder(), // keep files around in case we need to debug

        editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", // so pax-exam can fetch its feature
            "org.ops4j.pax.url.mvn.repositories", "https://repo1.maven.org/maven2@id=central"),

        editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", // so we can fetch local snapshots
            "org.ops4j.pax.url.mvn.localRepository", localRepo),

        useOwnKarafExamSystemConfiguration("nexus"),

        nexusPaxExam(), // registers invoker factory that waits for nexus to start before running tests

        // merge hamcrest-library extras with the core hamcrest bundle from Pax
        wrappedBundle(maven("org.hamcrest", "hamcrest-library").versionAsInProject()) //
            .instructions("Fragment-Host=org.ops4j.pax.tipi.hamcrest.core"),

        // move work directory inside unpacked distribution
        editConfigurationFilePut("etc/nexus.properties", //
            "nexus-work", "${nexus-base}/sonatype-work/nexus"),

        // randomize ports...
        editConfigurationFilePut("etc/nexus.properties", //
            "application-port", Integer.toString(portRegistry.reservePort())),
        editConfigurationFilePut("etc/nexus.properties", //
            "application-port-ssl", Integer.toString(portRegistry.reservePort())),
        editConfigurationFilePut("etc/org.apache.karaf.management.cfg", //
            "rmiRegistryPort", Integer.toString(portRegistry.reservePort())),
        editConfigurationFilePut("etc/org.apache.karaf.management.cfg", //
            "rmiServerPort", Integer.toString(portRegistry.reservePort()))

    );
  }

  /**
   * @return Pax-Exam option to change the context path for the Nexus distribution
   */
  public static Option withContextPath(final String contextPath) {
    return editConfigurationFilePut("etc/nexus.properties", "nexus-context-path", contextPath);
  }

  /**
   * @return Pax-Exam option to enable HTTPS support in the Nexus distribution
   */
  public static Option withHttps() {
    return composite(
        editConfigurationFileExtend("etc/custom.properties", "nexus-args", "${karaf.base}/etc/jetty-https.xml"),
        replaceConfigurationFile("etc/ssl/keystore.jks", resolveBaseFile("src/test/it-resources/ssl/nexus.jks")));
  }

  /**
   * @return Pax-Exam option to install a Nexus plugin based on groupId and artifactId
   */
  public static Option nexusPlugin(final String groupId, final String artifactId) {
    return nexusPlugin(maven(groupId, artifactId).versionAsInProject().classifier("features").type("xml"), artifactId);
  }

  /**
   * @return Pax-Exam option to install a Nexus plugin from the given feature XML and name
   */
  public static Option nexusPlugin(final MavenUrlReference featureXml, final String name) {
    return composite(features(featureXml), editConfigurationFileExtend("etc/nexus.properties", "nexus-features", name));
  }

  /**
   * Replacement for {@link CoreOptions#options(Option...)} to workaround Pax-Exam 'feature'
   * where only the last 'editConfigurationFileExtend' for a given property key is honoured.
   */
  public static Option[] options(final Option... options) {
    final List<Option> result = new ArrayList<>();

    // filter out the individual nexus-features values
    final List<String> nexusFeatures = new ArrayList<>();
    for (final Option o : OptionUtils.expand(options)) {
      if (o instanceof KarafDistributionConfigurationFileExtendOption) {
        if ("nexus-features".equals(((KarafDistributionConfigurationFileExtendOption) o).getKey())) {
          nexusFeatures.add(((KarafDistributionConfigurationFileExtendOption) o).getValue());
          continue;
        }
      }
      result.add(o);
    }

    if (nexusFeatures.size() > 0) {
      // combine the nexus-features values into a single request
      result.add(editConfigurationFileExtend("etc/nexus.properties", //
          "nexus-features", Joiner.on(',').join(nexusFeatures)));
    }

    return result.toArray(new Option[result.size()]);
  }

  // -------------------------------------------------------------------------

  @Before
  public void startTestRecording() {
    // Pax-Exam guarantees unique test location, use that with index
    testIndex.setDirectory(applicationDirectories.getAppDirectory());
  }

  @After
  public void stopTestRecording() {
    testIndex.recordAndCopyLink("karaf.log", resolveAppFile("data/log/karaf.log"));
    testIndex.recordAndCopyLink("nexus.log", resolveWorkFile("logs/nexus.log"));
    testIndex.recordAndCopyLink("request.log", resolveWorkFile("logs/request.log"));

    final String surefirePrefix = "target/surefire-reports/" + getClass().getName();
    testIndex.recordLink("surefire result", resolveBaseFile(surefirePrefix + ".txt"));
    testIndex.recordLink("surefire output", resolveBaseFile(surefirePrefix + "-output.txt"));

    final String failsafePrefix = "target/failsafe-reports/" + getClass().getName();
    testIndex.recordLink("failsafe result", resolveBaseFile(failsafePrefix + ".txt"));
    testIndex.recordLink("failsafe output", resolveBaseFile(failsafePrefix + "-output.txt"));
  }

  // -------------------------------------------------------------------------

  /**
   * @return Pax-Exam option to install custom invoker factory that waits for Nexus to start
   */
  private static Option nexusPaxExam() {
    final String version = MavenUtils.getArtifactVersion("org.sonatype.nexus", "nexus-pax-exam");
    Option result = mavenBundle("org.sonatype.nexus", "nexus-pax-exam", version);

    final File nexusPaxExam = resolveBaseFile("target/nexus-pax-exam-" + version + ".jar");
    if (nexusPaxExam.isFile()) {
      // when freshly built bundle of 'nexus-pax-exam' is available, copy it over to distribution's system repository
      final String systemPath = "system/org/sonatype/nexus/nexus-pax-exam/" + version + "/" + nexusPaxExam.getName();
      result = composite(replaceConfigurationFile(systemPath, nexusPaxExam), result);
    }

    return result;
  }

  /**
   * @return Timeout to apply when waiting for Nexus to start
   */
  private static int getExamTimeout() {
    try {
      return Integer.parseInt(System.getProperty(NEXUS_PAX_EXAM_TIMEOUT_KEY));
    }
    catch (final Exception e) {
      return NEXUS_PAX_EXAM_TIMEOUT_DEFAULT;
    }
  }
}
