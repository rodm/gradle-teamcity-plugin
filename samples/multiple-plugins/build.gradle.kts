
plugins {
    id ("com.github.rodm.teamcity-environments") version "1.4-beta-2"
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

extra["vendorName"] = "rodm"
extra["teamcityVersion"] = "2019.1"

extra["downloadsDir"] = project.findProperty("downloads.dir") ?: "${rootDir}/downloads"
extra["serversDir"] = project.findProperty("servers.dir") ?: "${rootDir}/servers"
extra["java7Home"] = project.findProperty("java7.home") ?: "/opt/jdk1.7.0_80"
extra["java8Home"] = project.findProperty("java8.home") ?: "/opt/jdk1.8.0_92"

val teamcityPlugins by configurations.creating

dependencies {
    teamcityPlugins (project(path = ":plugin1", configuration = "plugin"))
    teamcityPlugins (project(path = ":plugin2", configuration = "plugin"))
}

teamcity {
    version = extra["teamcityVersion"] as String

    environments {
        downloadsDir = extra["downloadsDir"] as String
        baseHomeDir = extra["serversDir"] as String
        baseDataDir = "data"

        register("teamcity2019.1") {
            version = "2019.1.5"
            javaHome = extra["java8Home"] as String
            plugins.setFrom(configurations["teamcityPlugins"])
        }

        register("teamcity2020.1") {
            version = "2020.1"
            javaHome = extra["java8Home"] as String
            plugins.setFrom(configurations["teamcityPlugins"])
        }
    }
}
