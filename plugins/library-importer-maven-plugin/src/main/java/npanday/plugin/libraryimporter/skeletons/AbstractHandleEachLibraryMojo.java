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

package npanday.plugin.libraryimporter.skeletons;

import npanday.plugin.libraryimporter.model.NugetPackage;
import npanday.plugin.libraryimporter.model.NugetPackageLibrary;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @author <a href="mailto:me@lcorneliussen.de>Lars Corneliussen, Faktum Software</a>
 */
public abstract class AbstractHandleEachLibraryMojo
    extends AbstractHandleEachImportMojo
{
    @Override
    protected void handleNugetPackage( NugetPackage nuget) throws MojoExecutionException, MojoFailureException
    {
        for ( NugetPackageLibrary lib : nuget.getLibraries( getLog(), mavenProjectsCacheDirectory ) )
        {
            getLog().debug( "NPANDAY-152-000: handling lib " + lib.toString() );
            try {
                handleLibrary( lib );
            }
            catch (MojoExecutionException e){
                throw new MojoExecutionException( "NPANDAY-152-001: error handling " + lib.toString(), e);
            }
            catch (MojoFailureException e){
                throw new MojoExecutionException( "NPANDAY-152-002: error handling " + lib.toString(), e);
            }
            catch (Exception e){
                throw new MojoExecutionException( "NPANDAY-152-003: error handling " + lib.toString(), e);
            }
        }
    }

    protected abstract void handleLibrary( NugetPackageLibrary lib ) throws MojoExecutionException, MojoFailureException;
}

