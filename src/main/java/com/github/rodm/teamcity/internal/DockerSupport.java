/*
 * Copyright 2022 the original author or authors.
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
package com.github.rodm.teamcity.internal;

import java.util.Arrays;
import java.util.Optional;

public class DockerSupport {

    public static Optional<String> getDebugPort(String options) {
        return Arrays.stream(options.split(" "))
            .filter(option -> option.contains("jdwp"))
            .flatMap(debugOption -> Arrays.stream(debugOption.split(",")))
            .filter(debugParameter -> debugParameter.contains("address="))
            .map(debugParameter -> debugParameter.replace("address=", ""))
            .map(debugParameterValue -> debugParameterValue.replaceAll("^.*:", ""))
            .findAny();
    }

    private DockerSupport() {
        throw new IllegalStateException("Utility class");
    }
}
