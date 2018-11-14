
import com.github.rodm.teamcity.TeamCityEnvironment
import com.github.rodm.teamcity.TeamCityPluginExtension

plugins {
    kotlin("jvm")
}

apply {
    plugin("com.github.rodm.teamcity-server")
    plugin("com.github.rodm.teamcity-environments")
}

extra["downloadsDir"] = project.findProperty("downloads.dir") ?: "${rootDir}/downloads"
extra["serversDir"] = project.findProperty("servers.dir") ?: "${rootDir}/servers"
extra["java7Home"] = project.findProperty("java7.home") ?: "/opt/jdk1.7.0_80"
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

        "teamcity9" {
            version = "9.1.7"
            javaHome = file(extra["java7Home"] as String)
        }

        "teamcity10" {
            version = "10.0.5"
            javaHome = file(extra["java8Home"] as String)
        }

        "teamcity2017" {
            version = "2017.1"
            javaHome = file(extra["java8Home"] as String)
        }
    }
}

// Extension function to allow cleaner configuration
fun Project.teamcity(configuration: TeamCityPluginExtension.() -> Unit) {
    configure(configuration)
}
