
plugins {
    id("java")
    id("io.github.rodm.teamcity-agent")
}

teamcity {
    // API version is inherited from the root project

    agent {
        descriptor {
            pluginDeployment {
                useSeparateClassloader = false
            }
        }
        // Alternative way to specify the agent plugin descriptor
        // descriptor = project.file('src/main/descriptor/teamcity-plugin.xml')
    }
}
