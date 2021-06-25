
plugins {
    id ("java")
    id ("com.github.rodm.teamcity-server") version "1.4-beta-2"
    id ("com.github.rodm.teamcity-environments") version "1.4-beta-2"
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

extra["vendorName"] = "rodm"
extra["teamcityVersion"] = "2020.2"

extra["downloadsDir"] = project.findProperty("downloads.dir") ?: "$rootDir/downloads"
extra["serversDir"] = project.findProperty("servers.dir") ?: "$rootDir/servers"
extra["java8Home"] = project.findProperty("java8.home") ?: "/opt/jdk1.8.0_92"

tasks {
    val dockerCommandLine = arrayOf("docker", "run", "--rm",
        "--name", "npmbuild",
        "-v", "${projectDir}:/project",
        "-w", "/project", "node:lts")

    register<Exec>("npmInstall") {
        commandLine = listOf(*dockerCommandLine, "npm", "install")
    }

    register<Exec>("webpack") {
        inputs.dir("src/main/js")
        inputs.file("package-lock.json")
        inputs.file("webpack.config.js")
        outputs.dir("$buildDir/js")
        commandLine = listOf(*dockerCommandLine, "npm", "run", "build")
    }
}

teamcity {
    version = extra["teamcityVersion"] as String

    server {
        descriptor {
            name = project.name
            displayName = project.name
            version = project.version as String
            vendorName = extra["vendorName"] as String
            vendorUrl = "http://example.com"
            description = "Example Sakura UI Plugin"

            downloadUrl = "https://github.com/rodm/gradle-teamcity-plugin/"
            email = "rod.n.mackenzie@gmail.com"
            useSeparateClassloader = true
        }

        web {
            tasks.named("webpack")
        }
    }

    environments {
        downloadsDir = extra["downloadsDir"] as String
        baseHomeDir = extra["serversDir"] as String
        baseDataDir = "${rootDir}/data"

        register("teamcity2020.2") {
            version = "2020.2.1"
            javaHome = extra["java8Home"] as String
            serverOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
            agentOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006")
        }
    }
}
