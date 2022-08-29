
pluginManagement {
    plugins {
        id ("io.github.rodm.teamcity-server") version "1.5"
    }
}

rootProject.name = "agent-tool-plugin"

includeBuild ("../..")

rootProject.buildFileName = "build.gradle.kts"
