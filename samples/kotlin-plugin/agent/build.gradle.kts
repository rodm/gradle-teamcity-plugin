
import com.github.rodm.teamcity.TeamCityPluginExtension

plugins {
    kotlin("jvm")
}

apply {
    plugin("com.github.rodm.teamcity-agent")
}

dependencies {
    implementation (project(":common"))
}

configure<TeamCityPluginExtension> {
    version = rootProject.extra["teamcityVersion"] as String

    agent {
        descriptor {
            pluginDeployment {
                useSeparateClassloader = true
            }
        }
    }
}
