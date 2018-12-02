
plugins {
    kotlin("jvm")
    id ("com.github.rodm.teamcity-common")
}

dependencies {
    implementation (kotlin("stdlib"))
}

teamcity {
    version = rootProject.extra["teamcityVersion"] as String
}
