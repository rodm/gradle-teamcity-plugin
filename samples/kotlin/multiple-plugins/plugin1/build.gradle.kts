
plugins {
    id ("java")
    id ("io.github.rodm.teamcity-server")
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

teamcity {
    server {
        archiveName = "plugin1.zip"
        descriptor {
            name = project.name
            displayName = project.name
            version = project.version as String
            vendorName = rootProject.extra["vendorName"] as String
            vendorUrl = "http://example.com"
            description = "TeamCity Example 1 Server Plugin"

            downloadUrl = "https://github.com/rodm/gradle-teamcity-plugin/"
            email = "rod.n.mackenzie@gmail.com"
            useSeparateClassloader = false

            parameters {
                parameter ("param", "example 1 server value")
            }
        }
    }
}
