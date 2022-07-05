
plugins {
    id ("io.github.rodm.teamcity-agent")
    id ("io.github.rodm.teamcity-server")
    id ("io.github.rodm.teamcity-environments")
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

val teamcityVersion by extra("2020.1")
val downloadsDir by extra((project.findProperty("downloads.dir") ?: "${rootDir}/downloads") as String)
val serversDir by extra((project.findProperty("servers.dir") ?: "${rootDir}/servers") as String)
val java11Home by extra((project.findProperty("java11.home") ?: "/opt/jdk-11.0.2") as String)

repositories {
    mavenCentral()
}

val maven by configurations.creating

dependencies {
    maven (group = "org.apache.maven", name = "apache-maven", version = "3.5.0", classifier = "bin", ext = "zip")
}

tasks {
    agentPlugin {
        archiveBaseName.set("maven3_5")
        archiveVersion.set("")
    }

    serverPlugin {
        archiveBaseName.set("maven3_5-tool")
        archiveVersion.set("")
    }
}

teamcity {
    version = teamcityVersion

    agent {
        descriptor {
            toolDeployment {
                executableFiles {
                    include ("bin/mvn")
                }
            }
        }

        files {
            from(zipTree(configurations["maven"].singleFile)) {
                includeEmptyDirs = false
                eachFile {
                    path = path.split(Regex.fromLiteral("/"), 2)[1]
                }
            }
        }
    }

    server {
        descriptor {
            name = "maven3_5-tool"
            displayName = "Repacked Maven 3.5"
            version = project.version as String
            vendorName = "rodm"
            vendorUrl = "http://example.com"
            description = "TeamCity Example Tool Plugin - Repacked Maven 3.5"
            downloadUrl = "https://github.com/rodm/gradle-teamcity-plugin/"
            email = "rod.n.mackenzie@gmail.com"
        }
    }

    environments {
        downloadsDir = extra["downloadsDir"] as String
        baseHomeDir = serversDir
        baseDataDir = "data"

        register("teamcity2022.04") {
            version = "2022.04.1"
            javaHome = java11Home
        }
    }
}
