
plugins {
    id ("java")
    id ("io.github.rodm.teamcity-server")
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

teamcity {
    server {
        archiveName = "plugin2.zip"
        descriptor {
            name = project.name
            displayName = project.name
            version = project.version as String
            vendorName = rootProject.extra["vendorName"] as String
            vendorUrl = "http://example.com"
            description = "TeamCity Example 2 Server Plugin"

            downloadUrl = "https://github.com/rodm/gradle-teamcity-plugin/"
            email = "rod.n.mackenzie@gmail.com"
            useSeparateClassloader = false

            parameters {
                parameter ("param", "example 2 server value")
            }
        }
    }
}
