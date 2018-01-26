
import com.github.rodm.teamcity.TeamCityPluginExtension

plugins {
    kotlin("jvm")
}

apply {
    plugin("com.github.rodm.teamcity-common")
}

dependencies {
    compile(kotlin("stdlib"))
}

configure<TeamCityPluginExtension> {
    version = rootProject.extra["teamcityVersion"] as String
}
