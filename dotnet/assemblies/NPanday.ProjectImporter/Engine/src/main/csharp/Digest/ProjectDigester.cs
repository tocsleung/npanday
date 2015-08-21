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
using System.IO;
using System.Windows.Forms;
using log4net;
using Microsoft.Build.BuildEngine;
using NPanday.ProjectImporter.Digest.Algorithms;
using NPanday.ProjectImporter.Digest.Model;
using NPanday.ProjectImporter.Parser.VisualStudioProjectTypes;

/// Author: Leopoldo Lee Agdeppa III

namespace NPanday.ProjectImporter.Digest
{
    public sealed class ProjectDigester
    {
        private static readonly ILog log = LogManager.GetLogger(typeof(ProjectDigester));

        public delegate ProjectDigest DigestProject(Dictionary<string, object> project, DependencySearchConfiguration depSearchConfig);
        private static readonly Dictionary<VisualStudioProjectTypeEnum, DigestProject> _digestAlgoritms;

        static ProjectDigester()
        {
            _digestAlgoritms = new Dictionary<VisualStudioProjectTypeEnum, DigestProject>();

            _digestAlgoritms.Add(VisualStudioProjectTypeEnum.Windows__CSharp, new NormalProjectDigestAlgorithm().DigestProject);
            _digestAlgoritms.Add(VisualStudioProjectTypeEnum.Windows__VbDotNet, new NormalProjectDigestAlgorithm().DigestProject);
            _digestAlgoritms.Add(VisualStudioProjectTypeEnum.Web_Site, new WebProjectDigestAlgorithm().DigestProject);
            
            
            // combined Projects
            _digestAlgoritms.Add(
                    VisualStudioProjectTypeEnum.Web_Site | VisualStudioProjectTypeEnum.Windows__CSharp, 
                    new NormalProjectDigestAlgorithm().DigestProject
                );
            
            _digestAlgoritms.Add(
                    VisualStudioProjectTypeEnum.Web_Site | VisualStudioProjectTypeEnum.Windows__VbDotNet, 
                    new NormalProjectDigestAlgorithm().DigestProject
                );


            _digestAlgoritms.Add(
                    VisualStudioProjectTypeEnum.Web_Application | VisualStudioProjectTypeEnum.Windows__CSharp,
                    new NormalProjectDigestAlgorithm().DigestProject
                );

            _digestAlgoritms.Add(
                    VisualStudioProjectTypeEnum.Web_Application | VisualStudioProjectTypeEnum.Windows__VbDotNet,
                    new NormalProjectDigestAlgorithm().DigestProject
                );

            _digestAlgoritms.Add(
                    VisualStudioProjectTypeEnum.WindowsAzure_CloudService,
                    new NormalProjectDigestAlgorithm().DigestProject
                );
        }

        

