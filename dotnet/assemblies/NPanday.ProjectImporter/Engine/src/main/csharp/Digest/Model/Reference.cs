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
using System.Net;
using System.Windows.Forms;
using log4net;
using NPanday.Artifact;
using NPanday.Utils;

/// Author: Leopoldo Lee Agdeppa III

namespace NPanday.ProjectImporter.Digest.Model
{

    public class Reference : IncludeBase
    {
        #region Constructors

        public Reference(string projectBasePath)
            : base(projectBasePath)
        {
        }

        #endregion

        #region Properties

        private string name;
        public string Name
        {
            get { return name; }
            set { name = value; }
        }

        private string hintPath;
        public string HintPath
        {
            get { return hintPath; }
            set
            {
                if (string.IsNullOrEmpty(value))
                {
                    return;
                }

                hintPath = value;
            }
        }

        public string HintFullPath
        {
            get
            {
                if (string.IsNullOrEmpty(hintPath))
                {
                    return null;
                }
                else if (Path.IsPathRooted(hintPath))
                {
                    return Path.GetFullPath(hintPath);
                }
                else
                {
                    return Path.GetFullPath(Path.Combine(projectBasePath, hintPath));
                }

            }
        }

        private string version;
        public string Version
        {
            get { return version; }
            set { version = value; }
        }

        private string publicKeyToken;
        public string PublicKeyToken
        {
            get { return publicKeyToken; }
            set { publicKeyToken = value; }
        }

        private string culture;
        public string Culture
        {
            get { return culture; }
            set { culture = value; }
        }

        private string processorArchitecture;
        public string ProcessorArchitecture
        {
            get { return processorArchitecture; }
            set { processorArchitecture = value; }
        }

        #endregion

        private static readonly ILog log = LogManager.GetLogger(typeof(Reference));

        #region HelperMethods

