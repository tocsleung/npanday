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
using NPanday.ProjectImporter.Parser.SlnParser;
using NPanday.ProjectImporter.Parser.SlnParser.Model;
using System.IO;
using NPanday.ProjectImporter.Parser.VisualStudioProjectTypes;
using System.Text.RegularExpressions;
using Microsoft.Build.BuildEngine;
using System.Xml;
using System.Windows.Forms;
using log4net;
using System.Xml.Xsl;

/// Author: Leopoldo Lee Agdeppa III

namespace NPanday.ProjectImporter.Parser.SlnParser
{
    public class ProjectSolutionParser
    {
        private static readonly ILog log = LogManager.GetLogger(typeof(ProjectSolutionParser));

        protected static string PROJECT_REFERENCE_REGEX;

        protected static Engine BUILD_ENGINE;

        static ProjectSolutionParser()
        {
            PROJECT_REFERENCE_REGEX = @"({(?<ProjectReferenceGUID>([^\}])*)}\|(?<ProjectReferenceDll>([^;])*);)";

            // gets the directory path of mscorlib using the System.String Type Assembly path
            string msBuildPath = Path.GetDirectoryName(System.Reflection.Assembly.GetAssembly(typeof(string)).Location);
            BUILD_ENGINE = new Engine(msBuildPath);
        }

