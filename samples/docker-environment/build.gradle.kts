
plugins {
    id ("com.github.rodm.teamcity-environments") version "1.4"
}

val vendorName by extra("rodm")
val teamcityVersion by extra("2020.1")

val java11Home by extra((project.findProperty("java11.home") ?: "/opt/jdk-11.0.2") as String)

val teamcityPlugins by configurations.creating

dependencies {
    teamcityPlugins (project(path = ":plugin", configuration = "plugin"))
}

teamcity {
    version = teamcityVersion

    environments {
        register("teamcity2020.1") {
            version = "2021.2.3"
            useDocker {
//                serverImage = "alt-teamcity-server"
//                agentImage = "alt-teamcity-agent"
                serverName = "tc-server"
                agentName = "tc-agent"
            }
            plugins = configurations["teamcityPlugins"]
            serverOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
            agentOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006")
        }
    }
}
