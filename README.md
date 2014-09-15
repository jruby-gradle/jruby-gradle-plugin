jruby-gradle-jar-plugin
=======================

[![Build Status](https://buildhive.cloudbees.com/job/jruby-gradle/job/jruby-gradle-jar-plugin/badge/icon)](https://buildhive.cloudbees.com/job/jruby-gradle/job/jruby-gradle-jar-plugin/) [![Gitter chat](https://badges.gitter.im/jruby-gradle/jruby-gradle-plugin.png)](https://gitter.im/jruby-gradle/jruby-gradle-plugin)

Plugin for creating JRuby-based java archives


## Compatibility

This plugin requires Gradle 2.0 or better.

## Bootstrap

To add the plugin to your project
```groovy
buildscript {
  repositories {
    jcenter()
  }
  
    dependencies {
      classpath group: 'com.github.jrubygradle', name: 'jruby-gradle-jar-plugin', version: '0.1.1'
    }  
}

apply plugin: 'com.github.jrubygradle.jar'
```

## Implicit loaded plugins

This loads the following plugins if they are not already loaded:
+ `com.github.jrubygradle.base`

## Using the plugin

This plugin does not add any new tasks or extensions, extends the `Jar` task type with a `jruby` closure. If the `java` plugin
is loaded, then the `jar` task can also be configured.

```groovy
jar {
  jruby {
  
    // Use the default GEM installation directory
    defaultGems()

    // Add this GEM installation directory to the JAR.
    // Can be called more than once for additional directories
    gemDir '/path/to/my/gemDir'
    
    // Equivalent to calling defaultGems()
    defaults 'gem'
        
  }
  
  // All other JAR methods and properties are still valid
}

task myJar (type :Jar) {
  jruby {
    // As above
  }

  // All other JAR methods and properties are still valid
}

```

## Executable JARs

Please note that executable JARs are still an incubating feature. At this point appropriate libs will be copied
to the `META-INF/lib` directory, but a working `init.rb` is not available. It is still the responsibility of the
the user to craft an appropriate `init.rb` and copy it to `META-INF` via the provided the [metaInf {}](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.bundling.Jar.html) closure.
 
 ```groovy
 jar {
   jruby {
   
     // Make the JAR executable and use the default main class
     defaultMainClass()
     
     // Make the JAR executable by supplying your own main class
     mainClass 'my.own.main.'
     
     // Equivalent to calling defaultMainClass()
     defaults 'mainClass'
     
     // Adds dependencies from this configuration into `META-INF/lib`
     // If none are specified, the plugin will default to 'jrubyJar','compile' & 'runtime'
     configurations 'myConfig'    
   }   
 }
```

Using the default main class method `defaultMainClass()` will include class files from 
[warbler-bootstrap](https://github.com/jruby-gradle/warbler-bootstrap) 


## Controlling the version of warbler-bootstrap

By default the version is set to `1.+` meaning anything version 1.0 or beyond. If your project wants to lock
down the specific version, then it can be set via

```groovy
jruby {
  warblerBootstrapversion = '1.0.0'
}
```

