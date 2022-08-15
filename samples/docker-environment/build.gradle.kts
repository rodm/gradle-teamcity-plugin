
import com.github.rodm.teamcity.DockerTeamCityEnvironment

plugins {
    id ("io.github.rodm.teamcity-environments") version "1.4"
}

val vendorName by extra("rodm")
val teamcityVersion by extra("2020.1")

val java11Home by extra((project.findProperty("java11.home") ?: "/opt/jdk-11.0.2") as String)

val teamcityPlugins by configurations.creating

dependencies {
    teamcityPlugins (project(path = ":plugin:server", configuration = "plugin"))
}

teamcity {
    version = teamcityVersion

    environments {
        register("teamcity2021.2", DockerTeamCityEnvironment::class.java) {
            version = "2021.2.3"
            port = "7111"
//            serverImage = "alt-teamcity-server"
//            agentImage = "alt-teamcity-agent"
            serverName = "tc-server"
            agentName = "tc-agent"
            plugins = configurations["teamcityPlugins"]
            serverOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
            agentOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006")
        }
    }
}