        public List<Dictionary<string, object>> Parse(FileInfo solutionFile, Dictionary<string, string> globalProperties, ref string warningMsg)
        {
            NPanday.ProjectImporter.Parser.SlnParser.Model.Solution solution;

            List<Dictionary<string, object>> list = new List<Dictionary<string, object>>();
            try
            {
                solution = SolutionFactory.GetSolution(solutionFile);
            }
            catch (Exception exception)
            {
                throw new Exception("Unknown error occurred parsing solution file: " + solutionFile + " (embedded error is: \"" + exception.Message + "\")");
            }

            String UnsupportedProjectsMessage = string.Empty;

            foreach (NPanday.ProjectImporter.Parser.SlnParser.Model.Project project in solution.Projects)
            {
                Dictionary<string, object> dictionary = new Dictionary<string, object>();

                string fullpath = Path.Combine(solutionFile.DirectoryName, project.ProjectPath);

                VisualStudioProjectTypeEnum type;
                try
                {
                    // Skip folders
                    // TODO: merge with method in VS Add-in
                    if (project.ProjectTypeGUID.Equals("{66A26720-8FB5-11D2-AA7E-00C04F688DDE}", StringComparison.OrdinalIgnoreCase)
                        || project.ProjectTypeGUID.Equals("{2150E333-8FDC-42A3-9474-1A3956D46DE8}", StringComparison.OrdinalIgnoreCase))
                    {
                        continue;
                    }
                    else
                    {
                        type = VisualStudioProjectType.GetVisualStudioProjectType(project.ProjectTypeGUID);
                    }
                }
                catch (Exception e)
                {
                    log.ErrorFormat("Error reading project with type GUID {0}: {1}", project.ProjectTypeGUID, e.Message);
                    if (string.Empty.Equals(UnsupportedProjectsMessage))
                    {
                        UnsupportedProjectsMessage += project.ProjectName;
                    }
                    else
                    {
                        UnsupportedProjectsMessage += ", " + project.ProjectName;
                    }
                    continue;
                }

                dictionary.Add("ProjectType", type);
                dictionary.Add("ProjectTypeGuid", project.ProjectTypeGUID);
                dictionary.Add("ProjectName", project.ProjectName);
                dictionary.Add("ProjectPath", project.ProjectPath);
                dictionary.Add("ProjectGUID", project.ProjectGUID);
                dictionary.Add("ProjectFullPath", fullpath);

                // this is for web projects
                if ((VisualStudioProjectTypeEnum)dictionary["ProjectType"] == VisualStudioProjectTypeEnum.Web_Site)
                {

                    string[] assemblies = GetWebConfigAssemblies(Path.Combine(fullpath, "web.config"));
                    dictionary.Add("WebConfigAssemblies", assemblies);

                    //get project target framework if available
                    if (project.ProjectSections.Count > 0)
                    {
                        if (project.ProjectSections[0].Map.ContainsKey("TargetFramework"))
                        {
                            dictionary.Add("TargetFramework", project.ProjectSections[0].Map["TargetFramework"]);
                        }
                    }

                    //@001 SERNACIO START retrieving webreference
                    Digest.Model.WebReferenceUrl[] webReferences = getWebReferenceUrls(fullpath);
                    dictionary.Add("WebReferencesUrl", webReferences);
                    //@001 SERNACIO END retrieving webreference

                    string[] binAssemblies = GetBinAssemblies(Path.Combine(fullpath, @"bin"));
                    dictionary.Add("BinAssemblies", binAssemblies);
                    //ParseInnerData(dictionary, match.Groups["projectInnerData"].ToString());
                    ParseProjectReferences(dictionary, project, solution);
                }
                // this is for normal projects
                else
                {
                    Microsoft.Build.BuildEngine.Project prj = new Microsoft.Build.BuildEngine.Project(BUILD_ENGINE);

                    try
                    {
                        // TODO: if we update to a .NET 3.5 minimum we can pass in ProjectLoadSettings.IgnoreMissingImports, and ignore the visualstudioversion
                        if (globalProperties != null)
                        {
                            foreach (KeyValuePair<string, string> entry in globalProperties)
                            {
                                prj.GlobalProperties.SetProperty(entry.Key, entry.Value);
                            }
                        }

                        // Some debugging to troubleshoot an infrequent error where Microsoft.Build throws a NullReferenceException.
                        // This may have been mitigated by setting VisualStudioVersion and can be removed if the problem doesn't re-appear.
                        if (log.IsDebugEnabled)
                        {
                            log.DebugFormat("VisualStudioVersion: global {0}, evaluated {1}",
                                prj.GlobalProperties["VisualStudioVersion"], prj.EvaluatedProperties["VisualStudioVersion"]);
                            log.DebugFormat("CloudExtensionsDir: global {0}, evaluated {1}",
                                prj.GlobalProperties["CloudExtensionsDir"], prj.EvaluatedProperties["CloudExtensionsDir"]);
                            log.DebugFormat("MSBuildExtensionsPath: global {0}, evaluated {1}",
                                prj.GlobalProperties["MSBuildExtensionsPath"], prj.EvaluatedProperties["MSBuildExtensionsPath"]);
                        }

                        prj.Load(fullpath);
                    }
                    catch (NullReferenceException e)
                    {
                        log.Error("Error reading project from path " + fullpath, e);
                        // workaround it by removing the imports
                        prj.LoadXml(TransformProjectFile(fullpath));
                        prj.FullFileName = fullpath;
                    }
                    catch (Exception e)
                    {
                        log.Error("Error reading project from path " + fullpath, e);
                        if (string.Empty.Equals(UnsupportedProjectsMessage))
                        {
                            UnsupportedProjectsMessage += project.ProjectName;
                        }
                        else
                        {
                            UnsupportedProjectsMessage += ", " + project.ProjectName;
                        }
                        continue;
                    }

                    //ParseInnerData(dictionary, match.Groups["projectInnerData"].ToString());
                    ParseProjectReferences(dictionary, project, solution);
                    dictionary.Add("Project", prj);
                }

                list.Add(dictionary);
            }
            if (!string.Empty.Equals(UnsupportedProjectsMessage))
            {
                warningMsg = string.Format("{0}\n    Unsupported Projects: {1}", warningMsg, UnsupportedProjectsMessage);
            }
            
            return list;
        }

