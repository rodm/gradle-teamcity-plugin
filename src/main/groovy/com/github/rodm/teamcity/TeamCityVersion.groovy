/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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

import static groovy.transform.TypeCheckingMode.SKIP

@CompileStatic
class TeamCityVersion implements Comparable<TeamCityVersion> {

    public static final TeamCityVersion VERSION_9_0
    public static final TeamCityVersion VERSION_2018_2
    public static final TeamCityVersion VERSION_2020_1
    static {
        VERSION_9_0 = version('9.0')
        VERSION_2018_2 = version('2018.2')
        VERSION_2020_1 = version('2020.1')
    }

    private static final Pattern RELEASE_VERSION_PATTERN = Pattern.compile('((\\d+)(\\.\\d+)+)')
    private static final Pattern SNAPSHOT_VERSION_PATTERN = Pattern.compile('((\\d+)(\\.\\d+)+)-SNAPSHOT')

    static final String INVALID_RELEASE_MESSAGE = "'%s' is not a valid TeamCity version string (examples: '9.0', '10.0.5', '2018.1')"
    static final String INVALID_SNAPSHOT_MESSAGE = "'%s' is not a valid TeamCity version string (examples: '10.0-SNAPSHOT', '2021.1' '2021.2.1-SNAPSHOT')"

    static TeamCityVersion version(String version) throws IllegalArgumentException {
        return new TeamCityVersion(version, false)
    }

    static TeamCityVersion version(String version, boolean allowSnapshots) throws IllegalArgumentException {
        return new TeamCityVersion(version, allowSnapshots)
    }

    private final String version

    private TeamCityVersion(String version, boolean allowSnapshots) throws IllegalArgumentException {
        if (version != 'SNAPSHOT') {
            Matcher releaseMatcher = RELEASE_VERSION_PATTERN.matcher(version)
            if (allowSnapshots) {
                Matcher snapshotMatcher = SNAPSHOT_VERSION_PATTERN.matcher(version)
                if (!snapshotMatcher.matches() && !releaseMatcher.matches()) {
                    throw new IllegalArgumentException(String.format(INVALID_SNAPSHOT_MESSAGE, version))
                }
            } else {
                if (!releaseMatcher.matches()) {
                    throw new IllegalArgumentException(String.format(INVALID_RELEASE_MESSAGE, version))
                }
            }
        }
        this.version = version
    }

    String toString() {
        return 'TeamCity ' + this.version
    }

    int compareTo(TeamCityVersion teamcityVersion) {
        if (version == 'SNAPSHOT' && teamcityVersion.version != 'SNAPSHOT') {
            return 1
        } else if (teamcityVersion.version == 'SNAPSHOT' && version != 'SNAPSHOT') {
            return -1
        } else if (version == teamcityVersion.version) {
            return 0
        }
        String[] versionParts = version.split('[.|-]')
        String[] otherVersionParts = teamcityVersion.version.split('[.|-]')

        for (int diff = 0; diff < versionParts.length && diff < otherVersionParts.length; ++diff) {
            if (versionParts[diff] == 'SNAPSHOT') continue
            if (otherVersionParts[diff] == 'SNAPSHOT') continue
            int part = Integer.parseInt(versionParts[diff])
            int otherPart = Integer.parseInt(otherVersionParts[diff])
            if (part > otherPart) {
                return 1
            }
            if (otherPart > part) {
                return -1
            }
        }
        return (versionParts.length > otherVersionParts.length) ? 1 : -1
    }

    boolean equals(Object o) {
        return o == this
    }

    int hashCode() {
        return version.hashCode()
    }

    @CompileStatic(SKIP)
    String getDataVersion() {
        return (version =~ (/(\d+\.\d+).*/))[0][1]
    }
}
