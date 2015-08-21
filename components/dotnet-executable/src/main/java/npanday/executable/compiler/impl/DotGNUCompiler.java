package npanday.executable.compiler.impl;

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

import npanday.executable.ExecutionException;
import org.apache.maven.artifact.Artifact;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Compiler for DotGNU.
 *
 * @author Shane Isbell
 */
public final class DotGNUCompiler
    extends BaseCompiler
{
    public boolean shouldCompile()
    {
        return true;
    }

    public boolean failOnErrorOutput()
    {
        return true;
    }

    public List<String> getCommands()
        throws ExecutionException
    {
        if ( compilerContext == null )
        {
            throw new ExecutionException( "NPANDAY-069-000: Compiler has not been initialized with a context" );
        }
        List<Artifact> resources = compilerContext.getLibraryDependencies();
        List<Artifact> modules = compilerContext.getDirectModuleDependencies();

        String artifactFilePath = compilerContext.getArtifact().getAbsolutePath();
        String targetArtifactType = compilerContext.getTargetArtifactType().getTargetCompileType();

        List<String> commands = new ArrayList<String>();
        commands.add( "/out:" + artifactFilePath );
        commands.add( "/target:" + targetArtifactType );
        if ( !modules.isEmpty() )
        {
            StringBuffer sb = new StringBuffer();
            for ( Iterator i = modules.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();
                String path = artifact.getFile().getAbsolutePath();
                sb.append( path );
                if ( i.hasNext() )
                {
                    sb.append( ";" );
                }
            }
        }
        if ( !resources.isEmpty() )
        {
            for ( Artifact artifact : resources )
            {
                String path = artifact.getFile().getAbsolutePath();
                commands.add( "/reference:" + path );
            }
        }

        for ( File file : compilerContext.getEmbeddedResources() )
        {
            commands.add( "/resource:" + file.getAbsolutePath() );
        }
        if ( compilerContext.getCommands() != null )
        {
            commands.addAll( compilerContext.getCommands() );
        }

        Set<File> sourceFiles = compilerContext.getSourceFiles();
        if( sourceFiles != null && !sourceFiles.isEmpty() )
        {
            for(File includeSource : sourceFiles )
            {
                // TODO: consider relative paths
                commands.add(includeSource.getAbsolutePath());
            }
        }

        //TODO: Apply command filter
        return commands;
    }

}
