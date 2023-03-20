
plugins {
    kotlin("jvm")
    id ("io.github.rodm.teamcity-agent")
}

dependencies {
    implementation (project(":common"))
    implementation (kotlin("stdlib"))
}

teamcity {
    agent {
        descriptor {
            pluginDeployment {
                useSeparateClassloader = true
            }
        }
    }
}
