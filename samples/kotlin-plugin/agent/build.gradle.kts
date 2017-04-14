
import com.github.rodm.teamcity.AgentPluginConfiguration
import com.github.rodm.teamcity.AgentPluginDescriptor
import com.github.rodm.teamcity.PluginDeployment
import com.github.rodm.teamcity.TeamCityPluginExtension

buildscript {
    repositories {
        mavenLocal()
        gradleScriptKotlin()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin"))
        classpath("com.github.rodm:gradle-teamcity-plugin:1.1-SNAPSHOT")
    }
}

apply {
    plugin("kotlin")
    plugin("com.github.rodm.teamcity-agent")
}

repositories {
    mavenCentral()
    gradleScriptKotlin()
}

dependencies {
    compile(project(":common"))
}

configure<TeamCityPluginExtension> {
    version = rootProject.extra["teamcityVersion"] as String

    agent(closureOf<AgentPluginConfiguration> {
        descriptor(closureOf<AgentPluginDescriptor> {
            pluginDeployment(closureOf<PluginDeployment> {
                useSeparateClassloader = true
            })
        })
    })
}
