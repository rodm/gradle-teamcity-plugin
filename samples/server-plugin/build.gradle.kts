
plugins {
    id ("java")
    id ("io.github.rodm.teamcity-server") version "1.5"
    id ("io.github.rodm.teamcity-environments") version "1.5"
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

val vendorName by extra("rodm")
val teamcityVersion by extra("2020.1")
val downloadsDir by extra((project.findProperty("downloads.dir") ?: "${rootDir}/downloads") as String)
val serversDir by extra((project.findProperty("servers.dir") ?: "${rootDir}/servers") as String)
val java8Home by extra((project.findProperty("java8.home") ?: "/opt/jdk1.8.0_92") as String)
val java11Home by extra((project.findProperty("java11.home") ?: "/opt/jdk-11.0.2") as String)

tasks {
    test {
        useTestNG()
    }
}

teamcity {
    version = teamcityVersion

    server {
        descriptor {
            name = project.name
            displayName = project.name
            version = project.version as String
            vendorName = extra["vendorName"] as String
            vendorUrl = "http://example.com"
            description = "TeamCity Example Server Plugin"

            downloadUrl = "https://github.com/rodm/gradle-teamcity-plugin/"
            email = "rod.n.mackenzie@gmail.com"
            useSeparateClassloader = false

            parameters {
                parameter ("param", "value")
            }
        }
    }

    environments {
        downloadsDir = extra["downloadsDir"] as String
        baseHomeDir = serversDir
        baseDataDir = "${rootDir}/data"

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
