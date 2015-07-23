
Example TeamCity Build Feature plugin

A simple plugin that demonstrates building a plugin with agent side and server side components and deploying the
plugin using the Gradle TeamCity plugin.

To build the plugin run
```
    ../../gradlew clean build
```

To download and install the TeamCity distribution run
```
    ../../gradlew installTeamCity
```    
    This command will take some time but only needs to be executed once.

To deploy the plugin run
```
    ../../gradlew deployPlugin
```

To start the TeamCity Server and Build Agent run
```
    ../../gradlew startServer
    ../../gradlew startAgent
```    

Create a project and build configuration, on the Build Steps page of the build configuration select the
'Example Build Feature'. Running the build will output the message 'ExampleBuildFeature enabled for build' to
the teamcity-agent.log file. 

To stop the TeamCity Server and Build Agent run
```
    ../../gradlew stopServer
    ../../gradlew stopAgent
```    
