
rootProject.name = "gradle-teamcity-plugin"

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        register("libs") {
            alias("download-task").to("de.undercouch", "gradle-download-task").version("5.1.0")

            version("structure", "3.190")
            alias("plugin-base").to("org.jetbrains.intellij.plugins", "structure-base").versionRef("structure")
            alias("plugin-teamcity").to("org.jetbrains.intellij.plugins", "structure-teamcity").versionRef("structure")
            alias("plugin-client").to("org.jetbrains.intellij", "plugin-repository-rest-client").version("2.0.20")
            alias("plugin-signer").to("org.jetbrains", "marketplace-zip-signer").version("0.1.8")
            bundle("publishing", listOf("plugin-base", "plugin-teamcity", "plugin-client", "plugin-signer"))

            version("docker", "3.2.13")
            alias("docker-core").to("com.github.docker-java", "docker-java-core").versionRef("docker")
            alias("docker-httpclient5").to("com.github.docker-java", "docker-java-transport-httpclient5").versionRef("docker")
            bundle("docker", listOf("docker-core", "docker-httpclient5"))

            alias("junit").to("org.junit.jupiter:junit-jupiter:5.8.2")
            alias("xmlunit").to("org.xmlunit:xmlunit-matchers:2.9.0")
            alias("hamcrest").to("org.hamcrest:hamcrest-library:2.2")
            alias("mockito").to("org.mockito:mockito-core:4.6.1")
            bundle("testing", listOf("junit", "xmlunit", "hamcrest", "mockito"))
        }
    }
}
