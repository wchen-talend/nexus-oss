package org.sonatype.nexus.plugins.migration.nxcm255;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.plugin.migration.artifactory.dto.MigrationSummaryDTO;
import org.sonatype.nexus.plugins.migration.AbstractMigrationIntegrationTest;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.test.utils.TestProperties;

public class NXCM255MapRepositoriesTest
    extends AbstractMigrationIntegrationTest
{

    @Test
    public void importMixedRepo()
        throws Exception
    {
        MigrationSummaryDTO migrationSummary = prepareMigration( getTestFile( "artifactoryBackup.zip" ) );
        commitMigration( migrationSummary );

        TaskScheduleUtil.waitForTasks( 40 );
        Thread.sleep( 2000 );

        File artifact = getTestFile( "artifact.jar" );
        URL url =
            new URL( "http://localhost:" + nexusApplicationPort
                + "/artifactory/main-local/nxcm255/released/1.0/released-1.0.jar" );
        File downloaded;
        try
        {
            downloaded = this.downloadFile( url, "target/downloads/nxcm255" );
        }
        catch ( IOException e )
        {
            Assert.fail( "Unable to download artifact " + url + " got:\n" + e.getMessage() );
            throw e; // never happen
        }

        Assert.assertTrue( "Downloaded artifact was not right, checksum comparation fail " + url,
                           FileTestingUtils.compareFileSHA1s( artifact, downloaded ) );

        File mavenProject = getTestFile( "maven-project" );

        Verifier verifier = createVerifier( mavenProject, null );
        verifier.executeGoal( "dependency:resolve" );
        verifier.verifyErrorFreeLog();

    }

    /**
     * Create a nexus verifier instance
     *
     * @param mavenProject Maven Project folder
     * @param settings A settings.xml file
     * @return
     * @throws VerificationException
     * @throws IOException
     */
    public Verifier createVerifier( File mavenProject, File settings )
        throws VerificationException, IOException
    {
        Verifier verifier = new Verifier( mavenProject.getAbsolutePath(), false );

        System.setProperty( "maven.home", TestProperties.getString( "maven.instance" ) );

        File mavenRepository = new File( TestProperties.getString( "maven.local.repo" ) );
        verifier.setLocalRepo( mavenRepository.getAbsolutePath() );
        cleanRepository( mavenRepository );

        verifier.resetStreams();

        List<String> options = new ArrayList<String>();
        options.add( "-X" );
        options.add( "-U" );
        options.add( "-Dmaven.repo.local=" + mavenRepository.getAbsolutePath() );
        if ( settings != null )
        {
            options.add( "-s " + settings.getAbsolutePath() );
        }
        else
        {
            options.add( "-s " + this.getOverridableFile( "settings.xml" ) );
        }
        verifier.setCliOptions( options );
        return verifier;
    }

    /**
     * Remove all artifacts on <code>testId</code> groupId
     *
     * @param verifier
     * @throws IOException
     */
    public void cleanRepository( File mavenRepo )
        throws IOException
    {

        File testGroupIdFolder = new File( mavenRepo, getTestId() );
        FileUtils.deleteDirectory( testGroupIdFolder );

    }
}
