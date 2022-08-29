
pluginManagement {
    plugins {
        kotlin("jvm") version "1.3.72"
        id("io.github.rodm.teamcity-server") version "1.5"
    }
}

rootProject.name = "kotlin-example-plugin"

include("common")
include("agent")
include("server")

includeBuild("../..")
