
buildscript {
    extra["kotlinVersion"] = "1.1-M03"

    repositories {
        mavenLocal()
        maven { setUrl("https://repo.gradle.org/gradle/repo") }
        maven { setUrl("https://plugins.gradle.org/m2/") }
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${extra["kotlinVersion"]}")
        classpath("com.github.rodm:gradle-teamcity-plugin:0.12-SNAPSHOT")
    }
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"
