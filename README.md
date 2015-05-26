# Gradle TeamCity plugin

Gradle plugin to support the development of TeamCity plugins

[![Build Status](https://travis-ci.org/rodm/gradle-teamcity-plugin.svg?branch=master)](https://travis-ci.org/rodm/gradle-teamcity-plugin)

The plugin applies the Java Plugin, adds the JetBrains Maven repository, adds the TeamCity server-api dependency
to the compile configuration and adds a task to package the plugin into a zip.

## Usage

The plugin is published on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.github.rodm.teamcity), follow
the instructions on the page to add the plugin to your build script. 

To select the version of the TeamCity server-api to use a 'teamcity' configuration block can be added with a version
property by default version 9.0 of the API is selected. The plugin descriptor can be specified as a path to a file or
by a configuration block within the build script.

Example with a plugin descriptor in the root of the project.
```
teamcity {
    // Use TeamCity 8.1 API 
    version = '8.1.5'
    // Locate the plugin descriptor in the project root
    descriptor = file('teamcity-plugin.xml')
}
```

Example with a plugin descriptor defined in the build script.
```
teamcity {
    // Use TeamCity 8.1 API 
    version = '8.1.5'

    // Plugin descriptor
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
        useSeparateClassloader = 'true'
    }
}
```
