
rootProject.name = "multiple-plugins"

include ("plugin1")
include ("plugin2")

includeBuild ("../..")

rootProject.buildFileName = "build.gradle.kts"
rootProject.children.forEach { project ->
    project.buildFileName = "build.gradle.kts"
}
