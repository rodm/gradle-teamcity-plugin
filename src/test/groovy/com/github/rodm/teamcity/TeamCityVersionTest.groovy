/*
 * Copyright 2018 Rod MacKenzie
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

import org.gradle.api.GradleException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.github.rodm.teamcity.TeamCityVersion.INVALID_RELEASE_MESSAGE
import static com.github.rodm.teamcity.TeamCityVersion.INVALID_SNAPSHOT_MESSAGE
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_9_0
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.lessThan
import static org.hamcrest.Matchers.greaterThan
import static org.hamcrest.core.StringEndsWith.endsWith
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assertions.fail

class TeamCityVersionTest {

    @Test
    void 'valid TeamCity versions'() {
        version('10.0')
        version('10.0.5')
        version('2017.1.2')
        version('2018.2')
        version('2022.04')
        version('2023.10')
        version('2023.10-12345')
        version('2023.09-23456')
        version('SNAPSHOT')
        version('LOCAL-SNAPSHOT')
    }

    @Test
    void 'invalid TeamCity versions'() {
        assertInvalidVersion('10')
        assertInvalidVersion('10-SNAPSHOT')
        assertInvalidVersion('2018')
        assertInvalidVersion('2018.2-SNAPSHOT')
        assertInvalidVersion('2018.2.1-SNAPSHOT')
    }

    @Test
    void 'compare TeamCity versions'() {
        assertThat(version('9.0'), lessThan(version('9.0.1')))
        assertThat(version('9.0'), lessThan(version('10.0')))
        assertThat(version('9.0'), lessThan(version('2018.1')))
        assertThat(version('2018.1.2'), lessThan(version('2018.1.3')))
        assertThat(version('2018.1.2'), lessThan(version('SNAPSHOT')))
        assertThat(version('2018.1-12345'), lessThan(version('2018.1-23451')))

        assertThat(version('9.0'), equalTo(VERSION_9_0))
        assertThat(version('2018.2'), equalTo(VERSION_2018_2))
        assertThat(version('10.0.5'), equalTo(version('10.0.5')))
        assertThat(version('SNAPSHOT'), equalTo(version('SNAPSHOT')))
        assertThat(version('LOCAL-SNAPSHOT'), equalTo(version('LOCAL-SNAPSHOT')))

        assertThat(version('9.0.1'), greaterThan(version('9.0')))
        assertThat(version('10.0'), greaterThan(version('9.0')))
        assertThat(version('SNAPSHOT'), greaterThan(version('2018.1.2')))
        assertThat(version('SNAPSHOT'), greaterThan(version('2018.1-12345')))
        assertThat(version('LOCAL-SNAPSHOT'), greaterThan(version('2018.1-12345')))
    }

    @Test
    void 'compare TeamCity version less than'() {
        assertTrue(version("9.0.1").lessThan(version("9.0.2")))
        assertFalse(version("9.0.1").lessThan(version("9.0.1")))
        assertFalse(version("9.0.1").lessThan(version("8.1")))
    }

    @Test
    void 'compare TeamCity version equal or greater than'() {
        assertTrue(version("9.0.1").equalOrGreaterThan(version("9.0.1")))
        assertTrue(version("9.0.1").equalOrGreaterThan(version("8.1")))
        assertFalse(version("9.0.1").equalOrGreaterThan(version("9.0.2")))
    }

    @Test
    void 'data version'() {
        assertThat(version('9.0').dataVersion, equalTo('9.0'))
        assertThat(version('9.0.3').dataVersion, equalTo('9.0'))
        assertThat(version('2020.2.4').dataVersion, equalTo('2020.2'))
        assertThat(version('2021.1.1').dataVersion, equalTo('2021.1'))
        assertThat(version('2022.04.1').dataVersion, equalTo('2022.04'))
    }

    @Test
    void 'data version cannot be determined for snapshot'() {
        def e =assertThrows(GradleException, () -> {
            version('SNAPSHOT').dataVersion
        })
        assertThat(e.message, equalTo('Invalid version'))
    }

    @Test
    void 'data version cannot be determined for local-snapshot'() {
        def e =assertThrows(GradleException, () -> {
            version('LOCAL-SNAPSHOT').dataVersion
        })
        assertThat(e.message, equalTo('Invalid version'))
    }

    @Nested
    class TeamCityVersionAllowingSnapshots {

        @Test
        void 'valid TeamCity versions allowing snapshots'() {
            version('10.0-SNAPSHOT')
            version('10.0.5-SNAPSHOT')
            version('2020.1.5-SNAPSHOT')
            version('2020.2-SNAPSHOT')
            version('2022.04-SNAPSHOT')
            version('LOCAL-SNAPSHOT')
        }

        @Test
        void 'invalid TeamCity versions'() {
            assertInvalidSnapshotVersion('10')
            assertInvalidSnapshotVersion('10-SNAPSHOT')
            assertInvalidSnapshotVersion('2018')
            assertInvalidSnapshotVersion('A.B-SNAPSHOT')
            assertInvalidSnapshotVersion('A.B.C-SNAPSHOT')
        }

        @Test
        void 'compare TeamCity versions allowing snapshots'() {
            assertThat(version('9.0-SNAPSHOT'), lessThan(version('9.0.1-SNAPSHOT')))
            assertThat(version('9.0-SNAPSHOT'), lessThan(version('10.0-SNAPSHOT')))
            assertThat(version('9.0-SNAPSHOT'), lessThan(version('2018.1-SNAPSHOT')))
            assertThat(version('2020.1.5-SNAPSHOT'), lessThan(version('2020.2-SNAPSHOT')))
            assertThat(version('2020.1.5-SNAPSHOT'), lessThan(version('SNAPSHOT')))

            assertThat(version('2020.1.3'), lessThan(version('2020.1.3-SNAPSHOT')))
            assertThat(version('2020.1.4'), greaterThan(version('2020.1.3-SNAPSHOT')))

            assertThat(version('10.0-SNAPSHOT'), equalTo(version('10.0-SNAPSHOT')))
            assertThat(version('2020.2-SNAPSHOT'), equalTo(version('2020.2-SNAPSHOT')))

            assertThat(version('9.0.1-SNAPSHOT'), greaterThan(version('9.0-SNAPSHOT')))
            assertThat(version('10.0-SNAPSHOT'), greaterThan(version('9.0-SNAPSHOT')))
            assertThat(version('SNAPSHOT'), greaterThan(version('2020.2-SNAPSHOT')))
            assertThat(version('LOCAL-SNAPSHOT'), greaterThan(version('2020.2-SNAPSHOT')))
            assertThat(version('2018.1-SNAPSHOT'), lessThan(version('2018.1-23451')))
            assertThat(version('2019.1-SNAPSHOT'), greaterThan(version('2018.1-23451')))
            assertThat(version('LOCAL-SNAPSHOT'), greaterThan(version('2018.1-23451')))
        }

        @Test
        void 'data version'() {
            assertThat(version('9.0-SNAPSHOT').dataVersion, equalTo('9.0'))
            assertThat(version('9.0.3-SNAPSHOT').dataVersion, equalTo('9.0'))
            assertThat(version('2020.2-SNAPSHOT').dataVersion, equalTo('2020.2'))
            assertThat(version('2020.2.4-SNAPSHOT').dataVersion, equalTo('2020.2'))
        }

        private static assertInvalidSnapshotVersion(String version) {
            assertInvalidVersion(version, true)
        }

        private static TeamCityVersion version(String version) {
            TeamCityVersion.version(version, true)
        }
    }

    private static void assertInvalidVersion(String version) {
        assertInvalidVersion(version, false)
    }

    private static void assertInvalidVersion(String version, boolean allowSnapshots) {
        try {
            TeamCityVersion.version(version, allowSnapshots)
            fail("Should throw exception for invalid version: " + version)
        }
        catch (IllegalArgumentException expected) {
            def expectedMessage = allowSnapshots ? INVALID_SNAPSHOT_MESSAGE : INVALID_RELEASE_MESSAGE
            assertThat(expected.getMessage(), endsWith(String.format(expectedMessage, version)))
        }
    }

    private static TeamCityVersion version(String version) {
        TeamCityVersion.version(version)
    }
}
