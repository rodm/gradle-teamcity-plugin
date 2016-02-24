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
        classpath "com.github.rodm:gradle-teamcity-plugin:0.8.2"
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
* `descriptor` : The plugin descriptor, the descriptor can be defined within the build script or reference an external file.
 The type property affects the type of descriptor generated.   
* `homeDir` : The path to a TeamCity install.
* `dataDir` : The path to the TeamCity Data directory.
* `javaHome` : The path to the version of Java used to run the server and build agent.
* `serverOptions` : Options passed to the TeamCity server via the `TEAMCITY_SERVER_OPTS` environment variable. Default '-Dteamcity.development.mode=true -Dteamcity.development.shadowCopyClasses=true'
 these plguin development settings are described on the [Development Environment](https://confluence.jetbrains.com/display/TCD9/Development+Environment) page.  
* `downloadBaseUrl` : The base URL used to download the TeamCity installer. Default 'http://download.jetbrains.com/teamcity'.
* `downloadDir` : The directory the TeamCity installer is downloaded into. Default 'downloads'.

The plugin descriptor properties are shown in the examples below and described in the TeamCity documentation for [Packaging Plugins](https://confluence.jetbrains.com/display/TCD9/Plugins+Packaging#PluginsPackaging-PluginDescriptor)  

### TeamCity Server Plugin

The plugin when applied with the Java Plugin, adds the JetBrains Maven repository and adds the TeamCity server-api and
tests-supprt dependencies to the compile and testCompile configurations. If the Java Plugin is not applied the plugin
provides only the tasks to package the server side plugin archive if a plugin descriptor is defined. 

The server plugin can be combined with the agent plugin but not with the Java Plugin. 

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
                
        // local TeamCity instance properties        
        homeDir = file("/opt/TeamCity")
        dataDir = file("$rootDir/data")
        javaHome = file("/opt/jdk1.7.0_80")
        
        // local web server for downloading TeamCity distributions 
        downloadBaseUrl = "http://repository/"
        
        // store the downloaded TeamCity distributions in /tmp
        downloadDir = '/tmp'
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