        private string TransformProjectFile(string fullpath)
        {
            XslCompiledTransform transform = new XslCompiledTransform();
            transform.Load(XmlReader.Create(new StringReader(
                @"<?xml version='1.0' encoding='ISO-8859-1'?>
<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns:msbuild='http://schemas.microsoft.com/developer/msbuild/2003'>
  <xsl:template match='@*|node()'>
    <xsl:copy>
      <xsl:apply-templates select='@*|node()'/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match='msbuild:Import[@Project != """"]'/>
</xsl:stylesheet>" )));
            StringWriter w = new StringWriter();
            transform.Transform(fullpath, XmlWriter.Create(w));
            log.DebugFormat("Project XML: {0}", w.ToString());
            return w.ToString();
        }

        protected void ParseProjectReferences(Dictionary<string, object> dictionary, NPanday.ProjectImporter.Parser.SlnParser.Model.Project project, NPanday.ProjectImporter.Parser.SlnParser.Model.Solution solution)
        {
            if (project.ProjectSections != null)
            {
                List<Microsoft.Build.BuildEngine.Project> projectReferenceList = new List<Microsoft.Build.BuildEngine.Project>();
                foreach (ProjectSection ps in project.ProjectSections)
                {
                    if ("WebsiteProperties".Equals(ps.Name))
                    {
                        // ProjectReferences = "{11F2FCC8-5941-418A-A0E7-42D250BA9D21}|SampleInterProject111.dll;{9F37BA7B-06F9-4B05-925D-B5BC16322E8B}|BongClassLib.dll;"

                        try
                        {
                            Regex regex = new Regex(PROJECT_REFERENCE_REGEX, RegexOptions.Multiline | RegexOptions.IgnoreCase);
                            MatchCollection matches = regex.Matches(ps.Map["ProjectReferences"]);


                            foreach (Match match in matches)
                            {
                                string projectReferenceGUID = match.Groups["ProjectReferenceGUID"].ToString();
                                string projectReferenceDll = match.Groups["ProjectReferenceDll"].ToString();
                                
                                Microsoft.Build.BuildEngine.Project prj = GetMSBuildProject(solution, projectReferenceGUID);
                                if (prj != null)
                                {
                                    projectReferenceList.Add(prj);
                                }
                            }
                        }
                        catch { }




                    }
                    else if("ProjectDependencies".Equals(ps.Name))
                    {
                        //{0D80BE11-F1CE-409E-B9AC-039D3801209F} = {0D80BE11-F1CE-409E-B9AC-039D3801209F}

                        foreach (string key in ps.Map.Keys)
                        {
                            Microsoft.Build.BuildEngine.Project prj = GetMSBuildProject(solution, key.Replace("{","").Replace("}", ""));
                            if (prj != null)
                            {
                                projectReferenceList.Add(prj);
                            }
                        }

                    }
                }

                dictionary.Add("InterProjectReferences", projectReferenceList.ToArray());
            }


            
        }

        Microsoft.Build.BuildEngine.Project GetMSBuildProject(NPanday.ProjectImporter.Parser.SlnParser.Model.Solution solution, string projectGuid)
        {

            foreach (NPanday.ProjectImporter.Parser.SlnParser.Model.Project p in solution.Projects)
            {
                if (p.ProjectGUID.Equals("{" + projectGuid + "}", StringComparison.OrdinalIgnoreCase))
                {
                    string projectReferenceName = p.ProjectName;
                    string projectReferencePath = p.ProjectPath;
                    string projectReferenceFullPath = null;

                    if (Path.IsPathRooted(projectReferencePath))
                    {
                        projectReferenceFullPath = Path.GetFullPath(projectReferencePath);
                    }
                    else
                    {
                        projectReferenceFullPath = Path.Combine(solution.File.Directory.FullName, projectReferencePath);
                    }


                    Microsoft.Build.BuildEngine.Project prj = new Microsoft.Build.BuildEngine.Project(BUILD_ENGINE);

                    try
                    {
                        // TODO: if we update to a .NET 3.5 minimum we can pass in ProjectLoadSettings.IgnoreMissingImports
                        prj.Load(projectReferenceFullPath);
                    }
                    catch (Exception e)
                    {
                        log.Error("Unable to load project reference from " + projectReferenceFullPath, e);
                    }

                    return prj;

                }
            }

            return null;
        }

        protected string getWebSiteFrameworkVersion(string webconfig)
        {
            string xpath = @"//configuration/system.codedom/compilers/compiler";
            
            if (!File.Exists(webconfig))
                return "2.0";

            XmlDocument xdoc = new XmlDocument();
            xdoc.Load(webconfig);
            XmlNodeList nodes = xdoc.SelectNodes(xpath);
            if (nodes.Count > 0)
            {
                string v = "2.0";
                foreach (XmlNode node in nodes[0].ChildNodes)
                {
                    if ("CompilerVersion".Equals(node.Attributes[0].Value, StringComparison.OrdinalIgnoreCase))
                    {
                        v = node.Attributes[1].Value.Substring(1);
                        break;
                    }
                }

                return v;
            }
            else
                return "2.0";
        }

        protected string[] GetWebConfigAssemblies(string webconfig)
        {
            List<string> list = new List<string>();

            string xpath_expr = @"//configuration/system.web/compilation/assemblies/add";

            FileInfo webConfigFile = new FileInfo(webconfig);

            if (!webConfigFile.Exists)
            {
                // return empty string array
                return list.ToArray();
            }


            XmlDocument xmldoc = new System.Xml.XmlDocument();
            xmldoc.Load(webConfigFile.FullName);

            XmlNodeList valueList = xmldoc.SelectNodes(xpath_expr);

            foreach (System.Xml.XmlNode val in valueList)
            {
                string assembly = val.Attributes["assembly"].Value;

                if (!string.IsNullOrEmpty(assembly))
                {
                    list.Add(assembly);
                }

            }



            return list.ToArray();

        }


        protected String[] GetBinAssemblies(string webBinDir)
        {
            List<string> list = new List<string>();

            DirectoryInfo dir = new DirectoryInfo(webBinDir);

            if (!dir.Exists)
            {
                // return an empty array string
                return list.ToArray();
            }

            foreach (FileInfo dll in dir.GetFiles("*.dll"))
            {
                list.Add(dll.FullName);

            }



            return list.ToArray();



        }



        Digest.Model.WebReferenceUrl[] getWebReferenceUrls(string projectPath)
        {
            List<Digest.Model.WebReferenceUrl> returnList = new List<Digest.Model.WebReferenceUrl>();
            string webPath = Path.GetFullPath(Path.Combine(projectPath, "App_WebReferences"));
            if (Directory.Exists(webPath))
            {
                DirectoryInfo dirInfo = new DirectoryInfo(webPath);
                foreach (DirectoryInfo folders in dirInfo.GetDirectories())
                {
                    if (folders.Equals(".svn")) continue;
                    returnList.AddRange(getWebReferenceUrls(folders, "App_WebReferences"));
                }
            }
            return returnList.ToArray();
        }

        Digest.Model.WebReferenceUrl[] getWebReferenceUrls(DirectoryInfo folder, string currentPath)
        {
            string relPath = Path.Combine(currentPath, folder.Name);
            string url = string.Empty;
            List<Digest.Model.WebReferenceUrl> webReferenceUrls = new List<Digest.Model.WebReferenceUrl>();

            FileInfo[] fileInfo = folder.GetFiles("*.discomap");
            if (fileInfo != null && fileInfo.Length > 0)
            {
                System.Xml.XPath.XPathDocument xDoc = new System.Xml.XPath.XPathDocument(fileInfo[0].FullName);
                System.Xml.XPath.XPathNavigator xNav = xDoc.CreateNavigator();
                string xpathExpression = @"DiscoveryClientResultsFile/Results/DiscoveryClientResult[@referenceType='System.Web.Services.Discovery.ContractReference']/@url";
                System.Xml.XPath.XPathNodeIterator xIter = xNav.Select(xpathExpression);
                if (xIter.MoveNext())
                {
                    url = xIter.Current.TypedValue.ToString();
                }
            }
            if (!string.IsNullOrEmpty(url))
            {
                Digest.Model.WebReferenceUrl newWebReferenceUrl = new Digest.Model.WebReferenceUrl();
                newWebReferenceUrl.RelPath = relPath;
                newWebReferenceUrl.UpdateFromURL = url;
                webReferenceUrls.Add(newWebReferenceUrl);
            }
            foreach (DirectoryInfo dirInfo in folder.GetDirectories())
            {
                webReferenceUrls.AddRange(getWebReferenceUrls(dirInfo, relPath));
            }
            return webReferenceUrls.ToArray();
        }


    }
}
