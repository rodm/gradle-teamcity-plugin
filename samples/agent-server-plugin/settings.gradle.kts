
rootProject.name = "agent-server-plugin"
include ("agent")

includeBuild ("../..")

rootProject.buildFileName = "build.gradle.kts"
rootProject.children.forEach { project ->
    project.buildFileName = "build.gradle.kts"
}
