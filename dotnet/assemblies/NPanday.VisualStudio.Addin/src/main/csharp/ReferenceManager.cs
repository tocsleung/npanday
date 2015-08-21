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
using System.IO;
using EnvDTE;
using log4net;
using NPanday.Model.Pom;

namespace NPanday.VisualStudio.Addin
{
    #region Interfaces

    #endregion

    public class ReferenceManager : IReferenceManager
    {
        bool initialized = false;
        string pomFile;
        string projectPath;
        Solution solution;

        private static readonly ILog log = LogManager.GetLogger(typeof(ReferenceManager));

        #region IReferenceManager Members

        public Artifact.Artifact Add(IReferenceInfo reference)
        {
            EnsureInitialized();

            string artifactFileName = CopyToReferenceFolder(reference.Artifact, referenceFolder);

            Artifact.Artifact a = reference.Artifact;
            
            a.FileInfo = new FileInfo(artifactFileName);
            return a;
        }

        public void Remove(IReferenceInfo reference)
        {
            throw new Exception("The method or operation is not implemented.");
        }

        public void Initialize(VSLangProj80.VSProject2 project)
        {
            solution = project.Project.DTE.Solution;
            projectPath = Path.GetDirectoryName(project.Project.FileName);
            referenceFolder = Path.Combine( projectPath,".references");
            pomFile =  Path.Combine(projectPath, "pom.xml");

            initialized = true;
            if (!pomExist())
            {
                throw new Exception("Project has no valid pom file.");
            }
            CreateReferenceFolder();
        }

        string referenceFolder;
        public string ReferenceFolder
        {
            get
            {
                return referenceFolder;
            }
            //for testing purposes
            set
            {
                initialized = true;
                referenceFolder = value;
            }
        }

        public void CopyArtifact(Artifact.Artifact artifact)
        {
            CopyArtifactImpl(artifact, ArtifactResyncSource.RemoteRepository);
        }

        public void ResyncArtifacts()
        {
            ResyncArtifactsImpl(ArtifactResyncSource.RemoteRepository);
        }

        public void ResyncArtifactsFromLocalRepository()
        {
            ResyncArtifactsImpl(ArtifactResyncSource.LocalRepository);
        }

        #endregion

        #region privates

        private enum ArtifactResyncSource
        {
            RemoteRepository,
            LocalRepository
        }

        private void ResyncArtifactsImpl(
            ArtifactResyncSource artifactResyncSource)
        {
            EnsureInitialized();

            GetReferencesFromPom(artifactResyncSource);
        }

        private void CopyArtifactImpl(
            Artifact.Artifact artifact, 
            ArtifactResyncSource artifactResyncSource)
        {
            EnsureInitialized();

            bool isSnapshot = ArtifactUtils.IsSnapshot(artifact);
            bool resyncFromRemoteRepo = artifactResyncSource == ArtifactResyncSource.RemoteRepository;

            if (!ArtifactUtils.Exists(artifact) || (isSnapshot && resyncFromRemoteRepo))
            {
                if (!ArtifactUtils.DownloadFromRemoteRepository(artifact))
                {
                    RaiseError("Unable to get the artifact {0} from any of your repositories.", artifact.ArtifactId);
                    return;
                }
            }

            CopyToReferenceFolder(artifact, referenceFolder);
        }

        static string CopyToReferenceFolder(Artifact.Artifact artifact, string referenceFolder)
        {
            string artifactReferenceFilePath = ArtifactUtils.GetArtifactReferenceFilePath(artifact, referenceFolder);

            bool overwriteReferenceFile;
            DateTime localRepoArtifactTimestamp = ArtifactUtils.GetArtifactTimestamp(artifact);
            if (File.Exists(artifactReferenceFilePath))
            {
                DateTime referenceFileTimestamp = new FileInfo(artifactReferenceFilePath).LastWriteTimeUtc;
                overwriteReferenceFile = ArtifactUtils.IsEarlierArtifactTimestamp(
                    referenceFileTimestamp, 
                    localRepoArtifactTimestamp);
            }
            else
            {
                overwriteReferenceFile = true;
            }

            if (overwriteReferenceFile)
            {
                File.Copy(artifact.FileInfo.FullName, artifactReferenceFilePath, true);
                // set the timestamp of the local repo's artifact
                new FileInfo(artifactReferenceFilePath).LastWriteTimeUtc = localRepoArtifactTimestamp;
            }

            return artifactReferenceFilePath;
        }

        void GetReferencesFromPom(ArtifactResyncSource artifactResyncSource)
        {
            Artifact.ArtifactRepository repository = new NPanday.Artifact.ArtifactContext().GetArtifactRepository();
            NPanday.Model.Pom.Model m = NPanday.Utils.PomHelperUtility.ReadPomAsModel(new FileInfo(pomFile));

            if (m.dependencies != null)
            {
                foreach (Dependency d in m.dependencies)
                {
                    // check if intra-project reference and copy artifacts
                    if (!IsIntraProject(m, d) && d.classifier == null)
                    {
                        Artifact.Artifact artifact = repository.GetArtifact(d);
                        CopyArtifactImpl(artifact, artifactResyncSource);
                    }
                }
            }
        }

        private void EnsureInitialized()
        {
            if (!initialized)
            {
                throw new InvalidOperationException("Reference manager not initialized.");
            }
        }

        private void RaiseError(string format, params object[] args)
        {
            ReferenceErrorEventArgs e = new ReferenceErrorEventArgs();
            e.Message = string.Format(format, args);
            RaiseError(e);
        }

        private void RaiseError(ReferenceErrorEventArgs e)
        {
            EventHandler<ReferenceErrorEventArgs> handler = OnError;
            if (handler != null)
            {
                handler(this, e);
            }
        }

        private bool IsIntraProject(NPanday.Model.Pom.Model m, Dependency d)
        {
            string pomGroupId = (m.parent != null) ? m.parent.groupId : m.groupId;
            if (d.groupId == pomGroupId)
            {
                // loop through VS projects (instead of modules in parent POM) because
                // we need real-time list of project names in the solution
                foreach (Project project in solution.Projects)
                {
                    if (d.artifactId == project.Name)
                    {
                        return true;
                    }
                }
            }

            return false;
        }

        private bool pomExist()
        {
            return File.Exists(pomFile);
        }

        void CreateReferenceFolder()
        {
            DirectoryInfo di = new DirectoryInfo(referenceFolder);
            if (!di.Exists)
            {
                di.Create();
                di.Attributes = FileAttributes.Hidden | FileAttributes.Directory;
            }
        }

        #endregion


        #region IReferenceManager Members

        public event EventHandler<ReferenceErrorEventArgs> OnError;

        #endregion
    }
}
