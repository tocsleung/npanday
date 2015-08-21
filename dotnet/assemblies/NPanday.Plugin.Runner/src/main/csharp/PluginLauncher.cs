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
using System.Reflection;
using System.Text;
using System.Diagnostics;

namespace NPanday.Plugin.Launcher
{
	/// <summary>
	/// Description of PluginLauncher.
	/// </summary>
	public class PluginLauncher
	{
		public PluginLauncher()
		{
		}

		[STAThread]
		static int Main(string[] args)
        {
            Console.WriteLine("NPanday: Start Process = " + DateTime.Now);
            Console.WriteLine(@flattenArgs(args));
            String vendor = GetArgFor("vendor", args);
            String startProcessAssembly = @GetArgFor("startProcessAssembly", args);
            ProcessStartInfo processStartInfo = null;

            if (vendor != null && vendor.Equals("MONO"))
            {
                processStartInfo =
                    new ProcessStartInfo("mono", startProcessAssembly + " " + @flattenArgs(args));
            }
            else
            {
                processStartInfo =
                    new ProcessStartInfo(startProcessAssembly, @flattenArgs(args));
            }

            processStartInfo.UseShellExecute = false;
            Process p = Process.Start(processStartInfo);
            p.WaitForExit();
            Console.WriteLine("NPanday: End Process = " + DateTime.Now + "; exit code = " + p.ExitCode);
                
            return p.ExitCode;
        }

		private static string GetArgFor(string name, string[] args)
		{
			char[] delim = {'='};
			foreach(string arg in args)
			{
                string[] tokens = arg.Split(delim);
                if (tokens[0].Equals(name)) return tokens[1];
			}
            return null;
		}
		
		private static string flattenArgs(string[] args)
		{
			StringBuilder stringBuilder = new StringBuilder();
			foreach(string arg in args)
			{
				//Console.WriteLine("ARG {0}: ", arg);
				stringBuilder.Append(@"""").Append(@arg).Append(@"""").Append(" ");
			}
			return stringBuilder.ToString();
		}
	}
}
