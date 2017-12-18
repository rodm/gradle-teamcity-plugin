
import com.github.rodm.teamcity.TeamCityPluginExtension

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl("https://dl.bintray.com/jetbrains/intellij-plugin-service") }
        gradleScriptKotlin()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin"))
        classpath("com.github.rodm:gradle-teamcity-plugin:1.1-SNAPSHOT")
    }
}

apply {
    plugin("kotlin")
    plugin("com.github.rodm.teamcity-common")
}

repositories {
    mavenCentral()
    gradleScriptKotlin()
}

dependencies {
    compile(group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version = rootProject.extra["kotlinVersion"] as String)
}

configure<TeamCityPluginExtension> {
    version = rootProject.extra["teamcityVersion"] as String
}
