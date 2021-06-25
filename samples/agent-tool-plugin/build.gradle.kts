
plugins {
    id ("com.github.rodm.teamcity-agent")
    id ("com.github.rodm.teamcity-server")
    id ("com.github.rodm.teamcity-environments")
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

extra["teamcityVersion"] = "2019.1"
extra["downloadsDir"] = project.findProperty("downloads.dir") ?: "${rootDir}/downloads"
extra["serversDir"] = project.findProperty("servers.dir") ?: "${rootDir}/servers"
extra["java8Home"] = project.findProperty("java8.home") ?: "/opt/jdk1.8.0_92"

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
    version = extra["teamcityVersion"] as String

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
        baseHomeDir = extra["serversDir"] as String
        baseDataDir = "data"

        register("teamcity2019.1") {
            version = "2019.1.5"
            javaHome = extra["java8Home"] as String
        }

        register("teamcity2020.1") {
            version = "2020.1"
            javaHome = extra["java8Home"] as String
        }
    }
}
