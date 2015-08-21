package npanday.registry;

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

import java.util.Hashtable;
import java.io.IOException;

/**
 * Provides services for loading repositories into the registry.
 *
 * @author Shane Isbell
 */

public interface RepositoryLoader
{

    Repository loadRepository( String location, String repositoryClass, Hashtable initParams )
            throws IOException, NPandayRepositoryException;

    /**
     * Accessor for the name of the <code>RepositoryLoader</code>. Typically will be the class name.
     *
     * @return name of the repository loader
     */
    String getLoaderName();

    /**
     * @param repositoryRegistry
     */
    void setRepositoryRegistry( RepositoryRegistry repositoryRegistry );

}
