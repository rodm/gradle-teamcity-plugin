
plugins {
    id ("java")
    id ("io.github.rodm.teamcity-server")
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

dependencies {
    agent (project(path = ":plugin:agent", configuration = "plugin"))
}

teamcity {
    server {
        archiveName = "docker-demo-plugin.zip"
        descriptor {
            name = project.name
            displayName = project.name
            version = project.version as String
            vendorName = rootProject.extra["vendorName"] as String
            vendorUrl = "http://example.com"
            description = "TeamCity Example Server Plugin"

            downloadUrl = "https://github.com/rodm/gradle-teamcity-plugin/"
            email = "rod.n.mackenzie@gmail.com"
            useSeparateClassloader = true
        }
    }
}
