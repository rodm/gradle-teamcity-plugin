
rootProject.name = "docker-environment"

include ("plugin")

includeBuild ("../..")

rootProject.buildFileName = "build.gradle.kts"
rootProject.children.forEach { project ->
    project.buildFileName = "build.gradle.kts"
}
