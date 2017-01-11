
import com.github.rodm.teamcity.ServerPluginConfiguration
import com.github.rodm.teamcity.ServerPluginDescriptor
import com.github.rodm.teamcity.TeamCityPluginExtension

buildscript {
    repositories {
        mavenLocal()
        gradleScriptKotlin()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin"))
        classpath("com.github.rodm:gradle-teamcity-plugin:0.12-SNAPSHOT")
    }
}

apply {
    plugin("kotlin")
    plugin("com.github.rodm.teamcity-server")
}

extra["kotlinVersion"] = "1.1-M03"
extra["teamcityVersion"] = "9.1"

//ext {
//    downloadsDir = project.findProperty('downloads.dir') ?: "$rootDir/downloads"
//    serversDir = project.findProperty('servers.dir') ?: "$rootDir/servers"
//    java7Home = project.findProperty('java7.home') ?: '/opt/jdk1.7.0_80'
//    java8Home = project.findProperty('java8.home') ?: '/opt/jdk1.8.0_92'
//}

repositories {
    mavenCentral()
    gradleScriptKotlin()
}

val agent = configurations.getByName("agent")

dependencies {
    compile(project(":common"))
    agent(project(path = ":agent", configuration = "plugin"))
}

configure<TeamCityPluginExtension> {
    version = extra["teamcityVersion"] as String

    server(closureOf<ServerPluginConfiguration> {
        descriptor(closureOf<ServerPluginDescriptor> {
            name = "Example TeamCity Plugin"
            displayName = "Example TeamCity Plugin"
            version = rootProject.version as String?
            vendorName = "rodm"
            description = "Example multi-project TeamCity plugin"
            email = "rod.n.mackenzie@gmail.com"
            useSeparateClassloader = true
        })
    })

//    environments {
//        downloadsDir = project.downloadsDir
//        baseHomeDir = project.serversDir
//        baseDataDir = 'data'
//
//        teamcity9 {
//            version = '9.1.7'
//            javaHome = file(java7Home)
//        }
//
//        teamcity10 {
//            version = '10.0.4'
//            javaHome = file(java8Home)
//        }
//    }
}
