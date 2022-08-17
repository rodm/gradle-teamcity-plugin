
rootProject.name = "gradle-teamcity-plugin"

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        register("libs") {
            alias("download-task").to("de.undercouch", "gradle-download-task").version("5.1.0")

            version("plugin-structure", "3.190")
            version("plugin-signer", "0.1.8")
            version("plugin-client", "2.0.20")
            alias("plugin-base").to("org.jetbrains.intellij.plugins", "structure-base").versionRef("plugin-structure")
            alias("plugin-teamcity").to("org.jetbrains.intellij.plugins", "structure-teamcity").versionRef("plugin-structure")
            alias("plugin-client").to("org.jetbrains.intellij", "plugin-repository-rest-client").versionRef("plugin-client")
            alias("plugin-signer").to("org.jetbrains", "marketplace-zip-signer").versionRef("plugin-signer")
            bundle("publishing", listOf("plugin-base", "plugin-teamcity", "plugin-client", "plugin-signer"))

            version("docker", "3.2.13")
            alias("docker-core").to("com.github.docker-java", "docker-java-core").versionRef("docker")
            alias("docker-httpclient5").to("com.github.docker-java", "docker-java-transport-httpclient5").versionRef("docker")
            bundle("docker", listOf("docker-core", "docker-httpclient5"))

            alias("junit").to("org.junit.jupiter:junit-jupiter:5.9.0")
            alias("xmlunit").to("org.xmlunit:xmlunit-matchers:2.9.0")
            alias("hamcrest").to("org.hamcrest:hamcrest-library:2.2")
            alias("mockito").to("org.mockito:mockito-core:4.7.0")
            bundle("testing", listOf("junit", "xmlunit", "hamcrest", "mockito"))
        }
    }
}
