
plugins {
    id ("org.gradle.java")
    id ("com.github.rodm.teamcity-server") version "1.4-beta-2"
    id ("com.github.rodm.teamcity-environments") version "1.4-beta-2"
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

extra["teamcityVersion"] = "2019.1"
extra["downloadsDir"] = project.findProperty("downloads.dir") ?: "${rootDir}/downloads"
extra["serversDir"] = project.findProperty("servers.dir") ?: "${rootDir}/servers"
extra["java8Home"] = project.findProperty("java8.home") ?: "/opt/jdk1.8.0_92"

dependencies {
    agent (project(path = ":agent", configuration = "plugin"))
}

teamcity {
    version = extra["teamcityVersion"] as String

    server {
        descriptor {
            name = project.name
            displayName = project.name
            version = project.version as String
            vendorName = "vendor name"
            vendorUrl = "http://example.com"
            description = "TeamCity Example Build Feature Plugin"
            downloadUrl = "https://github.com/rodm/gradle-teamcity-plugin/"
            email = "rod.n.mackenzie@gmail.com"
            useSeparateClassloader = true
        }
    }

    environments {
        downloadsDir = extra["downloadsDir"] as String
        baseHomeDir = extra["serversDir"] as String
        baseDataDir = "data"

        register("teamcity2019.1") {
            version = "2019.1.5"
            javaHome = extra["java8Home"] as String
            serverOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
            agentOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006")
        }

        register("teamcity2020.1") {
            version = "2020.1"
            javaHome = extra["java8Home"] as String
        }
    }
}
