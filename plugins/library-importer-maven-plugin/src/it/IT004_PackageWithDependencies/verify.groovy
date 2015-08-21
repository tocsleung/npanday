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





def projectDir = new File( basedir, "target\\generated-projects\\Common.Logging.Log4Net_Common.Logging.Log4Net_dotnet-library_2.0.1" )
assert projectDir.exists()

assert new File( projectDir, "pom.xml" ).exists()
assert new File( projectDir, "Common.Logging.Log4Net.dll" ).exists()

def project = new XmlSlurper().parse(new File(projectDir, "pom.xml"))

assert project.dependencies.dependency.size() == 2

return true;
