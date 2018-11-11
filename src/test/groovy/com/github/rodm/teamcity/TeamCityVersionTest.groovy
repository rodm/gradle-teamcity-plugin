/*
 * Copyright 2018 Rod MacKenzie
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

import org.junit.Test

import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_9_0
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.lessThan
import static org.hamcrest.Matchers.greaterThan
import static org.hamcrest.core.StringEndsWith.endsWith
import static org.junit.Assert.fail

class TeamCityVersionTest {

    @Test
    public void 'valid TeamCity versions'() {
        version('10.0')
        version('10.0.5')
        version('2017.1.2')
        version('2018.2')
        version('SNAPSHOT')
    }

    @Test
    void 'invalid TeamCity versions'() {
        assertInvalidVersion('10')
        assertInvalidVersion('10-SNAPSHOT')
        assertInvalidVersion('2018')
//        assertInvalidVersion('2018.2-SNAPSHOT')
        assertInvalidVersion('2018.2.1-SNAPSHOT')
    }

    @Test
    void 'compare TeamCity versions'() {
        assertThat(version('9.0'), lessThan(version('9.0.1')))
        assertThat(version('9.0'), lessThan(version('10.0')))
        assertThat(version('9.0'), lessThan(version('2018.1')))
        assertThat(version('2018.1.2'), lessThan(version('2018.1.3')))
        assertThat(version('2018.1.2'), lessThan(version('SNAPSHOT')))

        assertThat(version('9.0'), equalTo(VERSION_9_0))
        assertThat(version('2018.2'), equalTo(VERSION_2018_2))
        assertThat(version('10.0.5'), equalTo(version('10.0.5')))
        assertThat(version('SNAPSHOT'), equalTo(version('SNAPSHOT')))

        assertThat(version('SNAPSHOT'), greaterThan(version('2018.1.2')))
    }

    private static void assertInvalidVersion(String version) {
        try {
            TeamCityVersionTest.version(version)
            fail("Should throw exception for invalid version: " + version)
        }
        catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), endsWith("is not a valid TeamCity version string (examples: '9.0', '10.0.5', '2018.1')"))
        }
    }

    private static TeamCityVersion version(String version) {
        TeamCityVersion.version(version)
    }
}
