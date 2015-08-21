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
package npanday.plugin.ilmerge;

import npanday.ArtifactType;
import npanday.LocalRepositoryUtil;
import npanday.PlatformUnsupportedException;
import npanday.executable.ExecutableRequirement;
import npanday.executable.ExecutionException;
import npanday.executable.compiler.CompilerConfig;
import npanday.executable.compiler.CompilerExecutable;
import npanday.executable.compiler.CompilerRequirement;
import npanday.executable.compiler.KeyInfo;
import npanday.registry.RepositoryRegistry;
import npanday.resolver.NPandayDependencyResolution;
import npanday.vendor.SettingsUtil;
import npanday.vendor.Vendor;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * Merges assemblies into unified assembly.
 *
 * @phase package
 * @goal merge-assemblies
 */
public class AssemblyMerger extends AbstractMojo
{
    /**
     * @parameter expression="${npanday.settings}" default-value="${user.home}/.m2"
     */
    private String settingsPath;

    /**
     * @component
     */
    private RepositoryRegistry repositoryRegistry;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;
    
    /**
     * The Vendor for the executable. Supports MONO and MICROSOFT: the default value is <code>MICROSOFT</code>. Not
     * case or white-space sensitive.
     *
     * @parameter expression="${vendor}"
     */
    private String vendor;
    
    /**
     * @component
     */
    private npanday.executable.NetExecutableFactory netExecutableFactory;
    
    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * @parameter expression = "${frameworkVersion}"
     */
    private String frameworkVersion;
    
    /**
     * Specify a strong name key file.
     *
     * @parameter expression = "${keyfile}"
     */
    private File keyfile;

    /**
     * Specifies a strong name key container. (not currently supported)
     *
     * @parameter expression = "${keycontainer}"
     */
    private String keycontainer;


    /**
     * Copy assembly attributes into the merged assembly. Only the primary assembly attributes are copied.
     *
     * @parameter default-value = "false"
     */
    private boolean copyAttributes;

    /**
     * The profile that the compiler should use to compile classes: FULL, COMPACT, (or a custom one specified in a
     * compiler-plugins.xml).
     *
     * @parameter expression = "${profile}" default-value = "FULL"
     */
    private String profile;


    /**
     * .NET Language. The default value is <code>C_SHARP</code>. Not case or white-space sensitive.
     *
     * @parameter expression="${language}" default-value = "C_SHARP"
     * @required
     */
    private String language;

    /**
     * @parameter expression = "${vendorVersion}"
     */
    private String vendorVersion;

    /**
     * The home directory of your .NET SDK.
     *
     * @parameter expression="${netHome}"
     */
    private File netHome;
    
    /**
     * The ILMerge command name.  Supports either ILMerge (default) or ILRepack.
     *
     * @parameter expression = "${ilmergeCommand}"  default-value="ILMerge"
     */
    private String executable;
    
    /**
     * This overrides the defaultAssemblyPath for the compiler.
     *
     * @parameter expression = "${profileAssemblyPath}
     */
    private File profileAssemblyPath;

    /**
     * The directory for the compilated web application
     *
     * @parameter  expression = "${project.build.directory}/merged"
     */
    private File outputDirectory;

    /**
     * The location of the local Maven repository.
     *
     * @parameter expression="${settings.localRepository}"
     */
    private File localRepository;

    /**
     * @parameter 
     */
    private ArtifactSet artifactSet;

    /**
     * @parameter 
     */
    private ArtifactSet internalizeSet;

    /**
     * @parameter default-value="true"
     */
    private boolean mergeDebugSymbols;

    /**
     * @parameter default-value="true"
     */
    private boolean mergeDocumentation;

    /**
     * Defines whether the merged artifact should be attached as classifier to
     * the original artifact.  If false, the merged assembly will be the main artifact
     * of the project
     *
     * @parameter expression="${mergedArtifactAttached}" default-value="false"
     */
    private boolean mergedArtifactAttached;

    /**
     * The name of the classifier used in case the merged artifact is attached.
     *
     * @parameter expression="${mergedClassifierName}" default-value="merged"
     */
    private String mergedClassifierName;

    /**
     * If specified, this will include only artifacts which have groupIds which
     * start with this.
     *
     * @parameter expression="${mergedGroupFilter}"
     */
    private String mergedGroupFilter;

    /**
     *
     * @parameter default-value="true"
     */
    private boolean mergedArtifactReplacesProjectArtifact;

    /**
     * @component
     */
    private NPandayDependencyResolution dependencyResolution;

    /**
     * The scope up to which dependencies should be resolved.
     *
     * @parameter default-value="runtime"
     */
    private String requiredScope;

