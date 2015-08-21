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
package npanday.plugin.resgen;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Resource;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import java.util.List;

import java.util.Iterator;
import java.io.File;
import java.io.IOException;


/**
 * Copies embedded resources to target/assembly-resources/resource directory.
 *
 * @todo replace with the standard Maven resources plugin, we only need to reconfigure the default output location to be target/assembly-resources/resource, and add some extra handling for exe.config files.
 *
 * @author Shane Isbell
 * @goal copy-resources
 * @phase process-resources
 */
public class ResourceCopierMojo
    extends AbstractMojo
{

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
		try 
		{
		
			copyAllResources(project.getResources());

            // TODO: this might need to be handled differently, otherwise they could be copied into the artifact
			copyAllResources(project.getTestResources());
		}
		catch (Exception ex) 
		{
			getLog().debug("Exception thrown while copying files. Reason:", ex);
			throw new MojoExecutionException( "NPANDAY-1500-005: Failed to copy config files.", ex );
		}
    }

	private void copyAllResources(List<Resource> resources) 
		throws MojoExecutionException
	{
        
        if ( resources.isEmpty() )
        {
            getLog().info( "NPANDAY-1500-000: No resources found" );
            return;
        }

        File defaultTargetDirectory = new File( project.getBuild().getDirectory(), "assembly-resources/resource" );
		
		getLog().debug("NPANDAY-1500-002: Target directory:" + defaultTargetDirectory);
		getLog().debug("NPANDAY-1500-003: Project:" + project);
        
        for ( Resource resource : resources )
        {
			String resourceTargetPath = resource.getTargetPath();
			File targetDirectory = (resourceTargetPath != null && resourceTargetPath.length() > 0? new File(project.getBuild().getDirectory() + "/" + resourceTargetPath)  : defaultTargetDirectory);
        
            File file = new File( resource.getDirectory() );
            if ( file.exists() )
            {
                copyResourceDirectory( file, targetDirectory, resource.getIncludes(), resource.getExcludes() );
            }
        }
        try
        {
            FileUtils.copyDirectory( new File( project.getBasedir(), "src/main/config" ),
                                     new File( project.getBuild().getDirectory() ), "*.exe.config", null );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "NPANDAY-1500-004: Failed to copy config file.", e );
        }
    }

    private void copyResourceDirectory( File sourceDirectory, File outputDirectory, List<String> includes,
                                        List<String> excludes )
        throws MojoExecutionException
    {
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir( sourceDirectory );
     
        if ( !includes.isEmpty() )
        {
            directoryScanner.setIncludes( includes.toArray( new String[includes.size()] ) );
        }
        if ( !excludes.isEmpty() )
        {
            directoryScanner.setExcludes( excludes.toArray( new String[excludes.size()] ) );
        }
        directoryScanner.addDefaultExcludes();
        directoryScanner.scan();
        String[] files = directoryScanner.getIncludedFiles();
        for ( String file : files )
        {
            File sourceFile = new File( sourceDirectory, file );
            File destinationFile = new File( outputDirectory, file );
            try
            {
                destinationFile.getParentFile().mkdirs();
                FileUtils.copyFile( sourceFile, destinationFile );
                getLog().debug( "NPANDAY-1500-001: Copied Resource File: Source File = " + sourceFile.getAbsolutePath() +
                    ", Destination File = " + destinationFile.getAbsolutePath() );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                    "NPANDAY-1500-002: Unable to copy resource file: Source File = " + sourceFile.getAbsolutePath(), e );
            }
        }
        getLog().info( "NPANDAY-1500-003: Copied resource directory: Number of Resources = " + files.length +
            ", Resource Directory = " + sourceDirectory + ", Destination Directory = " + outputDirectory + File
            .separator + "assembly-resources" );
    }
}
