
plugins {
    kotlin("jvm")
    id ("com.github.rodm.teamcity-agent")
}

dependencies {
    implementation (project(":common"))
}

teamcity {
    version = rootProject.extra["teamcityVersion"] as String

    agent {
        descriptor {
            pluginDeployment {
                useSeparateClassloader = true
            }
        }
    }
}
