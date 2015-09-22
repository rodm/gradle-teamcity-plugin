
Example TeamCity Tool plugin

A tool plugin that repacks Apache Maven 3.3 using the Gradle TeamCity plugin.

To build the plugin run

    ../../gradlew clean build

To download and install the TeamCity distribution run
Note: This command will take some time but only needs to be executed once.

    ../../gradlew installTeamCity
    
To deploy the plugin run

    ../../gradlew deployPlugin


To start the TeamCity server run

    ../../gradlew startServer

Login to the TeamCity server as an Administrator and view the installed plugins on the Administration page, under the
External Plugins the 'Repacked Maven 3.3' plugin should be shown. 
