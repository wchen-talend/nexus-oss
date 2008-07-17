/*
 * Nexus: Maven Repository Manager
 * Copyright (C) 2008 Sonatype Inc.                                                                                                                          
 * 
 * This file is part of Nexus.                                                                                                                                  
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 */
package org.sonatype.nexus.configuration.validator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.nexus.configuration.model.CAuthSource;
import org.sonatype.nexus.configuration.model.CAuthzSource;
import org.sonatype.nexus.configuration.model.CGroupsSetting;
import org.sonatype.nexus.configuration.model.CGroupsSettingPathMappingItem;
import org.sonatype.nexus.configuration.model.CHttpProxySettings;
import org.sonatype.nexus.configuration.model.CRemoteAuthentication;
import org.sonatype.nexus.configuration.model.CRemoteConnectionSettings;
import org.sonatype.nexus.configuration.model.CRemoteHttpProxySettings;
import org.sonatype.nexus.configuration.model.CRemoteNexusInstance;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryGroup;
import org.sonatype.nexus.configuration.model.CRepositoryGrouping;
import org.sonatype.nexus.configuration.model.CRepositoryShadow;
import org.sonatype.nexus.configuration.model.CRepositoryTarget;
import org.sonatype.nexus.configuration.model.CRestApiSettings;
import org.sonatype.nexus.configuration.model.CRouting;
import org.sonatype.nexus.configuration.model.CSchedule;
import org.sonatype.nexus.configuration.model.CScheduledTask;
import org.sonatype.nexus.configuration.model.CSecurity;
import org.sonatype.nexus.configuration.model.Configuration;

/**
 * The default configuration validator provider. It checks the model for semantical validity.
 * 
 * @author cstamas
 * @plexus.component
 */
