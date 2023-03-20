
plugins {
    id("java")
    id("io.github.rodm.teamcity-agent")
}

teamcity {
    // API version is inherited from the root project

    agent {
        // Alternative way to specify the agent plugin descriptor
        descriptor = project.file("teamcity-plugin.xml")
    }
}
