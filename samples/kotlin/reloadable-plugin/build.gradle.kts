
import java.time.LocalDateTime

plugins {
    id ("java")
    id ("io.github.rodm.teamcity-server") version "1.5"
    id ("io.github.rodm.teamcity-environments") version "1.5"
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

val downloadsDir by extra((project.findProperty("downloads.dir") ?: "${rootDir}/downloads") as String)
val serversDir by extra((project.findProperty("servers.dir") ?: "${rootDir}/servers") as String)
val java11Home by extra((project.findProperty("java11.home") ?: "/opt/jdk-11.0.2") as String)

tasks {
    test {
        useTestNG()
    }
}

teamcity {
    version = "2018.2"

    server {
        descriptor {
            name = project.name
            displayName = project.name
            version = project.version as String
            vendorName = "rodm"
            vendorUrl = "http://example.com"
            description = "TeamCity Example Server Plugin"

            downloadUrl = "https://github.com/rodm/gradle-teamcity-plugin/"
            email = "rod.n.mackenzie@gmail.com"

            useSeparateClassloader = true
            allowRuntimeReload = true

            parameters {
                parameter ("build-time", "${LocalDateTime.now()}")
            }
        }
    }

    environments {
        downloadsDir = extra["downloadsDir"] as String
        baseHomeDir = serversDir
        baseDataDir = "data"

        register("teamcity2022.04") {
            version = "2022.04.3"
            javaHome = java11Home
//            serverOptions ("") // uncomment to disable super user token
        }
    }
}
