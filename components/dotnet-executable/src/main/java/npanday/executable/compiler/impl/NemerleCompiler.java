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
import java.util.List;
import java.util.Set;


/**
 * Compiler for the Nemerle language (see http://nemerle.org/). Cannot use the DefaultCompiler for Nemerle
 * because the Nemerle compiler does not support the recurse command.
 *
 * @author Shane Isbell
 */
public final class NemerleCompiler
    extends BaseCompiler
{

    public boolean shouldCompile()
    {
        // TODO: figure out when nemerle compile can be skipped; or just remove this all together :)
        return true;
    }

    public boolean failOnErrorOutput()
    {
        return true;
    }

    public List<String> getCommands()
        throws ExecutionException
    {
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
            for ( Artifact artifact : modules )
            {
                String path = artifact.getFile().getAbsolutePath();
                sb.append( path ).append( ";" );
            }
            commands.add( "/addmodule:" + sb.toString() );
        }
        if ( !resources.isEmpty() )
        {
            for ( Artifact artifact : resources )
            {
                String path = artifact.getFile().getAbsolutePath();
                commands.add( "/reference:" + path );
            }
        }

        Set<File> sourceFiles = compilerContext.getSourceFiles();
        if( sourceFiles != null && !sourceFiles.isEmpty() )
        {
            for(File includeSource : sourceFiles )
            {
                // TODO: consider relative paths
                commands.add( includeSource.getAbsolutePath() );
            }
        }

        commands.addAll( compilerContext.getCommands() );
        return commands;
    }

}
