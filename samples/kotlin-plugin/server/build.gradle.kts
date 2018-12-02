
import com.github.rodm.teamcity.TeamCityEnvironment

plugins {
    kotlin("jvm")
    id ("com.github.rodm.teamcity-server")
    id ("com.github.rodm.teamcity-environments")
}

extra["downloadsDir"] = project.findProperty("downloads.dir") ?: "${rootDir}/downloads"
extra["serversDir"] = project.findProperty("servers.dir") ?: "${rootDir}/servers"
extra["java8Home"] = project.findProperty("java8.home") ?: "/opt/jdk1.8.0_92"

val agent = configurations.getByName("agent")

dependencies {
    implementation (project(":common"))
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
        baseHomeDir = extra["serversDir"] as String
        baseDataDir = "${rootDir}/data"

        operator fun String.invoke(block: TeamCityEnvironment.() -> Unit) {
            environments.create(this, closureOf<TeamCityEnvironment>(block))
        }

        "teamcity2017.1" {
            version = "2017.1.5"
            javaHome = file(extra["java8Home"] as String)
            serverOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
            agentOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006")
        }

        "teamcity2018.1" {
            version = "2018.1.3"
            javaHome = file(extra["java8Home"] as String)
        }
    }
}
