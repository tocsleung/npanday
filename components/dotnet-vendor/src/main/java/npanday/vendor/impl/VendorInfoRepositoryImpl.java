package npanday.vendor.impl;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import npanday.PlatformUnsupportedException;
import npanday.model.settings.Framework;
import npanday.registry.RepositoryRegistry;
import npanday.vendor.InvalidVersionFormatException;
import npanday.vendor.SettingsRepository;
import npanday.vendor.Vendor;
import npanday.vendor.VendorInfo;
import npanday.vendor.VendorInfoMatchPolicy;
import npanday.vendor.VendorInfoRepository;
import npanday.vendor.VendorRequirement;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Provides an implementation of <code>VendorInfoRepository</code>.
 *
 * @author Shane Isbell
 */
public class VendorInfoRepositoryImpl
    implements VendorInfoRepository, LogEnabled
{

    /**
     * A registry component of repository (config) files
     * @component
     */
    private RepositoryRegistry repositoryRegistry;

    /**
     * A logger for writing log messages
     */
    private Logger logger;

    /**
     * Cache
     */
    private List<VendorInfo> cachedVendorInfos;

    /**
     * The version the repository was at, when the cache was built up.
     */
    private int cachedVendorInfosContentVersion;

    /**
     * Constructor. This method is intended to be invoked by the plexus-container, not by the application developer.
     */
    public VendorInfoRepositoryImpl()
    {
    }

    /**
     * @see LogEnabled#enableLogging(org.codehaus.plexus.logging.Logger)
     */
    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    public void clearCache()
    {
        if ( cachedVendorInfos != null )
        {
            cachedVendorInfos.clear();
            cachedVendorInfos = null;
        }
    }

    public VendorInfo getSingleVendorInfoByRequirement( VendorRequirement vendorRequirement )
        throws PlatformUnsupportedException
    {
        List<VendorInfo> infos = getVendorInfosFor( vendorRequirement, false);
        if (infos.size() == 0) {
           throw new PlatformUnsupportedException( "NPANDAY-113-001: Could not find configuration for " + vendorRequirement );
        }

        if (infos.size() > 2) {
            // reloadAll default
            infos = getVendorInfosFor( vendorRequirement, true);
        }

        assert infos.size() == 1;

        return infos.get(0);
    }

    private List<VendorInfo> getVendorInfos()
    {
        ensureCache();

        return Collections.unmodifiableList( cachedVendorInfos );
    }

    private void ensureCache()
    {
        SettingsRepository settingsRepository = (SettingsRepository) repositoryRegistry.find( "npanday-settings" );

        if (settingsRepository.isEmpty()) {
            logger.warn( "NPANDAY-113-000: The settings repository does not contain any vendor information" );
        }

        if ( settingsRepository.getContentVersion() > cachedVendorInfosContentVersion )
        {
            clearCache();
        }

        if ( cachedVendorInfos != null && cachedVendorInfos.size() > 0 )
        {
            return;
        }

        cachedVendorInfosContentVersion = settingsRepository.getContentVersion();
        cachedVendorInfos = new ArrayList<VendorInfo>();

        for ( npanday.model.settings.Vendor v : settingsRepository.getVendors() )
        {
            List<Framework> frameworks = v.getFrameworks();
            for ( Framework framework : frameworks )
            {
                cachedVendorInfos.add( new SettingsBasedVendorInfo( v, framework ) );
            }
        }
    }

    /**
     * @see VendorInfoRepository#getMaxVersion(java.util.Set<String>)
     */
    public String getMaxVersion( Set<String> versions )
        throws InvalidVersionFormatException
    {
        return new VersionMatcher().getMaxVersion( versions );
    }

    /**
     * @see VendorInfoRepository#getVendorInfosFor(String, String, String, boolean)
     */
    public List<VendorInfo> getVendorInfosFor( String vendorName, String vendorVersion, String frameworkVersion,
                                               boolean defaultOnly )
    {
        List<VendorInfo> vendorInfos = new ArrayList<VendorInfo>();
        MatchPolicyFactory matchPolicyFactory = new MatchPolicyFactory();
        matchPolicyFactory.init( logger );

        List<VendorInfoMatchPolicy> matchPolicies = new ArrayList<VendorInfoMatchPolicy>();
        if ( vendorName != null )
        {
            matchPolicies.add( matchPolicyFactory.createVendorNamePolicy( vendorName ) );
        }
        if ( vendorVersion != null )
        {
            matchPolicies.add( matchPolicyFactory.createVendorVersionPolicy( vendorVersion ) );
        }
        if ( frameworkVersion != null )
        {
            matchPolicies.add( matchPolicyFactory.createFrameworkVersionPolicy( frameworkVersion ) );
        }
        if ( defaultOnly )
        {
            matchPolicies.add( matchPolicyFactory.createVendorIsDefaultPolicy() );
        }
        for ( VendorInfo vendorInfo : getVendorInfos() )
        {
            if ( matchVendorInfo( vendorInfo, matchPolicies ) )
            {
                vendorInfos.add( vendorInfo );
            }
        }
        return vendorInfos;
    }

    /**
     * @see VendorInfoRepository#getVendorInfosFor(npanday.vendor.VendorRequirement, boolean)
     */
    public List<VendorInfo> getVendorInfosFor( VendorRequirement vendorRequirement, boolean defaultOnly )
    {
        if ( vendorRequirement == null )
        {
            return getVendorInfos();
        }
        return getVendorInfosFor( ( vendorRequirement.getVendor() != null ? vendorRequirement.getVendor().getVendorName() : null ),
                                  vendorRequirement.getVendorVersion(), vendorRequirement.getFrameworkVersion(), defaultOnly );
    }

    /**
     * Returns true if the specified vendor info matches <i>all</i> of the specified match policies, otherwise returns
     * false.
     *
     * @param vendorInfo    the vendor info to match against the match policies
     * @param matchPolicies the match policies
     * @return true if the specified vendor info matches <i>all</i> of the specified match policies, otherwise returns
     *         false
     */
    private boolean matchVendorInfo( VendorInfo vendorInfo, List<VendorInfoMatchPolicy> matchPolicies )
    {
        for ( VendorInfoMatchPolicy matchPolicy : matchPolicies )
        {
            if ( !matchPolicy.match( vendorInfo ) )
            {
                return false;
            }
        }
        return true;
    }

    public File getGlobalAssemblyCacheDirectoryFor( Vendor vendor, String frameworkVersion, String artifactType )
        throws PlatformUnsupportedException
    {
        return getSingleVendorInfoByRequirement( new VendorRequirement( vendor, null, frameworkVersion ) )
            .getGlobalAssemblyCacheDirectoryFor(artifactType);
    }

    public boolean isEmpty()
    {
        return getVendorInfos().size() == 0;
    }

    public void setRepositoryRegistry( RepositoryRegistry repositoryRegistry )
    {
        this.repositoryRegistry = repositoryRegistry;
    }
}

