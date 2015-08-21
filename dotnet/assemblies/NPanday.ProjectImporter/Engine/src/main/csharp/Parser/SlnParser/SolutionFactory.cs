#region Apache License, Version 2.0
//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//
#endregion
using System;
using System.Collections.Generic;
using System.Text;
using NPanday.ProjectImporter.Parser.SlnParser.Model;
using System.IO;

/// Author: Leopoldo Lee Agdeppa III

namespace NPanday.ProjectImporter.Parser.SlnParser
{
    public static class SolutionFactory
    {
        public static Solution GetSolution(FileInfo solutionFile)
        {
            LexicalAnalizer lexan = new LexicalAnalizer(solutionFile);
            Solution solution = new Solution();
            solution.File = solutionFile;

            lexan.MoveNext();
			
			while(lexan.Current.Token == Semantics.EOL)
			{
			    // some sln files contains blank lines before the header values, so its best to recourse it first
	            lexan.Expect(Semantics.EOL);
	            lexan.MoveNext();
			}

            lexan.Expect(Semantics.STRING_VALUE);
            solution.Header = lexan.Current.Value;
            lexan.MoveNext();

            lexan.Expect(Semantics.COMMA);
            lexan.MoveNext();

            lexan.Expect(Semantics.STRING_VALUE);
            solution.FormatVersion = lexan.Current.Value;
            lexan.MoveNext();

            lexan.Expect(Semantics.EOL);
            lexan.MoveNext();

            lexan.Expect(Semantics.STRING_VALUE);
            solution.VsVersion = lexan.Current.Value;
            lexan.MoveNext();

            lexan.Expect(Semantics.EOL);


            while (lexan.MoveNext())
            {
                switch (lexan.Current.Token)
                {
                    case Semantics.PROJECT:
                        solution.Projects.Add(GetProject(lexan));
                        break;
                    case Semantics.GLOBAL:
                        solution.Globals.Add(GetGlobal(lexan));
                        break;
                    case Semantics.EOL:
                        break;
                    case Semantics.STRING_VALUE:
                        if (lexan.Current.Value.Trim().Equals("VisualStudioVersion"))
                        {
                            solution.VisualStudioVersion = GetProperty(lexan);
                        }
                        else if (lexan.Current.Value.Trim().Equals("MinimumVisualStudioVersion"))
                        {
                            solution.MinimumVisualStudioVersion = GetProperty(lexan);
                        }
                        break;
                    default:
                        throw new Exception("Unknown Solution token: " + lexan.Current.Token);
                }
            }


            return solution;
        }

        private static string GetProperty(LexicalAnalizer lexan)
        {
            lexan.MoveNext();
            lexan.Expect(Semantics.EQUALS);

            lexan.MoveNext();
            lexan.Expect(Semantics.STRING_VALUE);
            string propertyValue = lexan.Current.Value;

            lexan.MoveNext();
            lexan.Expect(Semantics.EOL);

            return propertyValue;
        }

        public static Solution GetSolution(string solutionFile)
        {
            return GetSolution(new FileInfo(solutionFile));
        }


        private static Project GetProject(LexicalAnalizer lexan)
        {
            Project project = new Project();

            lexan.Expect(Semantics.PROJECT);
            lexan.MoveNext();

            lexan.Expect(Semantics.OPEN_PARENTHESIS);
            lexan.MoveNext();

            // Project Type GUID
            lexan.Expect(Semantics.QUOTED_STRING);
            project.ProjectTypeGUID = lexan.Current.Value;
            lexan.MoveNext();

            lexan.Expect(Semantics.CLOSE_PARENTHESIS);
            lexan.MoveNext();

            lexan.Expect(Semantics.EQUALS);
            lexan.MoveNext();

            // project name
            lexan.Expect(Semantics.QUOTED_STRING);
            project.ProjectName = lexan.Current.Value;
            lexan.MoveNext();

            lexan.Expect(Semantics.COMMA);
            lexan.MoveNext();

            // project path
            lexan.Expect(Semantics.QUOTED_STRING);
            project.ProjectPath = lexan.Current.Value;
            lexan.MoveNext();

            lexan.Expect(Semantics.COMMA);
            lexan.MoveNext();

            // project guid
            lexan.Expect(Semantics.QUOTED_STRING);
            project.ProjectGUID = lexan.Current.Value;
            lexan.MoveNext();

            lexan.Expect(Semantics.EOL);

            while (lexan.MoveNext())
            {
                switch (lexan.Current.Token)
                {
                    case Semantics.END_PROJECT:
                        return project;
                    case Semantics.PROJECT_SECTION:
                        project.ProjectSections.Add(GetProjectSection(lexan));
                        break;
                    case Semantics.EOL:
                        break;
                    case Semantics.STRING_VALUE:
                        break;
                    default:
                        throw new Exception("Invalid Project Entry!");
                }

            }
            throw new Exception("Expecting EndProject!");
        }

