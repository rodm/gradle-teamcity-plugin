
plugins {
    kotlin("jvm")
    id ("com.github.rodm.teamcity-agent")
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
