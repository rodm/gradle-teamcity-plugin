
buildscript {
    extra["kotlinVersion"] = "1.1.0"

    repositories {
        mavenLocal()
        maven { setUrl("https://repo.gradle.org/gradle/repo") }
        maven { setUrl("https://plugins.gradle.org/m2/") }
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${extra["kotlinVersion"]}")
        classpath("com.github.rodm:gradle-teamcity-plugin:1.0")
    }
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

extra["teamcityVersion"] = findProperty("teamcity.version") ?: "9.1"
