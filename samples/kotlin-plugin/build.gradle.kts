
plugins {
    kotlin("jvm") version "1.3.72" apply false
    id ("com.github.rodm.teamcity-server") version "1.2" apply false
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

extra["teamcityVersion"] = findProperty("teamcity.version") ?: "2019.1"