        public static ProjectDigest[] DigestProjects(List<Dictionary<string, object>> projects, DependencySearchConfiguration depSearchConfig, ref string warningMsg)
        {
            List<ProjectDigest> projectDigests = new List<ProjectDigest>();
            Dictionary<string, ProjectDigest> projDigestDictionary = new Dictionary<string, ProjectDigest>();
            Dictionary<string, ProjectDigest> projDigestGuidDictionary = new Dictionary<string, ProjectDigest>();

            foreach (Dictionary<string, object> project in projects)
            {
                DigestProject digestProject = _digestAlgoritms[(VisualStudioProjectTypeEnum)project["ProjectType"]];
                ProjectDigest projDigest = digestProject(project, depSearchConfig);
                projectDigests.Add(projDigest);
                if (projDigestDictionary.ContainsKey(projDigest.ProjectName))
                {
                    throw new Exception("Projects with duplicated assembly names are not supported: " + projDigest.ProjectName);
                }

                log.DebugFormat("Digested project Name = {0}, GUID = {1}", projDigest.ProjectName, projDigest.ProjectGuid);

                projDigestDictionary.Add(projDigest.ProjectName, projDigest);
                projDigestGuidDictionary.Add(projDigest.ProjectGuid, projDigest);
            }

            List<ProjectDigest> tobeIncluded = new List<ProjectDigest>();

            // verify if all project dependencies are in the solution
            foreach (ProjectDigest projectDigest in projectDigests)
            {
                foreach (ProjectReference projectReference in projectDigest.ProjectReferences)
                {
                    string refName = projectReference.Name;

                    if (!projDigestDictionary.ContainsKey(refName))
                    {
                        Project prjRef = GetProject(projectReference.ProjectFullPath);
                        if (prjRef == null)
                        {
                            // this might not be possible
                            warningMsg = string.Format(
                            "{0}\n    Missing project reference {1} located at {2}!"+
                            "\n        Note this might cause a missing artifact dependency!", 
                                warningMsg,
                                refName,
                                projectReference.ProjectFullPath);
                            continue;
                        }

                        DigestProject digestProject = _digestAlgoritms[VisualStudioProjectTypeEnum.Windows__CSharp];

                        Dictionary<string, object> projectMap = new Dictionary<string, object>();
                        projectMap.Add("Project", prjRef);

                        ProjectDigest prjRefDigest = digestProject(projectMap, depSearchConfig);
                        string errMsg = string.Format(
                            "Project \"{0}\"  requires \"{1}\" which is not included in the Solution File, "
                            + "\nWould you like to include \"{1}\" Generating NPanday Project Poms?"
                            + "\nNote: Not adding \"{1}\" will result to a missing Artifact Dependency \"{1}\"",
                            projectDigest.ProjectName,
                            prjRefDigest.ProjectName);

                        // TODO: should not be in the importer
                        DialogResult includeResult = MessageBox.Show(errMsg, "Include Project in Pom Generation:",
                            MessageBoxButtons.YesNo,
                            MessageBoxIcon.Question);

                        if (includeResult == DialogResult.Yes)
                        {
                            projDigestDictionary.Add(prjRefDigest.ProjectName, prjRefDigest);
                            tobeIncluded.Add(prjRefDigest);
                        }
                        else
                        {
                            warningMsg = string.Format(
                                "{0}\n    Please Make sure that Artifact[GroupId: {1}, ArtifactId: {1}] exists in your NPanday Repository, " +
                                "\n        Or an error will occur during NPanday-Build due to Missing Artifact Dependency!",
                                warningMsg, prjRefDigest.ProjectName);
                        }
                    }
                }
            }

            // add tobe included
            projectDigests.AddRange(tobeIncluded);

            // insert the projectRererences
            foreach (ProjectDigest prjDigest in projectDigests)
            {
                if (prjDigest.ProjectReferences != null)
                {
                    for (int i = 0; i < prjDigest.ProjectReferences.Length; i++)
                    {
                        ProjectReference prjRef = prjDigest.ProjectReferences[i];
                        if (projDigestDictionary.ContainsKey(prjRef.Name))
                        {
                            ProjectDigest pd = projDigestDictionary[prjRef.Name];
                            prjRef.ProjectReferenceDigest = pd;
                        }
                    }
                }

                if (prjDigest.SilverlightApplicationList != null)
                {
                    foreach (SilverlightApplicationReference app in prjDigest.SilverlightApplicationList)
                    {
                        if (!projDigestGuidDictionary.ContainsKey(app.Guid))
                        {
                            throw new Exception("Silverlight application in the web application project must be included in the solution: " + app.ToString());
                        }
                        app.Project = projDigestGuidDictionary[app.Guid];
                    }
                }
            }

            
            // sort by inter-project dependency
            projectDigests.Sort(CompareByDependency);

            return projectDigests.ToArray();
        }


        // dependency sorter
        private static int CompareByDependency(ProjectDigest x, ProjectDigest y)
        {
            // Less than 0, x is less than y. (x is referring to y)
            // 0, x equals y. (not refering to each other)
            // Greater than 0, x is greater than y. (x is reffered by y)

            foreach (ProjectReference prjRef in y.ProjectReferences)
            {
                if (x.ProjectName.Equals(prjRef.Name))
                {
                    // Greater than 0, x is greater than y. (x is reffered by y)
                    return -1;
                }
            }

            foreach (ProjectReference prjRef in x.ProjectReferences)
            {
                if (y.ProjectName.Equals(prjRef.Name))
                {
                    // Less than 0, x is less than y. (x is referring to y)
                    return 1;
                }
            }
            // x equals y.
            return 0;

        }




        public static Project GetProject(string projectFile)
        {
            FileInfo projectFileInfo = new FileInfo(projectFile);

            if (!projectFileInfo.Exists)
            {
                return null;
            }

            // Note: even though .ccproj is excluded here, it isn't a problem since it will never be called (only used for resolving ProjectReferences onto this project)
            if (!(projectFileInfo.Extension.ToUpper().Equals(".CSPROJ") || projectFileInfo.Extension.ToUpper().Equals(".VBPROJ")))
            {
                throw new Exception( "Unrecognized project type: " + projectFileInfo.Extension + " for file " + projectFile );
            }

            // gets the directory path of mscorlib using the System.String Type Assembly path
            string msBuildPath = Path.GetDirectoryName(System.Reflection.Assembly.GetAssembly(typeof(string)).Location);
            Engine engine = new Engine(msBuildPath);

            Project project = new Project(engine);

            // TODO: if we update to a .NET 3.5 minimum we can pass in ProjectLoadSettings.IgnoreMissingImports
            project.Load(projectFile);

            return project;
        }


        
    }
}
