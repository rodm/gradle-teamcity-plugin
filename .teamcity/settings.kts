
import com.github.rodm.teamcity.pipeline
import com.github.rodm.teamcity.project.githubIssueTracker
import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.version
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger.QuietPeriodMode.USE_DEFAULT
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS

version = "2023.11"

val javaVersionsSupportedByProjectGradle = (8..19).map { it.toString() }

val javaVersionsSupportedByGradleVersionMap = mapOf(
    "20" to "8.3-rc-3",
    "21" to "8.5",
    "22" to "8.8-rc-1"
)

fun supportedGradleVersion(javaVersion: String?): String? {
    if (javaVersion in javaVersionsSupportedByProjectGradle) return null
    if (javaVersion !in javaVersionsSupportedByGradleVersionMap.keys) throw IllegalArgumentException("Unsupported Java version: ${javaVersion}")
    return javaVersionsSupportedByGradleVersionMap[javaVersion]
}

fun BuildSteps.switchGradleBuildStep(javaHome: String, gradleVersion: String) {
    gradle {
        id = "SWITCH_GRADLE"
        name = "Switch Gradle"
        tasks = "wrapper --gradle-version $gradleVersion"
        jdkHome = javaHome
    }
}

project {

    val vcsId = "GradleTeamcityPlugin"
    val vcs = GitVcsRoot {
        id(vcsId)
        name = "gradle-teamcity-plugin"
        url = "https://github.com/rodm/gradle-teamcity-plugin.git"
        branch = "refs/heads/main"
        branchSpec = """
            +:refs/heads/(main)
            +:refs/tags/(*)
        """.trimIndent()
        useTagsAsBranches = true
        checkoutPolicy = NO_MIRRORS
    }
    vcsRoot(vcs)

    features {
        githubIssueTracker {
            displayName = "GradleTeamCityPlugin"
            repository = "https://github.com/rodm/gradle-teamcity-plugin"
            pattern = """#(\d+)"""
        }
    }

    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val buildTemplate = template {
        id("Build")
        name = "build"

        params {
            param("gradle.opts", "")
            param("gradle.tasks", "clean build")
            param("java.home", "%java8.home%")
        }

        vcs {
            root(vcs)
            cleanCheckout = true
        }

        steps {
            gradle {
                id = "GRADLE_BUILD"
                tasks = "%gradle.tasks%"
                buildFile = ""
                gradleParams = "%gradle.opts%"
                useGradleWrapper = true
                enableStacktrace = true
                jdkHome = "%java.home%"
            }
        }

        failureConditions {
            executionTimeoutMin = 10
        }

        features {
            perfmon {}
        }

        requirements {
            doesNotContain("teamcity.agent.jvm.os.name", "Windows", "RQ_Not_Windows")
        }
    }

    pipeline {
        stage ("Build") {
            build {
                id("BuildJava8")
                name = "Build - Java 8"
                templates(buildTemplate)
            }

            build {
                id("BuildJava11")
                name = "Build - Java 11"
                templates(buildTemplate)

                params {
                    param("java.home", "%java11.home%")
                }
            }

            build {
                id("BuildJava11Windows")
                name = "Build - Java 11 - Windows"
                templates(buildTemplate)

                params {
                    param("java.home", "%java11.home%")
                }

                failureConditions {
                    executionTimeoutMin = 12
                }

                requirements {
                    contains("teamcity.agent.jvm.os.name", "Windows")
                }
                disableSettings("RQ_Not_Windows")
            }

            build {
                id("ReportCodeQuality")
                name = "Report - Code Quality"
                templates(buildTemplate)

                params {
                    param("gradle.opts", "%sonar.opts%")
                    param("gradle.tasks", "clean build sonar")
                }
            }
        }

        stage("Functional Tests") {
            defaults {
                failureConditions {
                    executionTimeoutMin = 20
                }
            }

            matrix {
                axes {
                    "Java"("8", "11", "17", "18", "19", "20", "21", "22")
                }
                build {
                    val javaVersion = axes["Java"]
                    id("BuildFunctionalTestJava${javaVersion}")
                    name = "Build - Functional Test - Java ${javaVersion}"
                    templates(buildTemplate)

                    params {
                        param("gradle.tasks", "clean functionalTest")
                        param("java.home", "%java${javaVersion}.home%")
                    }

                    val gradleVersion = supportedGradleVersion(javaVersion)
                    gradleVersion?.apply {
                        steps {
                            switchGradleBuildStep("%java8.home%", gradleVersion)
                            stepsOrder = arrayListOf("SWITCH_GRADLE", "GRADLE_BUILD")
                        }
                    }
                }
            }

            build {
                templates(buildTemplate)
                id("BuildSamplesTestJava7")
                name = "Build - Samples Test - Java 8"

                params {
                    param("gradle.tasks", "clean samplesTest")
                }

                failureConditions {
                    executionTimeoutMin = 15
                }
            }
        }

        stage ("Publish") {
            build {
                id("TriggerSnapshotBuilds")
                name = "Trigger builds"

                vcs {
                    root(vcs)
                    cleanCheckout = true
                }

                triggers {
                    vcs {
                        id = "vcsTrigger"
                        quietPeriodMode = USE_DEFAULT
                        branchFilter = "+:<default>"
                        triggerRules = """
                            -:.github/**
                            -:README.adoc
                        """.trimIndent()
                    }
                }
            }
        }
    }
}
