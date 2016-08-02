# Gradle TeamCity plugin

The plugin provides support to [develop TeamCity plugins](https://confluence.jetbrains.com/display/TCD9/Extending+TeamCity), creating agent and server side archives, downloading and
installing a TeamCity server and tasks to deploy, start and stop the server and agent.

[![Build Status](https://travis-ci.org/rodm/gradle-teamcity-plugin.svg?branch=master)](https://travis-ci.org/rodm/gradle-teamcity-plugin)

## Using the plugin

### Apply the plugin to the build

The plugin is published on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.github.rodm.teamcity-server).
The following code adds the plugin to your build script. 

    buildscript {
      repositories {
        maven {
          url "https://plugins.gradle.org/m2/"
        }
      }
      dependencies {
        classpath "com.github.rodm:gradle-teamcity-plugin:0.9.1"
      }
    }

### Plugins

The jar contains three plugins:

* `com.github.rodm.teamcity-server` - Provides tasks to package a TeamCity plugin, deploy and undeploy the plugin to a
TeamCity server, and tasks to start and stop the server and default agent.
* `com.github.rodm.teamcity-agent` - Provides tasks to package the Agent side of a TeamCity plugin.
* `com.github.rodm.teamcity-common` - Adds the `common-api` dependency to a project.

### Configurations

The plugins add the following configurations.

* `provided` : The `provided` configuration is used to define dependencies required at compile time but not to be
included in the plugin. By default the plugins add the `agent-api` and `server-api` depenencies when the `java` plugin
is also applied.
* `agent` : The `agent` configuration is used to define additional dependencies to be included in the agent plugin
lib directory when used with the agent plugin. When used with the server plugin the dependencies are added to the
 agent directory of the plugin archive.
* `server` : The `server` configuration is used to define additional dependencies to be included in the server plugin
lib directory when used with the server plugin.
* `plugin` : The `plugin` configuration is used by the agent plugin when publishing the agent plugin archive.

### Extension Properties

The following properties are defined in the `teamcity` configuration block. The version property controls the version of
the TeamCity server-api added to the `compile` configuration and the version downloaded and installed. The plugin
descriptor can be specified as a path to a file or by a configuration block within the build script.

* `version` : The version of the TeamCity API to build against. Defaults to '9.0'.
* `defaultRepositories` : The defaultRepositories flag controls adding the default repositories to the build. By default Maven Central
and the TeamCity repository, http://download.jetbrains.com/teamcity-repository, are configured for resolving dependencies. Setting this
 flag to false allows a local repository to be used for resolving dependencies.
* `descriptor` : The plugin descriptor, the descriptor can be defined within the build script or reference an external file.
 The type property affects the type of descriptor generated.

The plugin descriptor properties are shown in the examples below and described in the TeamCity documentation for [Packaging Plugins](https://confluence.jetbrains.com/display/TCD9/Plugins+Packaging#PluginsPackaging-PluginDescriptor)

### TeamCity Server Plugin

The plugin when applied with the Java Plugin, adds the JetBrains Maven repository and adds the TeamCity server-api and
tests-supprt dependencies to the compile and testCompile configurations. If the Java Plugin is not applied the plugin
provides only the tasks to package the server side plugin archive if a plugin descriptor is defined. 

The server plugin can be combined with the agent plugin but not with the Java Plugin. 

### TeamCity Server Plugin Configuration

The following properties can be defined in the `server` configuration block.

* `downloadsDir` : The directory the TeamCity installers are downlowded to. Defaults to `downloads`
* `baseDownloadUrl` : The base URL used to download the TeamCity installer. Defaults to `http://download.jetbrains.com/teamcity`.
* `baseHomeDir` : The base directory for a TeamCity install. Defaults to `servers`.
* `baseDataDir` : The base directory for a TeamCity Data directory. Defaults to `data`.

An `environments` configuration block allows multiple TeamCity environments to be defined, each environment supports the following properties

* `version` : The TeamCity version, the version of TeamCity to download and install locally. Defaults to '9.0'.
* `downloadUrl` : The URL used to download the TeamCity installer. Defaults to `${baseDownloadUrl}/TeamCity-${version}.tar.gz`.
* `homeDir` : The path to a TeamCity install. Defaults to `${baseHomeDir}/TeamCity-${version}`
* `dataDir` : The path to the TeamCity Data directory. Defaults to `${baseDataDir}/${version}`, version excludes the bug fix digit.
* `javaHome` : The path to the version of Java used to run the server and build agent. Defaults to the Java used to run Gradle.
* `serverOptions` : Options passed to the TeamCity server via the `TEAMCITY_SERVER_OPTS` environment variable. Default '-Dteamcity.development.mode=true -Dteamcity.development.shadowCopyClasses=true'
 these plguin development settings are described on the [Development Environment](https://confluence.jetbrains.com/display/TCD9/Development+Environment) page.
* `agentOptions` : Options passed to the TeamCity agent via the `TEAMCITY_AGENT_OPTS` environment variable.

### TeamCity Server Plugin Tasks

* `serverPlugin` : Builds and packages a TeamCity plugin.
* `generateDescriptor` : If the descriptor is defined in the build script this task is enabled and will
output the descriptor to the build directory. 
* `processDescriptor` : If the descriptor is defined as an external file this task is enabled and will copy
the file to the build directory. ('build/descriptor/server')

* `deployPlugin` : Deploys the plugin archive to a local TeamCity server, requires the `dataDir` property to be defined.
* `undeployPlugin` : Undeploys the plugin archive from a local TeamCity server, requires the `dataDir` property to be defined.
* `startSever` : Starts the TeamCity Server, requires the `homeDir` and `dataDir` properties to be defined.
* `stopServer` : Stops the TeamCity Server, requires the `homeDir` property to be defined.
* `startAgent` : Starts the default TeamCity Build Agent, requires the `homeDir` property to be defined.
* `stopAgent` : Stops the default TeamCity Build Agent, requires the `homeDir` property to be defined.
* `installTeamCity` : Downloads and installs TeamCity, this tasks uses the `downloadBaseUrl` and the `homeDir` properties.

For each environment the following tasks are created based on the environment name:

* `deployPluginTo<envionment>` : Deploys the plugin archive to the TeamCity server for the environment, requires the environment `dataDir` property.
* `undeployPluginFrom<environment>` : Undeploys the plugin archive from the TeamCity server for the environment, requires the environment `dataDir` property.
* `start<environment>Sever` : Starts the TeamCity Server for the environment, requires the environment `homeDir` and `dataDir` properties to be defined.
* `stop<environment>Server` : Stops the TeamCity Server for the environment, requires the environment `homeDir` property to be defined.
* `start<environment>Agent` : Starts the default TeamCity Build Agent for the environment, requires the environment `homeDir` property to be defined.
* `stop<environment>Agent` : Stops the default TeamCity Build Agent for the environment, requires the environment `homeDir` property to be defined.
* `install<environment>` : Downloads and installs TeamCity for the environment, this tasks uses the `downloadBaseUrl` and the environment `homeDir` properties.

### Examples

Plugin descriptor defined in the build script.

    teamcity {
        // Use TeamCity 8.1 API 
        version = '8.1.5'
    
        // Plugin descriptor
        server {
            descriptor {
                // required properties
                name = project.name
                displayName = 'TeamCity Plugin'        
                version = project.version
                vendorName = 'vendor name'
                
                // optional properties
                description = 'Example TeamCity plugin'
                downloadUrl = 'download url'
                email = 'me@example.com'
                vendorUrl = 'vendor url'
                vendorLogo = 'vendor logo'
                useSeparateClassloader = true
                
                minimumBuild = '10'
                maximumBuild = '20'
        
                parameters {
                    parameter 'name1', 'value1'
                    parameter 'name2', 'value2'
                }
                
                dependencies {
                    plugin 'plugin1-name'
                    plugin 'plugin2-name'
                    tool 'tool1-name'
                    tool 'tool2-name'
                }
            }

            // Additional files can be included in the server plugin archive using the files configuration block
            files {
                into('tooldir') {
                    from('tooldir')
                }
            }
        }
    }

Plugin descriptor defined in an external file at the root of the project. A map of tokens to be replaced in the
descriptor file can be provided using the `tokens` property.

    teamcity {
        // Use TeamCity 8.1 API 
        version = '8.1.5'    
        
        server {
            // Locate the plugin descriptor in the root directory of the project
            descriptor = file('teamcity-plugin.xml')
            tokens = [VERSION: project.version, VENDOR_NAME: 'vendor name']
        }
    }

If the Gradle project only has the TeamCity Server Plugin applied the server configuration block can be omitted.

    teamcity {
        // Use TeamCity 8.1 API 
        version = '8.1.5'   
         
        // Locate the plugin descriptor in the root directory of the project
        descriptor = file('teamcity-plugin.xml')
    }

Environments allow a plugin to be tested against multiple versions for TeamCity.

    teamcity {
        // Use TeamCity 8.1 API
        version = '8.1'

        server {
            // Locate the plugin descriptor in the root directory of the project
            descriptor = file('teamcity-plugin.xml')

            // use a local web server for downloading TeamCity distributions
            baseDownloadUrl = "http://repository/"

            // store the downloaded TeamCity distributions in /tmp
            downloadsDir = '/tmp'

            // base properties for TeamCity servers and data directories
            baseHomeDir = 'teamcity/servers'
            baseDataDir = 'teamcity/data'

            environments {
                teamcity81 {
                    version = '8.1.5'
                    javaHome = file('/opt/jdk1.7.0_80')
                }

                teamcity90 {
                    version = '9.0.5'
                    javaHome = file('/opt/jdk1.7.0_80')
                    // Add to the default server options
                    serverOptions '-Xdebug'
                    serverOptions '-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5500'
                }

                teamcity91 {
                    version = '9.1.6'
                    downloadUrl = 'http://repository/teamcity/TeamCity-9.1.6.tar.gz'
                    homeDir = file("$rootDir/teamcity/servers/TeamCity-9.1.6")
                    dataDir = file("$rootDir/teamcity/data/9.1")
                    javaHome = file('/opt/jdk1.8.0_60')
                    // Replace the default server options
                    serverOptions = '-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5500'
                }
            }
        }
    }

### TeamCity Agent Plugin

The plugin when applied with the Java Plugin, adds the JetBrains Maven repository and adds the TeamCity agent-api and
tests-supprt dependencies to the compile and testCompile configurations. If the Java Plugin is not applied the plugin
provides only the tasks to package the agent side plugin archive if a plugin descriptor is defined. 

### TeamCity Agent Plugin Tasks

* `agentPlugin` : Builds and packages the agent side of a TeamCity plugin. The artifacts defined on the 'agent'
 configuration are added to the lib directory of the agent plugin archive. 
* `generateAgentDescriptor` : If the descriptor is defined in the build script this task is enabled and will
output the descriptor to the build directory. 
* `processAgentDescriptor` : If the descriptor is defined as an external file this task will copy the file to the build
directory. ('build/descriptor/agent')

### Examples

Agent side plugin descriptor 

    teamcity {
        version = teamcityVersion

        agent {
            descriptor {
                pluginDeployment {
                    useSeparateClassloader = false
                    executableFiles {
                        include 'file1'
                        include 'file2'
                    }
                }
                dependencies {
                    plugin 'plugin-name'
                    tool 'tool-name'
                }
            }
        }
    }

Agent tool descriptor

    teamcity {
        version = teamcityVersion

        agent {
            descriptor {
                toolDeployment {
                    executableFiles {
                        include 'tooldir/file1'
                        include 'tooldir/file2'
                    }
                }
                dependencies {
                    plugin 'plugin-name'
                    tool 'tool-name'
                }
            }

            // Additional files can be included in the agent plugin archive using the files configuration block
            files {
                into('tooldir') {
                    from('tooldir')
                }
            }
        }
    }

If the Gradle project only has the TeamCity Agent Plugin applied the agent configuration block can be omitted.

    teamcity {
        version = teamcityVersion

        descriptor {
            pluginDeployment {
                useSeparateClassloader = false
                executableFiles {
                    include 'file1'
                    include 'file2'
                }
            }
            dependencies {
                plugin 'plugin-name'
                tool 'tool-name'
            }
        }
    }

## Samples

The samples directory contains some simple examples of using the plugin.

* `server-plugin` : A simple server-side only plugin.
* `agent-server-plugin` : A simple plugin with an agent-side and server-side components.
* `multi-project-plugin` : A plugin with agent-side and server-side built from multiple Gradle Builds and packages a TeamCity plugin.
* `agent-tool-plugin` : A simple tool plugin that repackages Maven.

The following projects use the plugin.

* [AWS CodeDeploy](https://github.com/JetBrains/teamcity-aws-codedeploy-plugin) plugin
* [AWS CodePipeline](https://github.com/JetBrains/teamcity-aws-codepipeline-plugin) plugin
* [Rust and Cargo Support](https://github.com/JetBrains/teamcity-rust-plugin) plugin
* [Framework for process output parsers](https://github.com/JetBrains/process-output-parsers) plugin
* [TeamCity oAuth authentication](https://github.com/pwielgolaski/teamcity-oauth) plugin
* [Docker Deploy](https://github.com/codeamatic/teamcity-docker-runner) plugin
* [Teamcity web parameters](https://github.com/grundic/teamcity-web-parameters) plugin
* [JMX Plugin](https://github.com/rodm/teamcity-jmx-plugin) plugin
* [JVM Monitor Plugin](https://github.com/rodm/teamcity-jvm-monitor-plugin) plugin
