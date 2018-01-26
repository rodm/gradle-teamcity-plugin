
buildscript {
    repositories {
        mavenLocal()
        maven { setUrl("https://repo.gradle.org/gradle/repo") }
        maven { setUrl("https://plugins.gradle.org/m2/") }
        mavenCentral()
    }

    dependencies {
        classpath("com.github.rodm:gradle-teamcity-plugin:1.1-SNAPSHOT")
    }
}

plugins {
    kotlin("jvm") version "1.2.0" apply false
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

extra["teamcityVersion"] = findProperty("teamcity.version") ?: "9.1"
