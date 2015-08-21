package npanday.executable.impl;

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
import npanday.executable.CommandExecutor;
import npanday.executable.*;
import npanday.NPandayContext;
import npanday.vendor.Vendor;
import org.codehaus.plexus.logging.Logger;

import java.util.List;
import java.util.Collections;
import java.io.File;
import java.util.Properties;

/**
 * @author Shane Isbell
 */
public class DefaultRepositoryNetExecutable
    implements NetExecutable
{

    private RepositoryExecutableContext executableContext;

    /**
     * A logger for writing log messages
     */
    private Logger logger;

    private List<String> commands;

    private Properties configuration;

    public List<String> getCommands() throws ExecutionException
    {
        return commands;
    }

    public String getExecutable() throws ExecutionException
    {
        if ( executableContext == null )
        {
            logger.info( "NPANDAY-063-002: Executable has not been initialized with a context" );
            return null;
        }
        List<String> executables = executableContext.getExecutableConfig().getExecutionPaths();
        if ( executables != null )
        {
            for ( String executable : executables )
            {
                File exe = new File( executable );
                if ( exe.exists() )
                {
                    logger.info( "NPANDAY-068-005: Found executable: " + exe.getAbsolutePath() );
                    return exe.getAbsolutePath();
                }
            }
            if (executables.size() > 0)
            {
                logger.info( "NPANDAY-068-007: Assuming " + executables.get( 0 ) + " will be found on the path." );
                return executables.get( 0 );
            }
        }
        throw new ExecutionException( "NPANDAY-068-006: Couldn't find anything to be executed!" );
    }

    public ExecutionResult execute() throws ExecutionException
    {
        List<String> commands = getCommands();

        CommandExecutor commandExecutor = CommandExecutor.Factory.createDefaultCommmandExecutor(
           configuration
        );
        commandExecutor.setLogger( logger );

        try
        {
            commandExecutor.executeCommand( getExecutable(), getCommands(), null, true );
        }
        catch ( ExecutionException e )
        {
            throw new ExecutionException(
                "NPANDAY-063-000: Executable = " + getExecutable() + ", Args = " + commands, e
            );
        }

        // TODO: find out under what situation this was needed and remove hard coding - can catch false positives (see MSBuild plugin need for /v:q), better to rely on exit code
        if ( commandExecutor.getStandardOut().contains( "error" ) && !commandExecutor.getStandardOut().contains(
            "exit code = 0"
        ) )
        {
            throw new ExecutionException(
                "NPANDAY-063-001: Executable = " + getExecutable() + ", Args = " + commands
            );
        }

        return new ExecutionResult(
            commandExecutor.getResult(),
            commandExecutor.getStandardOut(),
            commandExecutor.getStandardError()
        );
    }

    public Vendor getVendor()
    {
        try
        {
            return executableContext.getNetExecutable().getVendor();
        }
        catch ( ExecutionException e )
        {
            return null;
        }
    }

    public void init( NPandayContext npandayContext, Properties properties )
    {
        configuration = properties;
        this.executableContext = (RepositoryExecutableContext) npandayContext;
        this.logger = executableContext.getLogger();
        commands = Collections.unmodifiableList( executableContext.getExecutableConfig().getCommands() );
    }
}
