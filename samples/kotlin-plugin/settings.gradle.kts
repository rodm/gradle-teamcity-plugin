
pluginManagement {
    plugins {
        kotlin("jvm") version "1.3.72"
        id("com.github.rodm.teamcity-server") version "1.4-beta-1"
    }
}

rootProject.name = "kotlin-example-plugin"

include("common")
include("agent")
include("server")

includeBuild("../..")
