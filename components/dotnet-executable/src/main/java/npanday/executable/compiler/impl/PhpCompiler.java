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
import org.codehaus.plexus.util.FileUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Compiler for PHP (http://php4mono.sourceforge.net/)
 *
 * @author Shane Isbell
 */
public final class PhpCompiler
    extends BaseCompiler
{

    public boolean shouldCompile()
    {
        // TODO: figure out when php compile can be skipped; or just remove this all together :)
        return true;
    }

    public boolean failOnErrorOutput()
    {
        return true;
    }

    public List<String> getCommands()
        throws ExecutionException
    {
        throw new ExecutionException( "NPANDAY-162-001: Php support has been discontinued" );
    }

}
