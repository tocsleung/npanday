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

package npanday.executable.execution;

/**
 * Interface for the various implementations of commandline argument quoting.
 * <i>Workaround for https://jira.codehaus.org/browse/PLXUTILS-147</i>
 *
 * @author <a href="mailto:lcorneliussen@apache.org">Lars Corneliussen</a>
 */
public interface ArgumentQuotingStrategy
{
    /**
     * Possibility to intercept the quoting mechanisms.
     *
     * @param source the unescaped/unquoted argument
     * @return the escaped and quoted argument
     */
    String quoteAndEscape(
        String source, char quoteChar, final char[] escapedChars, final char[] quotingTriggers, char escapeChar,
        boolean force );
}
