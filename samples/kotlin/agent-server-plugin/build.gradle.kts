
plugins {
    id ("org.gradle.java")
    id ("io.github.rodm.teamcity-server") version "1.5"
    id ("io.github.rodm.teamcity-environments") version "1.5"
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

val vendorName = "rodm"
val teamcityVersion by extra("2020.1")

val downloadsDir by extra((project.findProperty("downloads.dir") ?: "${rootDir}/downloads") as String)
val serversDir by extra((project.findProperty("servers.dir") ?: "${rootDir}/servers") as String)
val java8Home by extra((project.findProperty("java8.home") ?: "/opt/jdk1.8.0_92") as String)
val java11Home by extra((project.findProperty("java11.home") ?: "/opt/jdk-11.0.2") as String)

dependencies {
    agent (project(path = ":agent", configuration = "plugin"))
}

teamcity {
    version = teamcityVersion

    server {
        descriptor = project.file("teamcity-plugin.xml")
        tokens = mapOf("NAME" to project.name, "VERSION" to project.version, "VENDOR_NAME" to vendorName)
    }

    environments {
        downloadsDir = project.extra["downloadsDir"] as String
        baseHomeDir = serversDir
        baseDataDir = "data"

        register("teamcity2020.1") {
            version = "2020.1.5"
            javaHome = java8Home
            serverOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
            agentOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006")
        }

        register("teamcity2022.04") {
            version = "2022.04.3"
            javaHome = java11Home
            serverOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
            agentOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006")
        }
    }
}
