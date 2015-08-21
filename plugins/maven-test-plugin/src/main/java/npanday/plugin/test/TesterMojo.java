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

package npanday.plugin.test;

import npanday.ArtifactTypeHelper;
import npanday.LocalRepositoryUtil;
import npanday.PathUtil;
import npanday.PlatformUnsupportedException;
import npanday.executable.CommandExecutor;
import npanday.executable.ExecutableRequirement;
import npanday.executable.ExecutionException;
import npanday.executable.NetExecutable;
import npanday.executable.NetExecutableFactory;
import npanday.registry.RepositoryRegistry;
import npanday.resolver.NPandayDependencyResolution;
import npanday.resolver.filter.DotnetAssemblyArtifactFilter;
import npanday.resolver.filter.DotnetSymbolsArtifactFilter;
import npanday.resolver.filter.OrArtifactFilter;
import npanday.vendor.SettingsUtil;
import npanday.vendor.StateMachineProcessor;
import npanday.vendor.Vendor;
import npanday.vendor.VendorInfo;
import npanday.vendor.VendorRequirement;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.InversionArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Runs NUnit tests
 *
 * @author Shane Isbell
 * @goal test
 * @phase test
 * @description Runs NUnit tests
 */
public class TesterMojo
    extends AbstractTestrelatedMojo
{
    /**
     * The home of nunit. Use this if you 1) have not added nunit to your path and you only have MS installed; or 2)
     * have mono installed and want to use another version of nunit.
     * 
     * @parameter
     */
    private String nunitHome;

    /**
     * Display XML to the console
     * 
     * @parameter expression = "${xmlConsole}" default-value = "false"
     */
    private boolean xmlConsole;

    /**
     * On 64bit-systems, the tests will automatically perform in .NET 64bit-version. If you need to
     * run your tests using 32bit (native calls, i.e.) set this option to true.
     * @parameter default-value = "false"
     */
    private boolean forceX86;

    /**
     * Skips unit test
     * 
     * @parameter expression = "${skipTests}" default-value = "false"
     */
    private boolean skipTest;

    /**
     * Directory where reports are written.
     * 
     * @parameter expression = "${reportsDirectory}" default-value = "${project.build.directory}/nunit-reports"
     */
    private String reportsDirectory;

    /**
     * nUnitXmlFilePath
     * 
     * @parameter default-value = "${project.build.directory}/nunit-reports/TEST-${project.build.finalName}.xml"
     * @required
     */
    private File nUnitXmlFilePath;

    /**
     * nUnitResultOutputPath
     * 
     * @parameter default-value = "${project.build.directory}/nunit-reports/TEST-${project.build.finalName}-RESULTS.txt"
     * @required
     */
    private File nUnitResultOutputPath;

    /**
     * nUnitResultErrorOutputPath
     * 
     * @parameter default-value = "${project.build.directory}/nunit-reports/TEST-${project.build.finalName}-ERROR.txt"
     * @required
     */
    private File nUnitResultErrorOutputPath;

    /**
     * @component
     */
    private StateMachineProcessor processor;

    /**
     * Specify the name of the NUnit command to be run, from within the <i>nunitHome</i>/bin directory.
     *  
     * @parameter
     * @deprecated since 1.5.0-incubating
     */
    private String nunitCommand;

    /**
     * The framework version to run the tests: 1.1, 2.0, 3.5, 4.0. Note that using this setting will require you to use
     * NUnit >= 2.5.
     *
     * @parameter expression = "${executionFrameworkVersion}"
     */
    private String executionFrameworkVersion;

    /**
     * @component
     */
    private NPandayDependencyResolution dependencyResolution;

    /**
     * Specifies if debug symbols for all dependencies should be resolved and copied to the test directory.
     *
     * @parameter expression = "${test.resolvePdbs}" default-value="true"
     */
    private Boolean resolvePdbs;


    private File getExecutableHome() 
    {
        return (nunitHome != null) ? new File(nunitHome, "bin") : null;
    }

    private List<String> getCommandsFor( )
    {
        String finalName = project.getBuild().getFinalName();
        List<String> commands = new ArrayList<String>();
        if ( testAssemblyPath.startsWith( "/" ) ) // nunit-console thinks *nix file format /home/user/ is an option
                                                  // due to / and fails.
        {
            testAssemblyPath = "/" + testAssemblyPath;
        }

        commands.add( testAssemblyPath + File.separator + getTestFileName() );

        String switchChar = "-";
        commands.add( switchChar + "xml:" + nUnitXmlFilePath.getAbsolutePath() );

        commands.add( switchChar + "output:" + nUnitResultOutputPath.getAbsolutePath() );
        commands.add( switchChar + "err:" + nUnitResultErrorOutputPath.getAbsolutePath() );

        commands.add( switchChar + "labels" );

        if ( xmlConsole )
        {
            commands.add( switchChar + "xmlConsole" );
        }
        
        // Not supported on NUnit < 2.5 - see NPANDAY-332
        if ( executionFrameworkVersion != null && executionFrameworkVersion.length() > 0 )
        {
            getLog().debug( "NPANDAY-1100-012: Framework version:" + executionFrameworkVersion );
            commands.add( switchChar + "framework:" + "v" + executionFrameworkVersion );
        }

        return commands;
    }

    @Override
    protected void innerExecute() throws MojoExecutionException, MojoFailureException
    {
        String skipTests = System.getProperty( "maven.test.skip" );
        if ( ( skipTests != null && skipTests.equalsIgnoreCase( "true" ) ) || skipTest )
        {
            getLog().warn( "NPANDAY-1100-000: Unit tests have been disabled." );
            return;
        }

        super.innerExecute();

        String testFileName = "";
        String pdbTestFileName = "";

        if(integrationTest)
        {
            getLog().info("NPANDAY-1100-000.1: Artifact is an Integration Test");
            testFileName = project.getBuild().getDirectory() + File.separator + project.getArtifactId() + ".dll";
            pdbTestFileName = project.getBuild().getDirectory() + File.separator + project.getArtifactId() + ".pdb";
        }
        else
        {
            testFileName = project.getBuild().getDirectory() + File.separator + project.getArtifactId() + "-test.dll";
            pdbTestFileName = project.getBuild().getDirectory() + File.separator + project.getArtifactId() + "-test.pdb";
        }

        if ( !( new File( testFileName ).exists() ) )
        {
            getLog().info( "NPANDAY-1100-001: No Unit Tests" );
            return;
        }

        Set<Artifact> artifacts;
        try
        {
            AndArtifactFilter filter = new AndArtifactFilter();
            filter.add(new ScopeArtifactFilter("test"));

            if (!resolvePdbs){
              filter.add(new InversionArtifactFilter(new DotnetSymbolsArtifactFilter()));
            }

            artifacts = dependencyResolution.require(
                project, LocalRepositoryUtil.create( localRepository ), filter
            );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException(
                "NPANDAY-1100-009: dependency resolution for scope test failed!", e
            );
        }

        for ( Artifact artifact : artifacts )
        {
            if ( ArtifactTypeHelper.isDotnetAnyGac( artifact.getType() ) )
            {
                continue;
            }

            try
            {
                PathUtil.copyPlainArtifactFileToDirectory( artifact, new File( testAssemblyPath ) );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "NPANDAY-1100-002: Error on copying artifact " + artifact, e );
            }
        }

        try
        {
            if ( project.getArtifact() != null && project.getArtifact().getFile() != null
                && project.getArtifact().getFile().exists() )
            {
                FileUtils.copyFileToDirectory( project.getArtifact().getFile(), new File( testAssemblyPath ) );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "NPANDAY-1100-004: Unable to copy library to target directory: ", e );
        }

        try
        {
            FileUtils.copyFileToDirectory( new File( testFileName ), new File( testAssemblyPath ) );
            File pdbTestFile = new File( pdbTestFileName );
            if ( pdbTestFile.exists() ) 
            {
                FileUtils.copyFileToDirectory( pdbTestFile, new File( testAssemblyPath ) );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "NPANDAY-1100-005: Unable to copy library to target directory: ", e );
        }

        FileUtils.mkdir( reportsDirectory );


        List<String> commands = getCommandsFor( );

        // pretty print nunit logs
        getLog().info( System.getProperty( "line.separator" ) );

        File executableHome = getExecutableHome();

        VendorRequirement vendorRequirement = new VendorRequirement( vendor, vendorVersion, executionFrameworkVersion );

        VendorInfo vendorInfo;
        try
        {
            vendorInfo = processor.process( vendorRequirement );
        }
        catch ( npanday.vendor.IllegalStateException e )
        {
            throw new MojoExecutionException(
                "NPANDAY-902-008: Illegal state of vendor info: Message =  " + e.getMessage(), e);
        }
        catch ( PlatformUnsupportedException e )
        {
            throw new MojoExecutionException(
                "NPANDAY-902-009: Platform is unsupported: Message =  " + e.getMessage(), e);
        }

        String profile = "NUNIT";
        if ( !vendorInfo.getVendor().equals( Vendor.MONO ) && forceX86 )
        {
            profile = "NUNIT-x86";
        }

        try
        {
            try
            {
                NetExecutable executable = netExecutableFactory.getExecutable(
                    new ExecutableRequirement( vendorRequirement, profile ), commands,
                    executableHome
                );

                executable.execute();
            }
            catch (PlatformUnsupportedException pue)
            {
                if (isNullOrEmpty(nunitCommand))
                {
                    throw new MojoExecutionException( "NPANDAY-1100-008: Unsupported Platform.", pue );
                }

                // TODO: This should rather be done through a configurable local executable-plugins.xml; then remove nunitcommand
                getLog().debug( "NPANDAY-1100-008: Platform unsupported, is your npanday-settings.xml configured correctly?", pue );        
                CommandExecutor commandExecutor = CommandExecutor.Factory.createDefaultCommmandExecutor(null);
                commandExecutor.setLogger( new org.codehaus.plexus.logging.AbstractLogger( 0, "nunit-logger" )
                {
                    Log log = getLog();

                    public void debug( String message, Throwable throwable )
                    {
                        log.debug( message, throwable );
                    }

                    public void error( String message, Throwable throwable )
                    {
                        log.error( message, throwable );
                    }

                    public void fatalError( String message, Throwable throwable )
                    {
                        log.error( message, throwable );
                    }

                    public Logger getChildLogger( String message )
                    {
                        return null;
                    }

                    public void info( String message, Throwable throwable )
                    {
                        log.info( message, throwable );
                    }

                    public void warn( String message, Throwable throwable )
                    {
                        log.warn( message, throwable );
                    }
                } );

                String executablePath = (executableHome != null) ? new File(executableHome, nunitCommand).toString() : nunitCommand;
                commandExecutor.executeCommand( executablePath, commands );
            }
        }
        catch ( ExecutionException e )
        {
            String line = System.getProperty( "line.separator" );
            throw new MojoFailureException( "NPANDAY-1100-007: There are test failures." + line + line + e.getMessage(), e);
        }
    }
}
