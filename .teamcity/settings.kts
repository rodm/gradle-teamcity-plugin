
import com.github.rodm.teamcity.pipeline
import com.github.rodm.teamcity.gradle.switchGradleBuildStep
import com.github.rodm.teamcity.project.githubIssueTracker

import jetbrains.buildServer.configs.kotlin.v2019_2.version
import jetbrains.buildServer.configs.kotlin.v2019_2.project
import jetbrains.buildServer.configs.kotlin.v2019_2.Template
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.VcsTrigger.QuietPeriodMode.USE_DEFAULT
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS

version = "2021.2"

fun supportedGradleVersion(javaVersion: String?): String? {
    val versionsMap = mapOf(
        "14" to "6.3",
        "15" to "6.7.1",
        "16" to "7.0.2",
        "17" to "7.3"
    )
    return versionsMap[javaVersion]
}

project {

    val vcsId = "GradleTeamcityPlugin"
    val vcs = GitVcsRoot {
        id(vcsId)
        name = "gradle-teamcity-plugin"
        url = "https://github.com/rodm/gradle-teamcity-plugin.git"
        branchSpec = """
            +:refs/heads/(master)
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

    val buildTemplate = Template {
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
            feature {
                id = "perfmon"
                type = "perfmon"
            }
        }

        requirements {
            doesNotContain("teamcity.agent.jvm.os.name", "Windows", "RQ_Not_Windows")
        }
    }
    template(buildTemplate)

    pipeline {
        stage ("Build") {

            build {
                templates(buildTemplate)
                id("BuildJava8")
                name = "Build - Java 8"
            }

            build {
                templates(buildTemplate)
                id("BuildJava11")
                name = "Build - Java 11"

                params {
                    param("java.home", "%java11.home%")
                }
            }

            build {
                templates(buildTemplate)
                id("BuildJava11Windows")
                name = "Build - Java 11 - Windows"

                params {
                    param("java.home", "%java11.home%")
                }
                requirements {
                    contains("teamcity.agent.jvm.os.name", "Windows")
                }
                disableSettings("RQ_Not_Windows")
            }

            build {
                templates(buildTemplate)
                id("ReportCodeQuality")
                name = "Report - Code Quality"

                params {
                    param("gradle.opts", "%sonar.opts%")
                    param("gradle.tasks", "clean build sonarqube")
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
                    "Java"("8", "11", "12", "13")
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
                }
            }

            matrix {
                axes {
                    "Java"("14", "15", "16", "17")
                }
                build {
                    val javaVersion = axes["Java"]
                    val gradleVersion = supportedGradleVersion(javaVersion)
                    id("BuildFunctionalTestJava${javaVersion}")
                    name = "Build - Functional Test - Java ${javaVersion}"
                    templates(buildTemplate)

                    params {
                        param("gradle.tasks", "clean functionalTest")
                        param("gradle.version", "${gradleVersion}")
                        param("java.home", "%java${javaVersion}.home%")
                    }

                    steps {
                        switchGradleBuildStep()
                        stepsOrder = arrayListOf("SWITCH_GRADLE", "GRADLE_BUILD")
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
