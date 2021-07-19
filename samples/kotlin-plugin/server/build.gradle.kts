
import com.github.rodm.teamcity.TeamCityEnvironment

plugins {
    kotlin("jvm")
    id ("com.github.rodm.teamcity-server")
    id ("com.github.rodm.teamcity-environments")
}

val downloadsDir by extra((project.findProperty("downloads.dir") ?: "${rootDir}/downloads") as String)
val serversDir by extra((project.findProperty("servers.dir") ?: "${rootDir}/servers") as String)
val java8Home by extra((project.findProperty("java8.home") ?: "/opt/jdk1.8.0_92") as String)
val java11Home by extra((project.findProperty("java11.home") ?: "/opt/jdk-11.0.2") as String)

dependencies {
    implementation (project(":common"))
    implementation (kotlin("stdlib"))
    agent (project(path = ":agent", configuration = "plugin"))
}

teamcity {
    version = rootProject.extra["teamcityVersion"] as String

    server {
        descriptor {
            name = "Example TeamCity Plugin"
            displayName = "Example TeamCity Plugin"
            version = rootProject.version as String?
            vendorName = "rodm"
            vendorUrl = "https://example.com"
            description = "Example multi-project TeamCity plugin"
            email = "rod.n.mackenzie@gmail.com"
            useSeparateClassloader = true
        }
    }

    environments {
        downloadsDir = extra["downloadsDir"] as String
        baseHomeDir = serversDir
        baseDataDir = "${rootDir}/data"

        operator fun String.invoke(block: TeamCityEnvironment.() -> Unit) = environments.create(this, block)

        "teamcity2020.1" {
            version = "2020.1.5"
            javaHome = java8Home
            serverOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
            agentOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006")
        }

        create("teamcity2020.2") {
            version = "2020.2.4"
            javaHome = java8Home
        }

        register("teamcity2021.1") {
            version = "2021.1.1"
            javaHome = java11Home
        }
    }
}
