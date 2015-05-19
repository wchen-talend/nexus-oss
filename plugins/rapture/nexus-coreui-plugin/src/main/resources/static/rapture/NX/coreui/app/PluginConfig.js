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
/*global Ext, NX*/

/**
 * CoreUi plugin configuration.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.app.PluginConfig', {
  '@aggregate_priority': 100,

  namespaces: [
    'NX.coreui'
  ],

  requires: [
    'NX.coreui.app.PluginStrings'
  ],

  controllers: [
    {
      id: 'NX.coreui.controller.AssetInfo',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.AnalyticsSettings',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-analytics-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.AnalyticsEvents',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-analytics-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.AnonymousSettings',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Capabilities',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-capabilities-plugin');
      }
    },
    'NX.coreui.controller.FeatureGroups',
    {
      id: 'NX.coreui.controller.Feeds',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.HttpSettings',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.GeneralSettings',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.HealthCheckSearch',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-healthcheck-oss-plugin')
            || NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-clm-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.LdapServers',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-ldap-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Log',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-logging-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Loggers',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-logging-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Metrics',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    { id: 'NX.coreui.controller.NuGetApiKey',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Outreach',
      active: function () {
        return NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-outreach-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Bundles',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Repositories',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Blobstores',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Privileges',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.RealmSettings',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Roles',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Search',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SearchMaven',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SearchNuget',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SearchRaw',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SmtpSettings',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SslCertificates',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-ssl-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SslTrustStore',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-ssl-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SupportRequest',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SupportZip',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-atlas-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.SysInfo',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-atlas-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Tasks',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    },
    {
      id: 'NX.coreui.controller.Users',
      active: function () {
        return NX.app.Application.bundleActive('org.sonatype.nexus.plugins.nexus-coreui-plugin');
      }
    }
  ]
});