        public string getBinReference(string fileName)
        {
            string path = Path.Combine(this.IncludeFullPath, @"bin\" + Path.GetFileName(fileName));

            if (File.Exists(path))
                return path;

            path = Path.Combine(this.IncludeFullPath, @"bin\debug\" + Path.GetFileName(fileName));
            if (File.Exists(path))
                return path;

            path = Path.Combine(this.IncludeFullPath, @"bin\release\" + Path.GetFileName(fileName));
            if (File.Exists(path))
                return path;

            path = Path.Combine(this.IncludeFullPath, @"obj\debug\" + Path.GetFileName(fileName));
            if (File.Exists(path))
                return path;

            path = Path.Combine(this.IncludeFullPath, @"obj\release\" + Path.GetFileName(fileName));
            if (File.Exists(path))
                return path;

            return string.Empty;
        }

        public static bool DownloadArtifact(Artifact.Artifact artifact)
        {
            return downloadArtifactFromRemoteRepository(artifact, artifact.FileInfo.Extension);
        }

        // TODO: belongs in another utility classs
        public static bool downloadArtifactFromRemoteRepository(Artifact.Artifact artifact, string ext)
        {
            try
            {
                Dictionary<string, string> repos = SettingsUtil.GetSettingsRepositories();
                foreach (string id in repos.Keys)
                {
                    string url = repos[id];

                    ArtifactContext artifactContext = new ArtifactContext();

                    if (artifact.Version.Contains("SNAPSHOT"))
                    {
                        string newVersion = GetSnapshotVersion(artifact, url);

                        if (newVersion != null)
                        {
                            artifact.RemotePath = artifactContext.GetArtifactRepository().GetRemoteRepositoryPath(artifact, artifact.Version.Replace("SNAPSHOT", newVersion), url, ext);
                        }

                        else
                        {
                            artifact.RemotePath = artifactContext.GetArtifactRepository().GetRemoteRepositoryPath(artifact, url, ext);
                        }

                    }
                    else
                    {
                        artifact.RemotePath = artifactContext.GetArtifactRepository().GetRemoteRepositoryPath(artifact, url, ext);
                    }

                    if (downloadArtifact(artifact))
                    {
                        return true;
                    }
                }
                return false;
            }
            catch (Exception e)
            {
                log.Error("Cannot add reference of " + artifact.ArtifactId + ", an exception occurred trying to download it: " + e.Message);
                return false;
            }
        }

        private static string GetSnapshotVersion(NPanday.Artifact.Artifact artifact, string repo)
        {
            WebClient client = new WebClient();
            string timeStampVersion = null;
            string metadataPath = repo + "/" + artifact.GroupId.Replace('.', '/') + "/" + artifact.ArtifactId;
            string snapshot = "<snapshot>";
            string metadata = "/maven-metadata.xml";

            try
            {
                metadataPath = metadataPath + "/" + artifact.Version + metadata;

                string content = client.DownloadString(metadataPath);
                string[] lines = content.Split(new string[] { "\r\n", "\r", "\n" }, StringSplitOptions.None);

                string timeStamp = null;
                string buildNumber = null;

                foreach (string line in lines)
                {
                    int startIndex;
                    int len;

                    if (line.Contains("<timestamp>"))
                    {
                        startIndex = line.IndexOf("<timestamp>") + "<timestamp>".Length;
                        len = line.IndexOf("</timestamp>") - startIndex;

                        timeStamp = line.Substring(startIndex, len);
                    }

                    if (line.Contains("<buildNumber>"))
                    {
                        startIndex = line.IndexOf("<buildNumber>") + "<buildNumber>".Length;
                        len = line.IndexOf("</buildNumber>") - startIndex;

                        buildNumber = line.Substring(startIndex, len);
                    }
                }

                if (timeStamp == null)
                {
                    log.Warn("Timestamp was not specified in maven-metadata.xml - using default snapshot version");
                    return null;
                }

                if (buildNumber == null)
                {
                    log.Warn("Build number was not specified in maven-metadata.xml - using default snapshot version");
                    return null;
                }

                log.Info("Resolved SNAPSHOT: Timestamp = " + timeStamp + "; Build Number = " + buildNumber);
                timeStampVersion = timeStamp + "-" + buildNumber;
            }
            catch (Exception e)
            {
                return null;
            }
            finally
            {
                client.Dispose();
            }

            return timeStampVersion;
        }

        static bool downloadArtifact(Artifact.Artifact artifact)
        {
            WebClient client = new WebClient();
            bool dirCreated = false;

            try
            {
                if (!artifact.FileInfo.Directory.Exists)
                {
                    artifact.FileInfo.Directory.Create();
                    dirCreated = true;
                }


                log.InfoFormat("Download Start: {0} Downloading From {1}", DateTime.Now, artifact.RemotePath);

                client.DownloadFile(artifact.RemotePath, artifact.FileInfo.FullName);

                log.InfoFormat("Download Finished: {0}", DateTime.Now);

                string artifactDir = GetLocalUacPath(artifact, artifact.FileInfo.Extension);

                if (!Directory.Exists(Path.GetDirectoryName(artifactDir)))
                {
                    Directory.CreateDirectory(Path.GetDirectoryName(artifactDir));
                }
                if (!File.Exists(artifactDir))
                {
                    File.Copy(artifact.FileInfo.FullName, artifactDir);
                }

                return true;

            }

            catch (Exception e)
            {
                if (dirCreated)
                {
                    artifact.FileInfo.Directory.Delete();
                }

                log.WarnFormat("Download Failed {0}", e.Message);

                return false;
            }

            finally
            {
                client.Dispose();
            }
        }


        public static string GetLocalUacPath(Artifact.Artifact artifact, string ext)
        {
            return Path.Combine(SettingsUtil.GetLocalRepositoryPath(), string.Format(@"{0}\{1}\{1}{2}-{3}", Tokenize(artifact.GroupId), artifact.ArtifactId, artifact.Version, ext));
        }

        public static string Tokenize(string id)
        {
            return id.Replace(".", Path.DirectorySeparatorChar.ToString());
        }

        public void SetAssemblyInfoValues(string assemblyInfo)
        {
            if (!string.IsNullOrEmpty(assemblyInfo))
            {
                string[] referenceValues = assemblyInfo.Split(',');
                this.Name = referenceValues[0].Trim();

                if (referenceValues.Length > 1)
                {
                    for (int i = 1; i < referenceValues.Length; i++)
                    {
                        if (referenceValues[i].Contains("Version="))
                        {
                            this.Version = referenceValues[i].Replace("Version=", "").Trim();
                        }
                        else if (referenceValues[i].Contains("PublicKeyToken="))
                        {
                            this.PublicKeyToken = referenceValues[i].Replace("PublicKeyToken=", "").Trim();
                        }
                        else if (referenceValues[i].Contains("Culture="))
                        {
                            this.Culture = referenceValues[i].Replace("Culture=", "").Trim();
                        }
                        else if (referenceValues[i].Contains("processorArchitecture="))
                        {
                            this.ProcessorArchitecture = referenceValues[i].Replace("processorArchitecture=", "").Trim();
                        }
                    }
                }

            }

        }

        #endregion






    }
}
