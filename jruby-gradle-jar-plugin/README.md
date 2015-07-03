jruby-gradle-jar-plugin
=======================

[![Build Status](https://buildhive.cloudbees.com/job/jruby-gradle/job/jruby-gradle-jar-plugin/badge/icon)](https://buildhive.cloudbees.com/job/jruby-gradle/job/jruby-gradle-jar-plugin/) [![Download](https://api.bintray.com/packages/jruby-gradle/plugins/jruby-gradle-jar-plugin/images/download.png)](https://bintray.com/jruby-gradle/plugins/jruby-gradle-jar-plugin) [![Gitter chat](https://badges.gitter.im/jruby-gradle/jruby-gradle-plugin.png)](https://gitter.im/jruby-gradle/jruby-gradle-plugin)

Plugin for creating JRuby-based java archives

## Breaking Changes

the default init script is now: META-INF/jar-bootstrap.rb

this breaks the old behaviour where  the init script was META-INF/init.rb

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
      classpath group: 'com.github.jruby-gradle', name: 'jruby-gradle-jar-plugin', version: '0.2.+'
      classpath group: 'com.github.jruby-gradle', name: 'jruby-gradle-plugin', version: '0.1.+'
    }
}

apply plugin: 'com.github.jruby-gradle.jar'
```

## Implicit loaded plugins

This loads the following plugins if they are not already loaded:

+ `com.github.jrubygradle.base`
+ `java-base`

## Using the plugin

This plugin adds a new ```JRubyJar``` task and extends the `Jar` task type with a `jruby` closure. If the `java` plugin is loaded, then the `jar` task can also be configured.

the ```Jar``` task is mainly used to create a library jar with embedded gems and ruby scripts. the ```JRubyJar``` task is meant to create runnable or executable fat jars.

## JRubyJar task

```groovy
apply plugin: 'java'

jrubyJar {

  // Use the default GEM installation directory
  defaultGems()

  // Add this GEM installation directory to the JAR.
  // Can be called more than once for additional directories
  gemDir '/path/to/my/gemDir'

  // Equivalent to calling defaultGems()
  defaults 'gem'

  // All methods and properties from JAR task can be used 
}

task myJar (type :JRubyJarTask) {
  // As above

  // All methods and properties from JAR task can be used 
}
```

## JRubyJar task types

There are three types of jar which can be created:

* library jar
* runnable jar - you can pick executable from the embedded gems via ```-S rake``` syntax to run your code.
* executable jar - there is specific bootstrap ruby script which gets executed

### Executable Jar: Controlling the Ruby entry point script

**Please note that executable JARs are still an incubating feature**.

The ```initScript``` configuration is mandatory. Any path to ruby script will do. The plugin will pack the script in way that the ```defaultMainClass()``` or the ```extractingMainClass()``` will find and execute it.

```groovy
jrubyJar {
    jruby {
        initScript 'bin/asciidoctor'
    }
}
```

The ```defaultMainClass()``` is use used unless some other main class gets declared.

```groovy
jrubyJar {
    jruby {
        initScript 'bin/asciidoctor'
        extractingMainClass()
    }
}
```
This main will extract the jar into a temporary directory before executing the bootstrap script. In some cases where the ruby application tries to spawn a new JRuby process this extracting is needed.

## Runnable Jar

**Please note that runnable JARs are still an incubating feature**.

Configuration is needs to declare the main class then the jar will be executable.

```groovy
jrubyJar {

   // tell the plugin to pack a runnable jar (no bootstrap script)
   initScript runnable()

   // Use the default bootstrap class (can be omitted)
   defaultMainClass()

   // Includes the default gems to the jar (can be omitted)
   defaultGems()

   // Make the JAR executable by supplying your own main class
   mainClass 'my.own.main'

   // Equivalent to calling defaultMainClass() and defaultGems()
   defaults 'gems', 'mainClass'

 }
 ```

## Library Jar

**Please note that library JARs are still an incubating feature**.

```groovy
jrubyJar {

   // tell the plugin to pack a runnable jar (no bootstrap script)
   initScript library()

   // Includes the default gems to the jar (can be omitted)
   defaultGems()
 
   // Equivalent to calling defaultGems()
   defaults 'gems'

 }
```
