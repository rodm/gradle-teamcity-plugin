/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rodm.teamcity

import groovy.transform.CompileStatic

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
class TeamCityVersion implements Comparable<TeamCityVersion> {

    public static final TeamCityVersion VERSION_9_0
    public static final TeamCityVersion VERSION_2018_2
    static {
        VERSION_9_0 = version('9.0')
        VERSION_2018_2 = version('2018.2')
    }

    private static final Pattern VERSION_PATTERN = Pattern.compile('((\\d+)(\\.\\d+)+)')

    private final String version

    static TeamCityVersion version(String version) throws IllegalArgumentException {
        return new TeamCityVersion(version)
    }

    private TeamCityVersion(String version) {
        this.version = version
        Matcher matcher = VERSION_PATTERN.matcher(version)
        if ('2018.2-SNAPSHOT' == version) return
        if (!matcher.matches() && 'SNAPSHOT' != version) {
            throw new IllegalArgumentException("'${version}' is not a valid TeamCity version string (examples: '9.0', '10.0.5', '2018.1')")
        }
    }

    String toString() {
        return 'TeamCity ' + this.version
    }

    int compareTo(TeamCityVersion teamcityVersion) {
        if (version == '2018.2-SNAPSHOT' && teamcityVersion.version != '2018.2-SNAPSHOT') {
            return 1
        } else if (teamcityVersion.version == '2018.2-SNAPSHOT' && version != '2018.2-SNAPSHOT') {
            return -1
        } else if (version == teamcityVersion.version) {
            return 0
        }

        if (version == 'SNAPSHOT' && teamcityVersion.version != 'SNAPSHOT') {
            return 1
        } else if (teamcityVersion.version == 'SNAPSHOT' && version != 'SNAPSHOT') {
            return -1
        } else if (version == teamcityVersion.version) {
            return 0
        }
        String[] versionParts = version.split('\\.')
        String[] otherVersionParts = teamcityVersion.version.split('\\.')

        for (int diff = 0; diff < versionParts.length && diff < otherVersionParts.length; ++diff) {
            int part = Integer.parseInt(versionParts[diff])
            int otherPart = Integer.parseInt(otherVersionParts[diff])
            if (part > otherPart) {
                return 1
            }
            if (otherPart > part) {
                return -1
            }
        }

        if (versionParts.length > otherVersionParts.length) {
            return 1
        } else if (versionParts.length < otherVersionParts.length) {
            return -1
        }
        return 0
    }

    boolean equals(Object o) {
        if (o == this) {
            return true
        } else if (o != null && o.getClass() == this.getClass()) {
            TeamCityVersion other = (TeamCityVersion) o
            return version == other.version
        } else {
            return false
        }
    }

    int hashCode() {
        return version.hashCode()
    }
}
