package npanday.registry.impl;

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

import npanday.registry.ModelInterpolator;
import npanday.registry.NPandayRepositoryException;
import npanday.registry.Repository;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Provides base functionality for configuration repositories that read
 * from both resources and files. It supports multiple sources, clear, reload
 * and content versioning.
 *
 * @author <a href="mailto:lcorneliussen@apache.org">Lars Corneliussen</a>
 * @param <T>
 */
public abstract class AbstractMultisourceRepository<T>
    extends AbstractLogEnabled
    implements Repository
{
    private Hashtable properties;

    /**
     * The list of sources loaded into the repository.
     */
    private List<URL> sources = new ArrayList<URL>();

    private int contentVersion = 0;

    public void load( URL sourceUrl )
        throws NPandayRepositoryException
    {
        loadAndMerge( sourceUrl );
        sources.add( sourceUrl );
    }

    public void clearAll()
        throws OperationNotSupportedException
    {
        sources.clear();
        clear();
    }

    private void loadAndMerge( URL sourceUrl )
        throws NPandayRepositoryException
    {
        T model;
        try
        {
            Reader reader = new InputStreamReader( sourceUrl.openStream() );
            model = loadFromReader( reader, properties );
        }
        catch ( IOException e )
        {
            throw new NPandayRepositoryException(
                "NPANDAY-111-000: An error occurred while reading " + sourceUrl + " into " + getClass().getSimpleName(),
                e );
        }
        catch ( org.codehaus.plexus.util.xml.pull.XmlPullParserException e )
        {
            throw new NPandayRepositoryException(
                "NPANDAY-111-001: Could not read " + sourceUrl + " into " + getClass().getSimpleName(), e );
        }

        try
        {
            interpolateModel( model );
        }
        catch ( InterpolationException e )
        {
            throw new NPandayRepositoryException(
                "NPANDAY-111-001: Error on interpolating model from " + sourceUrl, e );
        }

        normalizeInterpolatedModelNodes( model );

        mergeLoadedModel( model );
        incrementContentVersion();
    }

    protected void normalizeInterpolatedModelNodes( T model ){

    }

    /**
     * Filters in properties from windows registry, environment variables, and (hopefully soon)
     * the running mojos Maven project.
     *
     * @param model
     * @throws InterpolationException
     */
    private void interpolateModel( T model )
        throws InterpolationException
    {
       // TODO: somehow we have to get the MavenProject here in order to filter it in
       getInterpolator().interpolate( model, null );
    }

    /**
     * @return The (probably injected) interpolator.
     */
    protected abstract ModelInterpolator getInterpolator();

    protected abstract T loadFromReader( Reader reader, Hashtable properties )
        throws IOException, org.codehaus.plexus.util.xml.pull.XmlPullParserException;

    protected abstract void mergeLoadedModel( T model )
        throws NPandayRepositoryException;

    /**
     * The properties configured in the registry.
     */
    public void setProperties( Hashtable props )
    {
        properties = props;
    }

    /**
     * Reloads this repository based on all provided sources.
     */
    public void reloadAll()
        throws IOException, NPandayRepositoryException
    {
        clear();
        for ( URL source : sources )
        {
            // TODO: throw better exception
            loadAndMerge( source );
        }
    }

    /**
     * Remove all stored values in preparation for a reload.
     */
    protected abstract void clear();

    /**
     * @return The current version of the content. Will be increased, every time new content is loaded
     *         into the same instance. This is useful, if you built a cache upon the values provided by this repository.
     */
    public int getContentVersion()
    {
        return this.contentVersion;
    }

    protected void incrementContentVersion()
    {
        this.contentVersion++;
    }

    /**
     * @return The properties, the repository was initialized with.
     */
    public Hashtable getProperties()
    {
        return properties;
    }
}