    /**
     * Merges the specified assemblies into a primary assembly with classifier "merged".
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        SettingsUtil.applyCustomSettings( getLog(), repositoryRegistry, settingsPath );

        resolveDependencies();

        try
        {
            // ilmerge.exe
            // determine how to set /lib:[assemblyPath]
            CompilerExecutable compilerExecutable = netExecutableFactory.getCompilerExecutable(
                getCompilerRequirement(), getCompilerConfig(), project
            );

            File assemblyPath = compilerExecutable.getAssemblyPath();
            if ( assemblyPath == null )
            {
                 throw new MojoExecutionException( "NPANDAY-1501-007: Unable to determine assembly path, perhaps missing profileAssemblyPath?" );
            }

            Set artifacts = new LinkedHashSet();

            ArtifactSelector artifactSelector =
                new ArtifactSelector( artifactSet, mergedGroupFilter );

            Artifact projectArtifact = project.getArtifact();

            if ( artifactSelector.isSelected( projectArtifact ) && !"pom".equals( projectArtifact.getType() ) )
            {

                if ( projectArtifact.getFile() == null )
                {
                    getLog().error( "The project main artifact does not exist. This could have the following" );
                    getLog().error( "reasons:" );
                    getLog().error( "- You have invoked the goal directly from the command line. This is not" );
                    getLog().error( "  supported. Please add the goal to the default lifecycle via an" );
                    getLog().error( "  <execution> element in your POM and use \"mvn package\" to have it run." );
                    getLog().error( "- You have bound the goal to a lifecycle phase before \"package\". Please" );
                    getLog().error( "  remove this binding from your POM such that the goal will be run in" );
                    getLog().error( "  the proper phase." );
                    throw new MojoExecutionException( "Failed to create shaded artifact, "
                        + "project main artifact does not exist." );
                }

                getLog().info( "Including " + projectArtifact.getId() + " in the merged assembly." );

                artifacts.add( projectArtifact.getFile() );
            }
            else
            {
                getLog().info( "Excluding " + projectArtifact.getId() + " from the merged assembly." );

            }

            ArtifactType packagingType = ArtifactType.getArtifactTypeForPackagingName(project.getPackaging());
            File mergedArtifactFile = new File(outputDirectory, project.getArtifactId() + "." + packagingType.getExtension());

            // TODO: /target defaults to same kind as first assembly
            Set candidateArtifacts = new HashSet();
            candidateArtifacts.addAll(project.getArtifacts());
            candidateArtifacts.addAll(project.getAttachedArtifacts());

            ArtifactSelector internalizeArtifactSelector = new ArtifactSelector( internalizeSet, null );

            Set internalizeArtifacts = new HashSet();

            for ( Iterator it = candidateArtifacts.iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();

                if ( !artifactSelector.isSelected( artifact ) )
                {
                    getLog().info( "Excluding " + artifact.getId() + " from the merged assembly." );

                    continue;
                }

                if ( "pom".equals( artifact.getType() ) )
                {
                    getLog().info( "Skipping pom dependency " + artifact.getId() + " in the merged assembly." );
                    continue;
                }

                boolean internalize = internalizeArtifactSelector.isSelected( artifact );

                getLog().info( "Including " + artifact.getId() + " in the merged assembly." + (internalize ? " (internalize)" : ""));

                artifacts.add( artifact.getFile() );

                if ( internalize )
                {
                    internalizeArtifacts.add( artifact.getFile() );
                }
            }

            // TODO: support multple non-internalized artifacts by executing ilmerge twice
            //         * first merge all the public assemblies together
            //         * then internalizing the rest using the previous result as the primary assembly
            if ( artifacts.removeAll( internalizeArtifacts ) )
            {
                if ( !internalizeArtifacts.isEmpty() && artifacts.size() > 1 )
                {
                    throw new MojoExecutionException( "NPANDAY-1501-011: Multiple non-internalized assemblies after applying internalizeSet filter to artifactSet" );
                }
            }

            // ILRepack on non-Windows appears to need a /lib: referring to the target directory
            // to avoid a problem during the merge process where it is unable to locate the primary assembly
            File artifactFile = (File) artifacts.iterator().next();
            Collection<String> searchDirectoryPaths = Arrays.asList( artifactFile.getParent() );

            // ILMerge cannot tolerate overwriting an input assembly with the output assembly
            File mergedArtifactTempDirectory = null;
            if ( artifacts.contains( mergedArtifactFile ) || internalizeArtifacts.contains( mergedArtifactFile ) )
            {
                mergedArtifactTempDirectory = new File( mergedArtifactFile.getParentFile(), "temp" );
                mergedArtifactTempDirectory.mkdirs();
            }

            List commands = new ArrayList();
            commands.add("/lib:" + assemblyPath.toString());

            for ( String searchDirectoryPath : searchDirectoryPaths )
            {
                commands.add("/lib:" + searchDirectoryPath);
            }

            if ( mergedArtifactTempDirectory != null )
            {
                File mergedArtifactTempFile = new File( mergedArtifactTempDirectory, mergedArtifactFile.getName() );
                commands.add("/out:" + mergedArtifactTempFile );
            }
            else
            {
                commands.add("/out:" + mergedArtifactFile);
            }

            if ( copyAttributes )
            {
                commands.add("/copyattrs");
            }

            if (!mergeDebugSymbols)
            {
                commands.add("/ndebug");
            }

            if (mergeDocumentation)
            {
                commands.add("/xmldocs");
            }

            for ( Iterator it = artifacts.iterator(); it.hasNext(); )
            {
                File artifact = (File)it.next();
                commands.add( artifact.getAbsolutePath() );
            }

            for ( Iterator it = internalizeArtifacts.iterator(); it.hasNext(); )
            {
                File internalizeArtifact = (File)it.next();
                commands.add( internalizeArtifact.getAbsolutePath() );
            }

            outputDirectory.mkdirs();
            netExecutableFactory.getExecutable(
                new ExecutableRequirement( vendor, null, frameworkVersion, executable ), commands, netHome
            ).execute();

            if ( mergedArtifactTempDirectory != null )
            {
                File mergedArtifactTempFile = new File( mergedArtifactTempDirectory, mergedArtifactFile.getName() );
                FileUtils.rename( mergedArtifactTempFile, mergedArtifactFile );

                if ( mergeDebugSymbols )
                {
                    Vendor vendor = compilerExecutable.getVendor();
                    String debugSymbolsExtension = ( vendor == Vendor.MONO ) ? ".mdb" : ".pdb";
                    String mergedArtifactSymbolFileName = mergedArtifactFile.getName().replace( ".dll", debugSymbolsExtension );
                    File mergedArtifactSymbolFile = new File( mergedArtifactFile.getParentFile(), mergedArtifactSymbolFileName );
                    if ( mergedArtifactSymbolFile.exists() )
                    {
                        File mergedArtifactTempSymbolFile = new File( mergedArtifactTempDirectory, mergedArtifactSymbolFileName );
                        FileUtils.rename( mergedArtifactTempSymbolFile, mergedArtifactSymbolFile );
                    }
                }

                FileUtils.deleteDirectory( mergedArtifactTempDirectory );
            }

            if ( mergedArtifactAttached )
            {
                getLog().info( "Attaching merged artifact." );
		projectHelper.attachArtifact(project, projectArtifact.getType(), mergedClassifierName, mergedArtifactFile);
            }
            else if ( mergedArtifactReplacesProjectArtifact )
            {
                getLog().info( "Replacing project artifact with merged artifact." );
                File projectArtifactFile = projectArtifact.getFile();
                FileUtils.rename( mergedArtifactFile, projectArtifactFile );
            }
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException( "NPANDAY-1501-002: Unable to execute " + executable + ": Vendor = " + vendor +
                ", frameworkVersion = " + frameworkVersion, e );
        }
        catch ( PlatformUnsupportedException e )
        {
            throw new MojoExecutionException( "NPANDAY-1501-003: Platform Unsupported", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "NPANDAY-1501-004: Unable to overwrite default artifact file", e );
        }
    }

    private void resolveDependencies() throws MojoExecutionException
    {
        try
        {
            dependencyResolution.require( project, LocalRepositoryUtil.create( localRepository ), requiredScope );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException(
                "NPANDAY-1501-012: Could not satisfy required dependencies of scope " + requiredScope, e
            );
        }
    }

    private String getFileNameMinusExtension(File file)
    {
        if (file==null) return null;
        
        String name = file.getName();
        int lastIndex = name.lastIndexOf( '.' );
                
        return name.substring( 0, lastIndex );
    }

    protected void initializeDefaults()  throws MojoExecutionException
    {


        if ( profileAssemblyPath != null && !profileAssemblyPath.exists() )
        {
            throw new MojoExecutionException( "NPANDAY-900-000: Profile Assembly Path does not exist: Path = " +
                profileAssemblyPath.getAbsolutePath() );
        }

    }

    protected CompilerRequirement getCompilerRequirement() throws MojoExecutionException
    {
        return new CompilerRequirement(vendor, vendorVersion, frameworkVersion, profile, language);
    }

    protected CompilerConfig getCompilerConfig()  throws MojoExecutionException
    {

          //Config
        CompilerConfig compilerConfig = new CompilerConfig();
        compilerConfig.setLocalRepository( localRepository );


        if ( keyfile != null )
        {
            KeyInfo keyInfo = KeyInfo.Factory.createDefaultKeyInfo();
            keyInfo.setKeyFileUri( keyfile.getAbsolutePath() );
            compilerConfig.setKeyInfo( keyInfo );
        }

        if(outputDirectory != null)
        {
            if(!outputDirectory.exists())
            {
                outputDirectory.mkdirs();
            }
            compilerConfig.setOutputDirectory(outputDirectory);
        }

        String artifactTypeName = project.getArtifact().getType();
        ArtifactType artifactType = ArtifactType.getArtifactTypeForPackagingName( artifactTypeName );
        if ( artifactType.equals( ArtifactType.NULL ) )
        {
            throw new MojoExecutionException( "NPANDAY-900-002: Unrecognized artifact type: Language = " + language +
                ", Vendor = " + vendor + ", ArtifactType = " + artifactTypeName );
        }
        compilerConfig.setArtifactType( artifactType );

        if (profileAssemblyPath != null){
            compilerConfig.setAssemblyPath( profileAssemblyPath );
        }

        return compilerConfig;
    }
}
