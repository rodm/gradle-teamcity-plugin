
rootProject.name = "docker-environment"

include ("plugin:agent")
include ("plugin:server")

includeBuild ("../..")

rootProject.buildFileName = "build.gradle.kts"
rootProject.children.forEach { project ->
    project.buildFileName = "build.gradle.kts"
}
