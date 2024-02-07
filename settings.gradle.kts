
rootProject.name = "gradle-teamcity-plugin"

dependencyResolutionManagement {
    versionCatalogs {
        register("libs") {
            library("download-task", "de.undercouch", "gradle-download-task").version("5.5.0")

            version("plugin-structure", "3.190")
            version("plugin-signer", "0.1.8")
            version("plugin-client", "2.0.20")
            library("plugin-base", "org.jetbrains.intellij.plugins", "structure-base").versionRef("plugin-structure")
            library("plugin-teamcity", "org.jetbrains.intellij.plugins", "structure-teamcity").versionRef("plugin-structure")
            library("plugin-client", "org.jetbrains.intellij", "plugin-repository-rest-client").versionRef("plugin-client")
            library("plugin-signer", "org.jetbrains", "marketplace-zip-signer").versionRef("plugin-signer")
            bundle("publishing", listOf("plugin-base", "plugin-teamcity", "plugin-client", "plugin-signer"))

            version("docker", "3.2.13")
            library("docker-core", "com.github.docker-java", "docker-java-core").versionRef("docker")
            library("docker-httpclient5", "com.github.docker-java", "docker-java-transport-httpclient5").versionRef("docker")
            bundle("docker", listOf("docker-core", "docker-httpclient5"))

            library("junit", "org.junit.jupiter:junit-jupiter:5.9.0")
            library("xmlunit", "org.xmlunit:xmlunit-matchers:2.9.0")
            library("hamcrest", "org.hamcrest:hamcrest-library:2.2")
            library("mockito", "org.mockito:mockito-core:4.7.0")
            bundle("testing", listOf("junit", "xmlunit", "hamcrest", "mockito"))
        }
    }
}
