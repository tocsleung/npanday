package npanday.executable;

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
import npanday.vendor.Vendor;

import java.util.List;
import java.util.Properties;

/**
 * Provides services for executing programs.
 *
 * @author Shane Isbell
 * @author <a href="mailto:lcorneliussen@apache.org">Lars Corneliussen</a>
 *
 * @see npanday.executable.compiler.CompilerExecutable
 */
public interface NetExecutable
{
    /**
     * Runs the executable.
     *
     * @throws npanday.executable.ExecutionException
     *         if the executable fails or writes to the standard error stream.
     */
    ExecutionResult execute() throws ExecutionException, PlatformUnsupportedException;

    /**
     * Initialize this executable.
     *
     * @param npandayContext
     * @param properties
     */
    void init( NPandayContext npandayContext, Properties properties );

    /**
     * Returns vendor framework used to run executable.
     *
     * @return vendor vendor framework used to run executable
     */
    @Deprecated
    Vendor getVendor();
}
