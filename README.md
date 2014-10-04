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
      classpath group: 'com.github.jruby-gradle', name: 'jruby-gradle-jar-plugin', version: '0.1.1'
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

This plugin does not add any new tasks or extensions, extends the `Jar` task type with a `jruby` closure. If the `java` plugin
is loaded, then the `jar` task can also be configured.

```groovy
apply plugin: 'java'

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

## Controlling the Ruby entry point script

If nothing is specified, then the bootstrap will look for a Ruby script `META-INF/init.rb`.
It is also possible to set the entry script. This must be specified relative to the root of the created JAR.

```groovy
jrubyJavaBootstrap {
    jruby {
        initScript = 'bin/asciidoctor'
    }
}
```

It is the user's responsibility to ensure that entry point script is created and added to the JAR, be it `META-INF/init.rb`
or another specified script.


## Executable JARs

**Please note that executable JARs are still an incubating feature**.

Executable JARs are indirectly supported via the [Gradle Shadow Jar plugin](http://plugins.gradle.org/plugin/com.github.johnrengelman.shadow).


### Adding Shadow JAR
```groovy
buildscript {
  repositories {
    jcenter()
  }

    dependencies {
      classpath 'com.github.jengelman.gradle.plugins:shadow:1.1.1'
      classpath group: 'com.github.jruby-gradle:jruby-gradle-jar-plugin:0.1.1'
      classpath group: 'com.github.jruby-gradle:jruby-gradle-plugin:0.1.+'
    }
}

apply plugin: 'com.github.jruby-gradle.jar'
apply plugin: 'com.github.johnrengelman.shadow'
apply plugin: 'java'
```

### Configuring Shadow JAR

Configuration is exactly the same as for a normal JAR class.

```groovy
shadowJar {
   jruby {

     // Use the default bootstrap class
     defaultMainClass()

     // Make the JAR executable by supplying your own main class
     mainClass 'my.own.main'

     // Equivalent to calling defaultMainClass()
     defaults 'gems', 'mainClass'

   }
 }
```

See [Shadow JAR README](https://github.com/johnrengelman/shadow/blob/master/README.md) for configuration specifics.
In a similar fashion to the `jar` task, the `shadowJar` task will make use of the `jrubyJavaBootstrap` task to
create and compile a basic bootstrap class.
