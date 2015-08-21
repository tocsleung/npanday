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
using System.IO;

/// Author: Leopoldo Lee Agdeppa III

namespace NPanday.ProjectImporter.Parser.SlnParser.Model
{
    public class Solution
    {
        FileInfo file;
        public FileInfo File
        {
            get { return file; }
            set { file = value; }
        }

        string header;
        public string Header
        {
            get { return header; }
            set { header = value; }
        }

        string formatVersion;
        public string FormatVersion
        {
            get { return formatVersion; }
            set { formatVersion = value; }
        }

        string vsVersion;
        public string VsVersion
        {
            get { return vsVersion; }
            set { vsVersion = value; }
        }

        // New property introduced in VS 2013
        string visualStudioVersion;
        public string VisualStudioVersion
        {
            get { return visualStudioVersion; }
            set { visualStudioVersion = value; }
        }

        // New property introduced in VS 2013
        string minimumVisualStudioVersion;
        public string MinimumVisualStudioVersion
        {
            get { return minimumVisualStudioVersion; }
            set { minimumVisualStudioVersion = value; }
        }


        List<Project> projects = new List<Project>();
        public List<Project> Projects
        {
            get { return projects; }
            set { projects = value; }
        }

        List<Global> globals = new List<Global>();
        public List<Global> Globals
        {
            get { return globals; }
            set { globals = value; }
        }


        public override string ToString()
        {
            StringBuilder sb = new StringBuilder();

            sb.AppendLine("###########################################################################");
            sb.AppendLine("FileName: " + File.FullName);
            sb.AppendLine("Header: " + Header);
            sb.AppendLine("FormatVersion: " + FormatVersion);
            sb.AppendLine("VsVersion: " + VsVersion);
            sb.AppendLine("VisualStudioVersion: " + VisualStudioVersion);
            sb.AppendLine("MinimumVisualStudioVersion: " + MinimumVisualStudioVersion);

            sb.AppendLine(string.Format("\n\nProject Entries({0}):", projects.Count));
            foreach (Project project in projects)
            {
                sb.AppendLine("\t==============================================");
                sb.AppendLine("\tProjectGUID: " + project.ProjectGUID);
                sb.AppendLine("\tProjectName: " + project.ProjectName);
                sb.AppendLine("\tProjectPath: " + project.ProjectPath);
                sb.AppendLine("\tProjectTypeGUID: " + project.ProjectTypeGUID);


                foreach (ProjectSection ps in project.ProjectSections)
                {
                    sb.AppendLine("\t\t--------------------------------------");
                    sb.AppendLine("\t\tName: " + ps.Name);
                    sb.AppendLine("\t\tValue: " + ps.Value);

                    sb.AppendLine("\t\t\t..............................");
                    foreach (string key in ps.Map.Keys)
                    {
                        sb.AppendLine(string.Format("\t\t\t {0} = {1}\n", key, ps.Map[key]));
                    }
                    sb.AppendLine("\t\t\t..............................");

                    sb.AppendLine("\t\t--------------------------------------");
                }


                sb.AppendLine("\t==============================================");
            }



            sb.AppendLine(string.Format("\n\nGlobal Entries({0}):", globals.Count));
            foreach (Global global in globals)
            {
                sb.AppendLine("\t==============================================");

                foreach (GlobalSection gs in global.GlobalSections)
                {
                    sb.AppendLine("\t\t--------------------------------------");
                    sb.AppendLine("\t\tName: " + gs.Name);
                    sb.AppendLine("\t\tValue: " + gs.Value);
                    
                    sb.AppendLine("\t\t\t..............................");
                    foreach (string key in gs.Map.Keys)
                    {
                        sb.AppendLine(string.Format("\t\t\t {0} = {1}\n", key, gs.Map[key]));
                    }
                    sb.AppendLine("\t\t\t..............................");


                    sb.AppendLine("\t\t--------------------------------------");
                }


                sb.AppendLine("\t==============================================");
            }


            sb.AppendLine("###########################################################################");

            return sb.ToString();
        }
    }
}
