jruby-gradle-jar-plugin
=======================

[![Build Status](https://buildhive.cloudbees.com/job/jruby-gradle/job/jruby-gradle-jar-plugin/badge/icon)](https://buildhive.cloudbees.com/job/jruby-gradle/job/jruby-gradle-jar-plugin/) [![Gitter chat](https://badges.gitter.im/jruby-gradle/jruby-gradle-plugin.png)](https://gitter.im/jruby-gradle/jruby-gradle-plugin)

Plugin for creating JRuby-based java archives


## Compatilibity

This plugin requires Gradle 2.0 or better

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

    // Add this directory to the list of GEM installation directories
    gemDir '/path/to/my/gemDir'
    
    // Make the JAR executable and use the default main class
    defaultMainClass()
    
    // Make the JAR executable by supplying your own main class
    mainClass 'my.own.main.'
    
    // Equivalent to calling defaultGems() and defaultMainClass()
    defaults 'gem, 'mainClass'
        
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

Using the default main class method `defaultMainClass()` will include class files from 
[warbler-bootstrap](https://github.com/jruby-gradle/warbler-bootstrap) 
