
plugins {
    id ("java")
    id ("io.github.rodm.teamcity-agent")
}

teamcity {
    agent {
        archiveName = "docker-demo-agent-plugin.zip"
        descriptor {
            pluginDeployment {
                useSeparateClassloader = false
            }
        }
    }
}
