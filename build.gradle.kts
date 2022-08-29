
plugins {
    id ("org.gradle.groovy")
    id ("org.gradle.java-gradle-plugin")
    id ("org.gradle.jacoco")
    id ("org.gradle.maven-publish")
    id ("com.gradle.plugin-publish") version "1.0.0"
    id ("org.jetbrains.kotlin.jvm") version "1.4.21"
    id ("org.sonarqube") version "3.4.0.2513"
}

version = "1.5"
group = "io.github.rodm"

repositories {
    mavenCentral()
}

val functional by sourceSets.creating
val samples by sourceSets.creating

configurations["functionalImplementation"].extendsFrom(configurations["testImplementation"])
configurations["samplesImplementation"].extendsFrom(configurations["testImplementation"])

dependencies {
    compileOnly (libs.bundles.publishing)
    compileOnly (libs.bundles.docker)

    implementation (libs.download.task)

    testImplementation (libs.bundles.testing)
    testImplementation (libs.bundles.publishing)
    testImplementation (libs.bundles.docker)

    "functionalImplementation" (project)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

jacoco {
    toolVersion = "0.8.7"
}

gradlePlugin {
    testSourceSets (functional, samples)

    plugins {
        create("teamcityBasePlugin") {
            id = "io.github.rodm.teamcity-base"
            displayName = "Gradle TeamCity base plugin"
            implementationClass = "com.github.rodm.teamcity.TeamCityBasePlugin"
        }
        create("teamcityServerPlugin") {
            id = "io.github.rodm.teamcity-server"
            displayName = "Gradle TeamCity Server plugin"
            implementationClass = "com.github.rodm.teamcity.TeamCityServerPlugin"
        }
        create("teamcityAgentPlugin") {
            id = "io.github.rodm.teamcity-agent"
            displayName = "Gradle TeamCity Agent plugin"
            implementationClass = "com.github.rodm.teamcity.TeamCityAgentPlugin"
        }
        create("teamcityCommonPlugin") {
            id = "io.github.rodm.teamcity-common"
            displayName = "Gradle TeamCity Common API plugin"
            implementationClass = "com.github.rodm.teamcity.TeamCityCommonPlugin"
        }
        create("teamcityEnvironmentsPlugin") {
            id = "io.github.rodm.teamcity-environments"
            displayName = "Gradle TeamCity Environments plugin"
            implementationClass = "com.github.rodm.teamcity.TeamCityEnvironmentsPlugin"
        }

        create("deprecatedServerPlugin") {
            id = "com.github.rodm.teamcity-server"
            displayName = "Gradle TeamCity Server plugin"
            implementationClass = "com.github.rodm.teamcity.DeprecatedTeamCityServerPlugin"
        }
        create("deprecatedAgentPlugin") {
            id = "com.github.rodm.teamcity-agent"
            displayName = "Gradle TeamCity Agent plugin"
            implementationClass = "com.github.rodm.teamcity.DeprecatedTeamCityAgentPlugin"
        }
        create("deprecatedCommonPlugin") {
            id = "com.github.rodm.teamcity-common"
            displayName = "Gradle TeamCity Common API plugin"
            implementationClass = "com.github.rodm.teamcity.DeprecatedTeamCityCommonPlugin"
        }
        create("deprecatedEnvironmentsPlugin") {
            id = "com.github.rodm.teamcity-environments"
            displayName = "Gradle TeamCity Environments plugin"
            implementationClass = "com.github.rodm.teamcity.DeprecatedTeamCityEnvironmentsPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("plugin") {
            from (components["java"])
        }
    }
}

pluginBundle {
    website = "https://github.com/rodm/gradle-teamcity-plugin"
    vcsUrl = "https://github.com/rodm/gradle-teamcity-plugin"
    description = "Gradle plugin for developing TeamCity plugins"
    tags = listOf("teamcity")
}

tasks {
    test {
        useJUnitPlatform()
        finalizedBy (jacocoTestReport)
        systemProperty ("plugin.structure.version", libs.versions.plugin.structure.get())
        systemProperty ("plugin.signer.version", libs.versions.plugin.signer.get())
        systemProperty ("plugin.client.version", libs.versions.plugin.client.get())
        systemProperty ("docker.version", libs.versions.docker.get())
    }

    jacocoTestReport {
        reports {
            xml.required.set(true)
        }
    }

    register<Test>("functionalTest") {
        description = "Runs the functional tests."
        group = "verification"
        useJUnitPlatform()
        testClassesDirs = functional.output.classesDirs
        classpath = functional.runtimeClasspath
    }

    register<Test>("samplesTest") {
        description = "Runs the sample builds."
        group = "verification"
        useJUnitPlatform()
        testClassesDirs = samples.output.classesDirs
        classpath = samples.runtimeClasspath
    }

    withType<Test> {
        systemProperty ("junit.jupiter.tempdir.cleanup.mode.default", "ON_SUCCESS")
    }
}
