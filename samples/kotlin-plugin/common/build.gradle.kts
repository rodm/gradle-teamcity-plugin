
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
    plugin("com.github.rodm.teamcity-common")
}

extra["kotlinVersion"] = "1.1-M03"
extra["teamcityVersion"] = "9.1"

repositories {
    mavenCentral()
    gradleScriptKotlin()
}

dependencies {
    compile(group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version = extra["kotlinVersion"] as String)
}

configure<TeamCityPluginExtension> {
    version = extra["teamcityVersion"] as String
}