        private static ProjectSection GetProjectSection(LexicalAnalizer lexan)
        {
            ProjectSection ps = new ProjectSection();
            lexan.Expect(Semantics.PROJECT_SECTION);
            lexan.MoveNext();

            lexan.Expect(Semantics.OPEN_PARENTHESIS);
            lexan.MoveNext();

            // Project Section Name
            lexan.Expect(Semantics.STRING_VALUE);
            ps.Name = lexan.Current.Value;
            lexan.MoveNext();


            lexan.Expect(Semantics.CLOSE_PARENTHESIS);
            lexan.MoveNext();

            lexan.Expect(Semantics.EQUALS);
            lexan.MoveNext();
            
            
            // Project Section Name
            lexan.Expect(Semantics.STRING_VALUE);
            ps.Value = lexan.Current.Value;
            lexan.MoveNext();


            lexan.Expect(Semantics.EOL);

            while (lexan.MoveNext())
            {
                switch (lexan.Current.Token)
                {
                    case Semantics.END_PROJECT_SECTION:
                        return ps;
                    case Semantics.STRING_VALUE:
                        {
                            lexan.Expect(Semantics.STRING_VALUE);
                            string key = lexan.Current.Value;
                            lexan.MoveNext();

                            lexan.Expect(Semantics.EQUALS);
                            lexan.MoveNext();

                            lexan.ExpectQuotedOrString();
                            string value = lexan.Current.Value;
                            ps.Map.Add(key.Trim(' ', '\n', '\t'), value);
                            lexan.MoveNext();

                            lexan.Expect(Semantics.EOL);

                        }
                        break;
                    case Semantics.QUOTED_STRING:
                        {
                            lexan.Expect(Semantics.QUOTED_STRING);
                            string key = lexan.Current.Value;
                            lexan.MoveNext();

                            lexan.Expect(Semantics.EQUALS);
                            lexan.MoveNext();

                            lexan.ExpectQuotedOrString();
                            string value = lexan.Current.Value;
                            ps.Map.Add(key.Trim(' ', '\n', '\t'), value);
                            lexan.MoveNext();

                            lexan.Expect(Semantics.EOL);

                        }
                        break;
                    case Semantics.EOL:
                        break;
                    default:
                        throw new Exception("Invalid ProjectSection Entry!");
                }

            }


            throw new Exception("Expecting EndProjectSection!");
        }


        private static Global GetGlobal(LexicalAnalizer lexan)
        {
            Global global = new Global();
            lexan.Expect(Semantics.GLOBAL);
            lexan.MoveNext();
            
            
            lexan.Expect(Semantics.EOL);




            while (lexan.MoveNext())
            {
                switch (lexan.Current.Token)
                {
                    case Semantics.END_GLOBAL:
                        return global;
                    case Semantics.GLOBAL_SECTION:
                        global.GlobalSections.Add(GetGlobalSection(lexan));
                        break;
                    case Semantics.EOL:
                        break;
                    case Semantics.STRING_VALUE:
                        break;
                    default:
                        throw new Exception("Invalid Global Entry!");
                }

            }


            throw new Exception("Expecting EndGlobal!");
        }

        private static GlobalSection GetGlobalSection(LexicalAnalizer lexan)
        {
            GlobalSection gs = new GlobalSection();
            
            lexan.Expect(Semantics.GLOBAL_SECTION);
            lexan.MoveNext();

            lexan.Expect(Semantics.OPEN_PARENTHESIS);
            lexan.MoveNext();

            lexan.Expect(Semantics.STRING_VALUE);
            gs.Name = lexan.Current.Value;
            lexan.MoveNext();

            lexan.Expect(Semantics.CLOSE_PARENTHESIS);
            lexan.MoveNext();

            lexan.Expect(Semantics.EQUALS);
            lexan.MoveNext();

            lexan.Expect(Semantics.STRING_VALUE);
            gs.Value = lexan.Current.Value;
            lexan.MoveNext();

            lexan.Expect(Semantics.EOL);




            while (lexan.MoveNext())
            {
                switch (lexan.Current.Token)
                {
                    case Semantics.END_GLOBAL_SECTION:
                        return gs;
                    case Semantics.STRING_VALUE:
                        {
                            lexan.Expect(Semantics.STRING_VALUE);
                            string key = lexan.Current.Value;
                            lexan.MoveNext();

                            lexan.Expect(Semantics.EQUALS);
                            lexan.MoveNext();

                            lexan.ExpectQuotedOrString();
                            string value = lexan.Current.Value;
                            gs.Map.Add(key.Trim(' ', '\n', '\t'), value);
                            lexan.MoveNext();

                            lexan.Expect(Semantics.EOL);

                        }
                        break;
                    case Semantics.QUOTED_STRING:
                        {
                            lexan.Expect(Semantics.QUOTED_STRING);
                            string key = lexan.Current.Value;
                            lexan.MoveNext();

                            lexan.Expect(Semantics.EQUALS);
                            lexan.MoveNext();

                            lexan.ExpectQuotedOrString();
                            string value = lexan.Current.Value;
                            gs.Map.Add(key.Trim(' ', '\n', '\t'), value);
                            lexan.MoveNext();

                            lexan.Expect(Semantics.EOL);

                        }
                        break;
                    case Semantics.EOL:
                        break;
                    default:
                        throw new Exception("Invalid GlobalSection Entry!");
                }

            }


            throw new Exception("Expecting EndGlobalSection!");
        }

    }
}
