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

import npanday.NPandayContext;
import npanday.PlatformUnsupportedException;
import npanday.executable.ExecutionException;
import npanday.executable.compiler.CompilerContext;
import npanday.executable.compiler.CompilerExecutable;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * A compiler to be used for compiling with .NET Profiles.
 *
 * @author Shane Isbell
 */
public final class CSharpCompilerForProfile
    extends BaseCompiler
{
    private DefaultCompiler netCompiler;
    private CompilerContext compilerContext;

    public CSharpCompilerForProfile()
    {
        netCompiler = new DefaultCompiler();
    }

    public void init( NPandayContext npandayContext, Properties properties )
    {
        super.init( npandayContext, properties );
        netCompiler.init( npandayContext, properties );
        this.compilerContext = (CompilerContext) npandayContext;
    }

    public boolean shouldCompile()
    {
        return netCompiler.shouldCompile();
    }

    public boolean failOnErrorOutput()
    {
        return netCompiler.failOnErrorOutput();
    }

    public List<String> getCommands() throws ExecutionException, PlatformUnsupportedException
    {
        File assemblyPath = compilerContext.getAssemblyPath();
        if ( assemblyPath == null )
        {
            throw new ExecutionException(
                "NPANDAY-067-003: The assembly path is not specified" );
        }

        if ( !assemblyPath.exists() )
        {
            throw new ExecutionException(
                "NPANDAY-067-002: The assembly path does not exist: Path = " + assemblyPath.getAbsolutePath() );
        }

        List<String> commands = netCompiler.getCommands();
        commands.add( "/nostdlib+" );
        commands.add( "/noconfig" );
        for ( String coreAssembly : compilerContext.getCoreAssemblyNames() )
        {
            commands.add( "/reference:" + assemblyPath.getAbsolutePath() + File.separator + coreAssembly + ".dll" );
        }
        return commands;
    }

}
