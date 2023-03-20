
plugins {
    id ("io.github.rodm.teamcity-base")
}

group = "com.github.rodm.teamcity"
version = "1.0-SNAPSHOT"

extra["teamcityVersion"] = findProperty("teamcity.version") ?: "2020.1"

teamcity {
    version = extra["teamcityVersion"] as String
    setValidateBeanDefinition("fail")
}
