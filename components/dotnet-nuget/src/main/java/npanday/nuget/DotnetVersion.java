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

package npanday.nuget;

import org.codehaus.plexus.util.StringUtils;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A four-digit .NET version.
 *
 * @author <a href="me@lcorneliussen.de">Lars Corneliussen, Faktum Software</a>
 */
@Immutable
public final class DotnetVersion
    implements Comparable<DotnetVersion>
{

    /**
     * {@link DotnetVersion} element. From most meaningful to less meaningful.
     */
    public enum Element
    {
        MAJOR, MINOR, PATCH, BUILD;
    }

    private static final String FORMAT = "(\\d+)\\.(\\d+)(?:\\.)?(\\d*)(?:\\.)?(\\d*)";

    private static final Pattern PATTERN = Pattern.compile( DotnetVersion.FORMAT );

    private final int major;

    private final int minor;

    private final int patch;

    private final int build;

    public DotnetVersion( @Nonnegative final int major, @Nonnegative final int minor, @Nonnegative final int patch )
    {
        this( major, minor, patch, 0 );
    }

    public DotnetVersion(
        @Nonnegative final int major, @Nonnegative final int minor, @Nonnegative final int patch,
        @Nonnegative final int build )
    {
        if ( major < 0 )
        {
            throw new IllegalArgumentException( Element.MAJOR + " must be positive" );
        }
        if ( minor < 0 )
        {
            throw new IllegalArgumentException( Element.MINOR + " must be positive" );
        }
        if ( patch < 0 )
        {
            throw new IllegalArgumentException( Element.PATCH + " must be positive" );
        }
        if ( build < 0 )
        {
            throw new IllegalArgumentException( Element.BUILD + " must be positive" );
        }

        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.build = build;
    }

    /**
     * Creates a version from a string representation. Must match version#FORMAT.
     *
     * @param version
     * @return
     */
    public static DotnetVersion parse( @Nonnull final String version )
    {
        final Matcher matcher = DotnetVersion.PATTERN.matcher( version );
        if ( !matcher.matches() )
        {
            throw new IllegalArgumentException( "<" + version + "> does not match format " + DotnetVersion.FORMAT );
        }

        final int major = Integer.valueOf( matcher.group( 1 ) );
        final int minor = Integer.valueOf( matcher.group( 2 ) );
        final int patch;
        final String patchMatch = matcher.group( 3 );
        if ( StringUtils.isNotEmpty( patchMatch ) )
        {
            patch = Integer.valueOf( patchMatch );
        }
        else
        {
            patch = 0;
        }
        final int build;
        final String buildMatch = matcher.group( 4 );
        if ( StringUtils.isNotEmpty( buildMatch ) )
        {
            build = Integer.valueOf( buildMatch );
        }
        else
        {
            build = 0;
        }
        return new DotnetVersion( major, minor, patch, build );
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 43 * hash + this.major;
        hash = 43 * hash + this.minor;
        hash = 43 * hash + this.patch;
        hash = 43 * hash + this.build;
        return hash;
    }

    @Override
    public boolean equals( @Nullable final Object object )
    {
        if ( !( object instanceof DotnetVersion ) )
        {
            return false;
        }

        final DotnetVersion other = (DotnetVersion) object;
        if ( other.major != this.major || other.minor != this.minor || other.patch != this.patch
            || other.build != this.build )
        {
            return false;
        }
        return true;
    }

    public int compareTo( final DotnetVersion other )
    {
        if ( equals( other ) )
        {
            return 0;
        }

        if ( this.major < other.major )
        {
            return -1;
        }
        else if ( this.major == other.major )
        {
            if ( this.minor < other.minor )
            {
                return -1;
            }
            else if ( this.minor == other.minor )
            {
                if ( this.patch < other.patch )
                {
                    return -1;
                }
                else if ( this.patch == other.patch )
                {
                    if ( this.build < other.build )
                    {
                        return -1;
                    }
                }
            }
        }
        return 1;
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append( this.major );
        builder.append( "." ).append( this.minor );
        builder.append( "." ).append( this.patch );
        builder.append(".").append( this.build );

        return builder.toString();
    }

    public int getMajor()
    {
        return major;
    }

    public int getMinor()
    {
        return minor;
    }

    public int getPatch()
    {
        return patch;
    }

    public int getBuild()
    {
        return build;
    }
}