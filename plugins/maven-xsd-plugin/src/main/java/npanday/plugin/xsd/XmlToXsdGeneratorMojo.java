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
package npanday.plugin.xsd;

import npanday.PlatformUnsupportedException;
import npanday.executable.ExecutableRequirement;
import npanday.executable.ExecutionException;
import npanday.registry.RepositoryRegistry;
import npanday.vendor.SettingsUtil;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates XSD from XML instance
 *
 * @author Shane Isbell
 * @goal xml
 * @phase process-sources
 * @description Generates XSD from XML instance
 */
public class XmlToXsdGeneratorMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${npanday.settings}" default-value="${user.home}/.m2"
     */
    private String settingsPath;

    /**
     * The directory to place the generated binding classes.
     *
     * @parameter expression="${outputDirectory}" default-value = "${project.build.directory}${file.separator}generated-resources"
     * @required
     */
    private String outputDirectory;

    /**
     * @parameter expression = "${xmlFiles}"
     * @required
     */
    private ArrayList<String> xmlFiles;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * The home directory of your .NET SDK.
     *
     * @parameter expression="${netHome}"
     */
    private File netHome;

    /**
     * The Vendor for the executable. Supports MONO and MICROSOFT: the default value is <code>MICROSOFT</code>. Not
     * case or white-space sensitive.
     *
     * @parameter expression="${vendor}"
     */
    private String vendor;

    /**
     * @parameter expression = "${frameworkVersion}"  default-value = "2.0.50727"
     */
    private String frameworkVersion;

    /**
     * The profile that the executable should use.
     *
     * @parameter expression = "${profile}" default-value = "XSD:XML_INSTANCE"
     */
    private String profile;

    /**
     * @component
     */
    private npanday.executable.NetExecutableFactory netExecutableFactory;

    /**
     * @component
     */
    private RepositoryRegistry repositoryRegistry;

    public void execute()
        throws MojoExecutionException
    {
        SettingsUtil.applyCustomSettings( getLog(), repositoryRegistry, settingsPath );

        FileUtils.mkdir( outputDirectory );
        try
        {
            netExecutableFactory.getExecutable(
                new ExecutableRequirement( vendor, null, frameworkVersion, profile ), getCommands(), netHome
            ).execute();
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException( "NPANDAY-1401-000: Unable to execute xsd: Vendor " + vendor +
                ", frameworkVersion = " + frameworkVersion + ", Profile = " + profile, e );
        }
        catch ( PlatformUnsupportedException e )
        {
            throw new MojoExecutionException( "NPANDAY-1401-001: Platform Unsupported: Vendor " + vendor +
                ", frameworkVersion = " + frameworkVersion + ", Profile = " + profile, e );
        }
    }

    public List<String> getCommands()
        throws MojoExecutionException
    {
        List<String> commands = new ArrayList<String>();
        for ( String xmlFile : xmlFiles )
        {
            if ( !( new File( xmlFile ) ).exists() )
            {
                throw new MojoExecutionException( "NPANDAY-1401-002: XML File does not exist: FileName = " + xmlFile );
            }
            commands.add( xmlFile );
        }
        commands.add( "/o:" + outputDirectory );
        return commands;
    }
}
