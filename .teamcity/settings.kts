
import com.github.rodm.teamcity.pipeline
import com.github.rodm.teamcity.gradle.switchGradleBuildStep
import com.github.rodm.teamcity.project.githubIssueTracker

import jetbrains.buildServer.configs.kotlin.v2019_2.version
import jetbrains.buildServer.configs.kotlin.v2019_2.project
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.Template
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

version = "2019.2"

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
        useMirrors = false
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
            checkoutMode = CheckoutMode.ON_SERVER
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
    }
    template(buildTemplate)

    pipeline {
        stage ("Build") {

            build {
                templates(buildTemplate)
                id("BuildJava7")
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

            build {
                templates(buildTemplate)
                id("BuildFunctionalTestJava8")
                name = "Build - Functional Test - Java 8"

                params {
                    param("gradle.tasks", "clean functionalTest")
                }
            }

            build {
                templates(buildTemplate)
                id("BuildFunctionalTestJava9")
                name = "Build - Functional Test - Java 9"

                params {
                    param("gradle.tasks", "clean functionalTest")
                    param("java.home", "%java9.home%")
                }
            }

            build {
                templates(buildTemplate)
                id("BuildFunctionalTestJava10")
                name = "Build - Functional Test - Java 10"

                params {
                    param("gradle.tasks", "clean functionalTest")
                    param("java.home", "%java10.home%")
                }
            }

            build {
                templates(buildTemplate)
                id("BuildFunctionalTestJava11")
                name = "Build - Functional Test - Java 11"

                params {
                    param("gradle.tasks", "clean functionalTest")
                    param("java.home", "%java11.home%")
                }
            }

            build {
                templates(buildTemplate)
                id("BuildFunctionalTestJava12")
                name = "Build - Functional Test - Java 12"

                params {
                    param("gradle.tasks", "clean functionalTest")
                    param("gradle.version", "5.4")
                    param("java.home", "%java12.home%")
                }

                steps {
                    switchGradleBuildStep()
                    stepsOrder = arrayListOf("SWITCH_GRADLE", "GRADLE_BUILD")
                }
            }

            build {
                templates(buildTemplate)
                id("BuildFunctionalTestJava13")
                name = "Build - Functional Test - Java 13"

                params {
                    param("gradle.tasks", "clean functionalTest")
                    param("gradle.version", "6.0")
                    param("java.home", "%java13.home%")
                }

                steps {
                    switchGradleBuildStep()
                    stepsOrder = arrayListOf("SWITCH_GRADLE", "GRADLE_BUILD")
                }
            }

            build {
                templates(buildTemplate)
                id("BuildSamplesTestJava7")
                name = "Build - Samples Test - Java 8"

                artifactRules = "samples/**/build/distributions/*.zip"

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

                triggers {
                    vcs {
                        id = "vcsTrigger"
                        quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
                        branchFilter = "+:*"
                    }
                }
            }
        }
    }
}
