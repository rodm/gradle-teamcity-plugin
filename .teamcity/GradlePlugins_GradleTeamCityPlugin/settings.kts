package GradlePlugins_GradleTeamCityPlugin

import jetbrains.buildServer.configs.kotlin.v2017_2.version
import jetbrains.buildServer.configs.kotlin.v2017_2.project
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2017_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2017_2.Template
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2017_2.projectFeatures.VersionedSettings
import jetbrains.buildServer.configs.kotlin.v2017_2.projectFeatures.versionedSettings
import jetbrains.buildServer.configs.kotlin.v2017_2.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.v2017_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2017_2.vcs.GitVcsRoot

version = "2017.2"
project {
    uuid = "eb3a697d-cd54-4621-bb47-d9daf32fec9a"
    id = "GradlePlugins_GradleTeamCityPlugin"
    parentId = "GradlePlugins"
    name = "Gradle TeamCity Plugin"

    val vcsId = "GradleTeamcityPlugin"
    val vcs = GitVcsRoot({
        uuid = "0d4c8f2d-6673-4f1d-a16d-c2924ea2f114"
        id = vcsId
        name = "gradle-teamcity-plugin"
        url = "https://github.com/rodm/gradle-teamcity-plugin.git"
        branchSpec = """
            +:refs/heads/(master)
            +:refs/tags/(*)
        """.trimIndent()
        useTagsAsBranches = true
        useMirrors = false
    })
    vcsRoot(vcs)

    features {
        versionedSettings {
            id = "PROJECT_EXT_8"
            mode = VersionedSettings.Mode.ENABLED
            rootExtId = vcsId
            showChanges = true
            settingsFormat = VersionedSettings.Format.KOTLIN
            buildSettingsMode = VersionedSettings.BuildSettingsMode.PREFER_SETTINGS_FROM_VCS
        }
        feature {
            id = "PROJECT_EXT_2"
            type = "JetBrains.SharedResources"
            param("quota", "1")
            param("name", "GradlePluginBuildLimit")
            param("id", "PROJECT_EXT_2")
            param("type", "quoted")
            param("enabled", "true")
        }
    }

    val buildTemplate = Template({
        uuid = "2e01646d-6212-453d-8fae-61093898e266"
        id = "GradlePlugins_GradleTeamCityPlugin_Build"
        name = "build"

        params {
            param("gradle.opts", "")
            param("gradle.tasks", "clean build")
            param("java.home", "%java7.home%")
        }

        vcs {
            root(vcs)
            checkoutMode = CheckoutMode.ON_SERVER
            cleanCheckout = true
        }

        steps {
            gradle {
                id = "RUNNER_38"
                tasks = "%gradle.tasks%"
                buildFile = ""
                gradleParams = "%gradle.opts%"
                useGradleWrapper = true
                enableStacktrace = true
                jdkHome = "%java.home%"
            }
        }

        triggers {
            vcs {
                id = "vcsTrigger"
                quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
                branchFilter = "+:<default>"
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
            feature {
                id = "BUILD_EXT_1"
                type = "JetBrains.SharedResources"
                param("locks-param", "GradlePluginBuildLimit readLock")
            }
        }

        requirements {
            doesNotEqual("teamcity.agent.jvm.os.name", "Linux", "RQ_15")
        }
    })
    template(buildTemplate)

    val buildJava7 = BuildType({
        template(buildTemplate)
        uuid = "757ce893-53d8-41b8-92a4-bd1b5450d0a9"
        id = "GradlePlugins_GradleTeamCityPlugin_BuildJava7"
        name = "Build - Java 7"
    })
    buildType(buildJava7)

    val buildFunctionalTestJava7 = BuildType({
        template(buildTemplate)
        uuid = "401cdb0c-9c4b-4f56-a17e-f7ddc0823c04"
        id = "GradlePlugins_GradleTeamCityPlugin_BuildFunctionalTestJava7"
        name = "Build - Functional Test - Java 7"

        params {
            param("gradle.tasks", "clean functionalTest")
        }

        failureConditions {
            executionTimeoutMin = 20
        }
    })
    buildType(buildFunctionalTestJava7)

    val buildFunctionalTestJava8 = BuildType({
        template(buildTemplate)
        uuid = "c8fd1023-03a2-40cd-a226-abbc5086ee47"
        id = "GradlePlugins_GradleTeamCityPlugin_BuildFunctionalTestJava8"
        name = "Build - Functional Test - Java 8"

        params {
            param("gradle.tasks", "clean functionalTest")
            param("java.home", "%java8.home%")
        }

        failureConditions {
            executionTimeoutMin = 20
        }
    })
    buildType(buildFunctionalTestJava8)

    val buildFunctionalTestJava9 = BuildType({
        template(buildTemplate)
        uuid = "332c8e0a-7e3f-4b0c-9443-3a4e46a22eea"
        id = "GradlePlugins_GradleTeamCityPlugin_BuildFunctionalTestJava9"
        name = "Build - Functional Test - Java 9"

        params {
            param("gradle.tasks", "clean functionalTest")
            param("gradle.version", "4.6")
            param("java.home", "%java9.home%")
        }

        steps {
            script {
                id = "RUNNER_23"
                scriptContent = """
                #!/bin/sh

                JAVA_HOME=%java8.home% ./gradlew wrapper --gradle-version=%gradle.version%
                JAVA_HOME=%java.home% ./gradlew --version
                """.trimIndent()
            }
            stepsOrder = arrayListOf("RUNNER_23", "RUNNER_38")
        }

        failureConditions {
            executionTimeoutMin = 20
        }
    })
    buildType(buildFunctionalTestJava9)

    val buildFunctionalTestJava10 = BuildType({
        template(buildTemplate)
        uuid = "332c8e0a-7e3f-4b0c-9443-3a4e46a22eeb"
        id = "GradlePlugins_GradleTeamCityPlugin_BuildFunctionalTestJava10"
        name = "Build - Functional Test - Java 10"

        params {
            param("gradle.tasks", "clean functionalTest")
            param("gradle.version", "4.7")
            param("java.home", "%java10.home%")
        }

        steps {
            script {
                id = "RUNNER_23"
                scriptContent = """
                #!/bin/sh

                JAVA_HOME=%java8.home% ./gradlew wrapper --gradle-version=%gradle.version%
                JAVA_HOME=%java.home% ./gradlew --version
                """.trimIndent()
            }
            stepsOrder = arrayListOf("RUNNER_23", "RUNNER_38")
        }

        failureConditions {
            executionTimeoutMin = 20
        }
    })
    buildType(buildFunctionalTestJava10)

    val buildSamplesTestJava7 = BuildType({
        template(buildTemplate)
        uuid = "1bd1f032-62a6-4fc7-9d2a-ef4be7db82c6"
        id = "GradlePlugins_GradleTeamCityPlugin_BuildSamplesTestJava7"
        name = "Build - Samples Test - Java 7"

        artifactRules = "samples/**/build/distributions/*.zip"

        params {
            param("gradle.tasks", "clean samplesTest")
        }

        failureConditions {
            executionTimeoutMin = 15
        }
    })
    buildType(buildSamplesTestJava7)

    val buildSamplesTestJava8 = BuildType({
        template(buildTemplate)
        uuid = "473f178e-494b-4dd1-95dc-3719ac685bcc"
        id = "GradlePlugins_GradleTeamCityPlugin_BuildSamplesTestJava8"
        name = "Build - Samples Test - Java 8"

        artifactRules = "samples/**/build/distributions/*.zip"

        params {
            param("gradle.tasks", "clean samplesTest")
            param("java.home", "%java8.home%")
        }

        failureConditions {
            executionTimeoutMin = 15
        }
    })
    buildType(buildSamplesTestJava8)

    val reportCodeQuality = BuildType({
        template(buildTemplate)
        uuid = "cac4b0be-d3cf-4028-8e77-ba22555e525f"
        id = "GradlePlugins_GradleTeamCityPlugin_ReportCodeQuality"
        name = "Report - Code Quality"

        params {
            param("gradle.opts", "%sonar.opts%")
            param("gradle.tasks", "clean build sonarqube")
            param("java.home", "%java8.home%")
        }

        features {
            feature {
                id = "gradle-init-scripts"
                type = "gradle-init-scripts"
                param("initScriptName", "sonarqube.gradle")
            }
        }
    })
    buildType(reportCodeQuality)

    buildTypesOrder = arrayListOf(buildJava7, buildFunctionalTestJava7, buildFunctionalTestJava8, buildFunctionalTestJava9, buildFunctionalTestJava10, buildSamplesTestJava7, buildSamplesTestJava8, reportCodeQuality)
}