public class DefaultConfigurationValidator
    extends AbstractLogEnabled
    implements ConfigurationValidator
{
    @SuppressWarnings( "unchecked" )
    public ValidationResponse validateModel( ValidationRequest request )
    {
        ValidationResponse response = new ValidationResponse();

        Configuration model = request.getConfiguration();

        // check for security model
        if ( model.getSecurity() != null )
        {
            response.append( validateSecurity( response.getContext(), model.getSecurity() ) );
        }
        else
        {
            model.setSecurity( new CSecurity() );

            response
                .addValidationWarning( "Security configuration block, which is mandatory, was missing. Reset with defaults." );

            response.setModified( true );
        }

        // global conn settings
        if ( model.getGlobalConnectionSettings() != null )
        {
            response.append( validateRemoteConnectionSettings( response.getContext(), model
                .getGlobalConnectionSettings() ) );
        }
        else
        {
            model.setGlobalConnectionSettings( new CRemoteConnectionSettings() );

            response
                .addValidationWarning( "Global connection settings block, which is mandatory, was missing. Reset with defaults." );

            response.setModified( true );
        }

        // global httpproxy settings (optional)
        if ( model.getGlobalHttpProxySettings() != null )
        {
            response
                .append( validateRemoteHttpProxySettings( response.getContext(), model.getGlobalHttpProxySettings() ) );
        }

        // rest api
        if ( model.getRestApi() != null )
        {
            response.append( validateRestApiSettings( response.getContext(), model.getRestApi() ) );
        }
        else
        {
            model.setRestApi( new CRestApiSettings() );

            response.addValidationWarning( "The REST API section was missing from configuration, defaulted it." );

            response.setModified( true );
        }

        // nexus built-in http proxy
        if ( model.getHttpProxy() != null )
        {
            response.append( validateHttpProxySettings( response.getContext(), model.getHttpProxy() ) );
        }
        else
        {
            model.setHttpProxy( new CHttpProxySettings() );

            response.addValidationWarning( "The HTTP Proxy section was missing from configuration, defaulted it." );

            response.setModified( true );
        }

        // routing
        if ( model.getRouting() != null )
        {
            response.append( validateRouting( response.getContext(), model.getRouting() ) );
        }
        else
        {
            model.setRouting( new CRouting() );

            model.getRouting().setGroups( new CGroupsSetting() );

            response.addValidationWarning( "The routing section was missing from configuration, defaulted it." );

            response.setModified( true );
        }

        // check existing reposes and check their realms
        response.getContext().addExistingRepositoryIds();

        List<CRepository> reposes = model.getRepositories();

        for ( CRepository repo : reposes )
        {
            response.append( validateRepository( response.getContext(), repo ) );
        }

        // check shadow reposes and check their realms (optional section)
        if ( model.getRepositoryShadows() != null )
        {
            response.getContext().addExistingRepositoryShadowIds();

            List<CRepositoryShadow> shadows = model.getRepositoryShadows();

            for ( CRepositoryShadow shadow : shadows )
            {
                response.append( validateRepository( response.getContext(), shadow ) );
            }
        }

        // check groups (optional section)
        if ( model.getRepositoryGrouping() != null )
        {
            response.append( validateRepositoryGrouping( response.getContext(), model.getRepositoryGrouping() ) );
        }

        // check remote nexus instances (optional section)
        if ( model.getRemoteNexusInstances() != null )
        {
            List<CRemoteNexusInstance> instances = model.getRemoteNexusInstances();

            for ( CRemoteNexusInstance instance : instances )
            {
                response.append( validateRemoteNexusInstance( response.getContext(), instance ) );
            }
        }

        // check repo targets (optional section)
        if ( model.getRepositoryTargets() != null )
        {
            List<CRepositoryTarget> targets = model.getRepositoryTargets();

            for ( CRepositoryTarget target : targets )
            {
                response.append( validateRepositoryTarget( response.getContext(), target ) );
            }
        }

        // check tasks (optional section)
        if ( model.getTasks() != null )
        {
            List<CScheduledTask> tasks = model.getTasks();

            for ( CScheduledTask task : tasks )
            {
                response.append( validateScheduledTask( response.getContext(), task ) );
            }
        }

        // summary
        if ( response.getValidationErrors().size() > 0 || response.getValidationWarnings().size() > 0 )
        {
            getLogger().error( "* * * * * * * * * * * * * * * * * * * * * * * * * *" );

            getLogger().error( "Nexus configuration has validation errors/warnings" );

            getLogger().error( "* * * * * * * * * * * * * * * * * * * * * * * * * *" );

            if ( response.getValidationErrors().size() > 0 )
            {
                getLogger().error( "The ERRORS:" );

                for ( ValidationMessage msg : response.getValidationErrors() )
                {
                    getLogger().error( msg.toString() );
                }
            }

            if ( response.getValidationWarnings().size() > 0 )
            {
                getLogger().error( "The WARNINGS:" );

                for ( ValidationMessage msg : response.getValidationWarnings() )
                {
                    getLogger().error( msg.toString() );
                }
            }

            getLogger().error( "* * * * * * * * * * * * * * * * * * * * *" );
        }
        else
        {
            getLogger().info( "Nexus configuration validated succesfully." );
        }

        return response;
    }

    // ---------------
    // Public

    public ValidationResponse validateSecurity( ValidationContext ctx, CSecurity settings )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        // if security enabled, at least auth source must be defined
        if ( settings.isEnabled() )
        {
            if ( settings.getAuthenticationSource() == null )
            {
                settings.setAnonymousAccessEnabled( true );

                settings.setAuthenticationSource( new CAuthSource() );

                settings.getAuthenticationSource().setType( "simple" );

                response
                    .addValidationWarning( "Security is enabled, but no authenticationSource is set, setting 'simple' authentication source." );

                response.setModified( true );
            }
        }

        // collect existing realms, if any
        response.getContext().addExistingRealms();

        if ( settings.isEnabled() && settings.getRealms() != null )
        {
            List<CAuthzSource> realms = settings.getRealms();

            for ( CAuthzSource authz : realms )
            {
                response.getContext().getExistingRealms().add( authz.getId() );
            }
        }

        return response;
    }

    public ValidationResponse validateRepository( ValidationContext ctx, CRepository repo )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        if ( StringUtils.isEmpty( repo.getId() ) )
        {
            response.addValidationError( "Repository ID's may not be empty!" );
        }

        if ( StringUtils.isEmpty( repo.getName() ) )
        {
            repo.setName( repo.getId() );

            response.addValidationWarning( "Repository with ID='" + repo.getId()
                + "' has no name, defaulted to it's ID." );

            response.setModified( true );
        }

        if ( !validateLocalStatus( repo.getLocalStatus() ) )
        {
            response.addValidationError( "LocalStatus of repository with ID='" + repo.getId() + "' is wrong:'"
                + repo.getType() + "'! (Allowed values are: '" + Configuration.LOCAL_STATUS_IN_SERVICE + "' and '"
                + Configuration.LOCAL_STATUS_OUT_OF_SERVICE + "')" );
        }

        if ( !validateRepositoryType( repo.getType() ) )
        {
            response.addValidationError( "Type of repository with ID='" + repo.getId()
                + "' is wrong! (Allowed values are: '" + CRepository.TYPE_MAVEN2 + "', '" + CRepository.TYPE_MAVEN1
                + "', '" + CRepository.TYPE_MAVEN_SITE + "', '" + CRepository.TYPE_ECLIPSE_UPDATE_SITE + "')" );
        }

        if ( !CRepository.PROXY_MODE_ALLOW.equals( repo.getProxyMode() )
            && !CRepository.PROXY_MODE_BLOCKED_MANUAL.equals( repo.getProxyMode() )
            && !CRepository.PROXY_MODE_BLOCKED_AUTO.equals( repo.getProxyMode() ) )
        {
            response.addValidationError( "ProxyMode of repository with ID='" + repo.getId()
                + "' is wrong! (Allowed values are: " + CRepository.PROXY_MODE_ALLOW + ", "
                + CRepository.PROXY_MODE_BLOCKED_MANUAL + " and " + CRepository.PROXY_MODE_BLOCKED_AUTO + ")" );
        }

        if ( repo.getRepositoryPolicy() == null
            || ( !CRepository.REPOSITORY_POLICY_RELEASE.equals( repo.getRepositoryPolicy() ) && !CRepository.REPOSITORY_POLICY_SNAPSHOT
                .equals( repo.getRepositoryPolicy() ) ) )
        {
            response.addValidationError( "Repository " + repo.getId() + " have wrong repository policy: \""
                + repo.getRepositoryPolicy() + "\". Repository policy may be \""
                + CRepository.REPOSITORY_POLICY_RELEASE + "\" or \"" + CRepository.REPOSITORY_POLICY_SNAPSHOT
                + "\" only." );
        }

        if ( repo.getChecksumPolicy() == null
            || ( !CRepository.CHECKSUM_POLICY_IGNORE.equals( repo.getChecksumPolicy() )
                && !CRepository.CHECKSUM_POLICY_WARN.equals( repo.getChecksumPolicy() )
                && !CRepository.CHECKSUM_POLICY_STRICT.equals( repo.getChecksumPolicy() ) && !CRepository.CHECKSUM_POLICY_STRICT_IF_EXISTS
                .equals( repo.getChecksumPolicy() ) ) )
        {
            response.addValidationError( "Repository " + repo.getId() + " have wrong checksum policy: \""
                + repo.getChecksumPolicy() + "\". Repository checksum policy may be \""
                + CRepository.CHECKSUM_POLICY_IGNORE + "\", \"" + CRepository.CHECKSUM_POLICY_WARN + "\", \""
                + CRepository.CHECKSUM_POLICY_STRICT_IF_EXISTS + "\" or \"" + CRepository.CHECKSUM_POLICY_STRICT
                + "\" only." );
        }

        if ( response.getContext().getExistingRepositoryIds() != null )
        {
            if ( response.getContext().getExistingRepositoryIds().contains( repo.getId() ) )
            {
                response.addValidationError( "Repository " + repo.getId() + " declared more than once!" );
            }

            response.getContext().getExistingRepositoryIds().add( repo.getId() );
        }

        if ( response.getContext().getExistingRealms() != null )
        {
            if ( repo.getRealmId() != null )
            {
                if ( !ctx.getExistingRealms().contains( repo.getRealmId() ) )
                {
                    response.addValidationError( "The " + repo.getId()
                        + " repository points to a nonexistent security realm!" );
                }
            }
        }

        if ( repo.getLocalStorage() != null && repo.getLocalStorage().getUrl() != null )
        {
            try
            {
                new URL( repo.getLocalStorage().getUrl() );
            }
            catch ( MalformedURLException e )
            {
                response.addValidationError( "Repository " + repo.getId() + " has malformed local storage URL!", e );
            }
        }
        if ( repo.getRemoteStorage() != null && repo.getRemoteStorage().getUrl() != null )
        {
            try
            {
                new URL( repo.getRemoteStorage().getUrl() );
            }
            catch ( MalformedURLException e )
            {
                response.addValidationError( "Repository " + repo.getId() + " has malformed remote storage URL!", e );
            }
        }

        return response;
    }

    public ValidationResponse validateRepository( ValidationContext ctx, CRepositoryShadow shadow )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        if ( StringUtils.isEmpty( shadow.getId() ) )
        {
            response.addValidationError( "Repository shadow ID's may not be empty!" );
        }

        if ( StringUtils.isEmpty( shadow.getName() ) )
        {
            shadow.setName( shadow.getId() );

            response.addValidationWarning( "Repository shadow with ID='" + shadow.getId()
                + "' has no name, defaulted to it's ID." );

            response.setModified( true );
        }

        if ( response.getContext().getExistingRepositoryShadowIds() != null )
        {
            if ( response.getContext().getExistingRepositoryShadowIds().contains( shadow.getId() ) )
            {
                response.addValidationError( "Shadow repository " + shadow.getId() + " declared more than once!" );
            }

            response.getContext().getExistingRepositoryShadowIds().add( shadow.getId() );
        }

        if ( response.getContext().getExistingRepositoryIds() != null )
        {
            if ( !response.getContext().getExistingRepositoryIds().contains( shadow.getShadowOf() ) )
            {
                response.addValidationError( "The shadow with ID='" + shadow.getId() + "' of repository "
                    + shadow.getShadowOf() + " of type " + shadow.getType() + " points to a nonexistent repository!" );
            }
        }

        if ( response.getContext().getExistingRealms() != null )
        {
            if ( shadow.getRealmId() != null )
            {
                if ( !response.getContext().getExistingRealms().contains( shadow.getRealmId() ) )
                {
                    response.addValidationError( "The shadow with ID='" + shadow.getId() + "' of repository "
                        + shadow.getShadowOf() + " of type " + shadow.getType()
                        + " points to a nonexistent security realm!" );
                }
            }
        }

        if ( !validateLocalStatus( shadow.getLocalStatus() ) )
        {
            response.addValidationError( "LocalStatus of repository with ID='" + shadow.getId()
                + "' is wrong! (Allowed values are: " + Configuration.LOCAL_STATUS_IN_SERVICE + " and "
                + Configuration.LOCAL_STATUS_OUT_OF_SERVICE + ")" );
        }

        if ( !validateShadowRepositoryType( shadow.getType() ) )
        {
            response.addValidationError( "Type of repository shadow with ID='" + shadow.getId() + "' is wrong: '"
                + shadow.getType() + "'! (Allowed values are: '" + CRepositoryShadow.TYPE_MAVEN2 + "', '"
                + CRepositoryShadow.TYPE_MAVEN1 + "', '" + CRepositoryShadow.TYPE_MAVEN2_CONSTRAINED + "')" );
        }

        return response;
    }

    public ValidationResponse validateRepositoryGrouping( ValidationContext ctx, CRepositoryGrouping settings )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        response.getContext().addExistingRepositoryGroupIds();

        if ( settings.getRepositoryGroups() != null )
        {
            for ( CRepositoryGroup group : (List<CRepositoryGroup>) settings.getRepositoryGroups() )
            {
                response.append( validateRepositoryGroup( response.getContext(), group ) );
            }
        }

        response.getContext().addExistingPathMappingIds();

        if ( settings.getPathMappings() != null )
        {
            for ( CGroupsSettingPathMappingItem item : (List<CGroupsSettingPathMappingItem>) settings.getPathMappings() )
            {
                response.append( validateGroupsSettingPathMappingItem( response.getContext(), item ) );
            }
        }

        return response;
    }

    public ValidationResponse validateGroupsSettingPathMappingItem( ValidationContext ctx,
        CGroupsSettingPathMappingItem item )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        Random rnd = new Random();

        if ( StringUtils.isEmpty( item.getId() )
            || "0".equals( item.getId() )
            || ( response.getContext().getExistingPathMappingIds() != null && response
                .getContext().getExistingPathMappingIds().contains( item.getId() ) ) )
        {
            String newId = Long.toHexString( System.currentTimeMillis() + rnd.nextInt( 2008 ) );

            item.setId( newId );

            response.addValidationWarning( "Fixed wrong route ID from '" + item.getId() + "' to '" + newId + "'" );

            response.setModified( true );
        }

        if ( !isValidRegexp( item.getRoutePattern() ) )
        {
            response.addValidationError( "The regexp in Route with ID='" + item.getId() + "' is not valid: "
                + item.getRoutePattern() );
        }

        if ( response.getContext().getExistingPathMappingIds() != null )
        {
            response.getContext().getExistingPathMappingIds().add( item.getId() );
        }

        if ( !item.getRouteType().equals( CGroupsSettingPathMappingItem.INCLUSION_RULE_TYPE )
            && !item.getRouteType().equals( CGroupsSettingPathMappingItem.EXCLUSION_RULE_TYPE )
            && !item.getRouteType().equals( CGroupsSettingPathMappingItem.BLOCKING_RULE_TYPE ) )
        {
            response.addValidationError( "The groupMapping pattern with ID=" + item.getId()
                + " have invalid routeType='" + item.getRouteType() + "'. Valid route types are '"
                + CGroupsSettingPathMappingItem.INCLUSION_RULE_TYPE + "', '"
                + CGroupsSettingPathMappingItem.EXCLUSION_RULE_TYPE + "' and '"
                + CGroupsSettingPathMappingItem.BLOCKING_RULE_TYPE + "'." );
        }

        if ( response.getContext().getExistingRepositoryIds() != null
            && response.getContext().getExistingRepositoryShadowIds() != null )
        {
            List<String> existingReposes = response.getContext().getExistingRepositoryIds();

            List<String> existingShadows = response.getContext().getExistingRepositoryShadowIds();

            for ( String repoId : (List<String>) item.getRepositories() )
            {
                if ( !existingReposes.contains( repoId ) && !existingShadows.contains( repoId ) )
                {
                    response.addValidationError( "The groupMapping pattern with ID=" + item.getId()
                        + " refers to a nonexistent repository with repoID = " + repoId );
                }
            }
        }

        return response;
    }

    public ValidationResponse validateRepositoryGroup( ValidationContext ctx, CRepositoryGroup group )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        if ( StringUtils.isEmpty( group.getGroupId() ) )
        {
            response.addValidationError( "RepositoryGroup ID's may not be empty!" );
        }

        if ( StringUtils.isEmpty( group.getName() ) )
        {
            group.setName( group.getGroupId() );

            response.addValidationWarning( "RepositoryGroup with ID='" + group.getGroupId()
                + "' has no name, defaulted to it's ID." );

            response.setModified( true );
        }

        if ( response.getContext().getExistingRepositoryGroupIds() != null )
        {
            if ( response.getContext().getExistingRepositoryGroupIds().contains( group.getGroupId() ) )
            {
                response.addValidationError( "The group with GroupID " + group.getGroupId()
                    + " is defined more than once!" );
            }
        }

        if ( response.getContext().getExistingRepositoryIds() != null
            && response.getContext().getExistingRepositoryShadowIds() != null )
        {
            List<String> existingReposes = response.getContext().getExistingRepositoryIds();

            List<String> existingShadows = response.getContext().getExistingRepositoryShadowIds();

            List<String> members = group.getRepositories();

            for ( String repoId : members )
            {
                if ( !existingReposes.contains( repoId ) && !existingShadows.contains( repoId ) )
                {
                    response.addValidationError( "The group with GroupID " + group.getGroupId()
                        + " refers to a nonexistent repository with ID = " + repoId );
                }
            }
        }

        return response;
    }

    public ValidationResponse validateHttpProxySettings( ValidationContext ctx, CHttpProxySettings settings )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        if ( settings.getPort() < 80 )
        {
            settings.setPort( 8082 );

            response.addValidationWarning( "The HTTP Proxy port is below 80? Settings defaulted." );

            response.setModified( true );
        }

        if ( !CHttpProxySettings.PROXY_POLICY_PASS_THRU.equals( settings.getProxyPolicy() )
            && !CHttpProxySettings.PROXY_POLICY_STRICT.equals( settings.getProxyPolicy() ) )
        {
            response.addValidationError( "The HTTP Proxy policy settings is invalid: '" + settings.getProxyPolicy()
                + "'. Valid policies are '" + CHttpProxySettings.PROXY_POLICY_STRICT + "' and '"
                + CHttpProxySettings.PROXY_POLICY_PASS_THRU + "'." );
        }

        return response;
    }

    public ValidationResponse validateRemoteAuthentication( ValidationContext ctx, CRemoteAuthentication settings )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        return response;
    }

    public ValidationResponse validateRemoteConnectionSettings( ValidationContext ctx,
        CRemoteConnectionSettings settings )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        return response;
    }

    public ValidationResponse validateRemoteHttpProxySettings( ValidationContext ctx, CRemoteHttpProxySettings settings )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        return response;
    }

    public ValidationResponse validateRepositoryTarget( ValidationContext ctx, CRepositoryTarget settings )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        if ( StringUtils.isEmpty( settings.getId() ) )
        {
            response.addValidationError( "The RepositoryTarget may have no empty/null ID!" );
        }

        if ( StringUtils.isEmpty( settings.getName() ) )
        {
            settings.setName( settings.getId() );

            response.addValidationWarning( "Repository target with ID='" + settings.getId()
                + "' had no name, defaulted it to it's ID." );

            response.setModified( true );
        }

        if ( StringUtils.isEmpty( settings.getContentClass() ) )
        {
            response.addValidationError( "Repository target with ID='" + settings.getId()
                + "' has empty content class!" );
        }

        List<String> patterns = settings.getPatterns();

        for ( String pattern : patterns )
        {
            if ( !isValidRegexp( pattern ) )
            {
                response.addValidationError( "Repository target with ID='" + settings.getId()
                    + "' has invalid regexp pattern: " + pattern );
            }
        }

        return response;
    }

    public ValidationResponse validateRestApiSettings( ValidationContext ctx, CRestApiSettings settings )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        return response;
    }

    public ValidationResponse validateRouting( ValidationContext ctx, CRouting settings )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        return response;
    }

    public ValidationResponse validateRemoteNexusInstance( ValidationContext ctx, CRemoteNexusInstance settings )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        return response;
    }

    public ValidationResponse validateScheduledTask( ValidationContext ctx, CScheduledTask settings )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        response.append( validateSchedule( response.getContext(), settings.getSchedule() ) );

        return response;
    }

    public ValidationResponse validateSchedule( ValidationContext ctx, CSchedule settings )
    {
        ValidationResponse response = new ValidationResponse();

        if ( ctx != null )
        {
            response.setContext( ctx );
        }

        return response;
    }

    // --------------
    // Inner stuff

    protected boolean validateLocalStatus( String ls )
    {
        return Configuration.LOCAL_STATUS_IN_SERVICE.equals( ls )
            || Configuration.LOCAL_STATUS_OUT_OF_SERVICE.equals( ls );
    }

    protected boolean validateRepositoryType( String type )
    {
        return CRepository.TYPE_MAVEN2.equals( type ) || CRepository.TYPE_MAVEN1.equals( type )
            || CRepository.TYPE_MAVEN_SITE.equals( type ) || CRepository.TYPE_ECLIPSE_UPDATE_SITE.equals( type );
    }

    protected boolean validateShadowRepositoryType( String type )
    {
        return CRepositoryShadow.TYPE_MAVEN1.equals( type ) || CRepositoryShadow.TYPE_MAVEN2.equals( type )
            || CRepositoryShadow.TYPE_MAVEN2_CONSTRAINED.equals( type );
    }

    protected boolean isValidRegexp( String regexp )
    {
        try
        {
            Pattern.compile( regexp );

            return true;
        }
        catch ( PatternSyntaxException e )
        {
            return false;
        }
    }
}
