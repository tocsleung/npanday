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

/// Author: Leopoldo Lee Agdeppa III

namespace NPanday.ProjectImporter.Parser.SlnParser
{
    public enum LexanState
    {
        Error_State = -1,
        Start_State = 0,

        // Project chars
        Project_P,
        Project_r,
        Project_o,
        Project_j,
        Project_e,
        Project_c,
        Project_t,

        // ProjectSection chars will start at 'Section' 
        // since 'Project' word is coverd by Project chars
        ProjectSection_S,
        ProjectSection_e,
        ProjectSection_c,
        ProjectSection_t,
        ProjectSection_i,
        ProjectSection_o,
        ProjectSection_n,



        // Global chars
        Global_G,
        Global_l,
        Global_o,
        Global_b,
        Global_a,
        Global_l2,

        // GlobalSection chars will start at 'Section' 
        // since 'Global' word is coverd by Global chars
        GlobalSection_S,
        GlobalSection_e,
        GlobalSection_c,
        GlobalSection_t,
        GlobalSection_i,
        GlobalSection_o,
        GlobalSection_n,

        // End
        End_E,
        End_n,
        End_d,

        // EndProject Chars
        EndProject_P,
        EndProject_r,
        EndProject_o,
        EndProject_j,
        EndProject_e,
        EndProject_c,
        EndProject_t,

        // EndProjectSection charswill start at 'Section' 
        // since 'EndProject' word is coverd by EndProject chars
        EndProjectSection_S,
        EndProjectSection_e,
        EndProjectSection_c,
        EndProjectSection_t,
        EndProjectSection_i,
        EndProjectSection_o,
        EndProjectSection_n,


        // EndGlobal Chars
        EndGlobal_G,
        EndGlobal_l,
        EndGlobal_o,
        EndGlobal_b,
        EndGlobal_a,
        EndGlobal_l2,


        // EndGlobalSection charswill start at 'Section' 
        // since 'EndGlobal' word is coverd by EndGlobal chars
        EndGlobalSection_S,
        EndGlobalSection_e,
        EndGlobalSection_c,
        EndGlobalSection_t,
        EndGlobalSection_i,
        EndGlobalSection_o,
        EndGlobalSection_n,


        // OTHER TOKENS

        QUOTE_1,
        QUOTE_DATA,
        QUOTE_2,
        COMMA,
        OPEN_PARENTHESIS,
        CLOSE_PARENTHESIS,
        EQUALS,
        STRING_VALUE
    }
}
