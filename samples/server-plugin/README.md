
Example TeamCity Server plugin

A simple server side plugin that demonstrates building and deploying a TeamCity plugin using the Gradle TeamCity plugin.

To build the plugin run
    ../../gradlew clean build

To build the plugin using the build script that uses an external plugin descriptor run
    ../../gradlew -b build-alt.gradle clean build

To download and install the TeamCity distribution run
    ../../gradlew installTeamCity
    
    This command will take some time but only needs to be executed once.

To deploy the plugin run
    ../../gradlew deployPlugin

To start the TeamCity server run
    ../../gradlew startServer
    
    The teamcity-server.log should contain log messages from the plugin. 
