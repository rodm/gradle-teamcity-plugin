
plugins {
    id ("com.github.rodm.teamcity-environments") version "1.4"
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

val vendorName by extra("rodm")
val teamcityVersion by extra("2020.1")

val downloadsDir by extra((project.findProperty("downloads.dir") ?: "${rootDir}/downloads") as String)
val serversDir by extra((project.findProperty("servers.dir") ?: "${rootDir}/servers") as String)
val java8Home by extra((project.findProperty("java8.home") ?: "/opt/jdk1.8.0_92") as String)
val java11Home by extra((project.findProperty("java11.home") ?: "/opt/jdk-11.0.2") as String)

val teamcityPlugins by configurations.creating

dependencies {
    teamcityPlugins (project(path = ":plugin1", configuration = "plugin"))
    teamcityPlugins (project(path = ":plugin2", configuration = "plugin"))
}

teamcity {
    version = teamcityVersion

    environments {
        downloadsDir = extra["downloadsDir"] as String
        baseHomeDir = serversDir
        baseDataDir = "data"

        register("teamcity2020.1") {
            version = "2020.1.5"
            javaHome = java8Home
            plugins = configurations["teamcityPlugins"]
        }

        register("teamcity2021.1") {
            version = "2021.1.1"
            javaHome = java11Home
            plugins = configurations["teamcityPlugins"]
        }
    }
}
