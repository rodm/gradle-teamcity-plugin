
plugins {
    id ("java")
    id ("io.github.rodm.teamcity-server") version "1.5"
    id ("io.github.rodm.teamcity-environments") version "1.5"
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

val vendorName by extra("rodm")
val teamcityVersion by extra("2020.2")

val downloadsDir by extra(project.findProperty("downloads.dir") ?: "${rootDir}/downloads" as String)
val serversDir by extra(project.findProperty("servers.dir") ?: "${rootDir}/servers" as String)
val java11Home by extra((project.findProperty("java11.home") ?: "/opt/jdk-11.0.2") as String)

tasks {
    val dockerCommandLine = arrayOf("docker", "run", "--rm",
        "--name", "npmbuild",
        "-v", "${projectDir}:/project",
        "-w", "/project", "node:lts")

    register<Exec>("npmInstall") {
        commandLine = listOf(*dockerCommandLine, "npm", "install")
    }

    register<Exec>("webpack") {
        outputs.cacheIf { true }
        inputs.dir("src/main/js")
            .withPropertyName("js")
            .withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.files("package-lock.json", "webpack.config.js")
            .withPropertyName("configFiles")
            .withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.dir("$buildDir/js")
            .withPropertyName("bundle")
        commandLine = listOf(*dockerCommandLine, "npm", "run", "build")
    }
}

teamcity {
    version = teamcityVersion

    server {
        descriptor {
            name = project.name
            displayName = project.name
            version = project.version as String
            vendorName = project.extra["vendorName"] as String
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
        downloadsDir = project.extra["downloadsDir"] as String
        baseHomeDir = serversDir as String
        baseDataDir = "${rootDir}/data"

        register("teamcity2022.04") {
            version = "2022.04.3"
            javaHome = java11Home
            serverOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
            agentOptions ("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006")
        }
    }
}
