# JRuby Gradle plugin

[![Build Status](https://buildhive.cloudbees.com/job/jruby-gradle/job/jruby-gradle-plugin/badge/icon)](https://buildhive.cloudbees.com/job/jruby-gradle/job/jruby-gradle-plugin/) [![Download](https://api.bintray.com/packages/rtyler/jruby/jruby-gradle-plugin/images/download.png)](https://bintray.com/rtyler/jruby/jruby-gradle-plugin/\_latestVersion) [![Gitter chat](https://badges.gitter.im/jruby-gradle/jruby-gradle-plugin.png)](https://gitter.im/jruby-gradle/jruby-gradle-plugin)

The purpose of plugin is to encapsulate useful [Gradle](http://www.gradle.org/)
functionality for JRuby projects. Use of this plugin replaces the need for both
[Bundler](http://bundler.io/) and [Warbler](https://github.com/jruby/warbler)
in JRuby projects.


The Ruby gem dependency code for this project relies on the [Rubygems Maven
proxy](http://rubygems-proxy.torquebox.org/) provided by the
[Torquebox](http://torquebox.org) project.

## Compatilibity

This plugin requires Gradle 2.0 or better

## Getting Started

### Setting up Gradle

**Note:** This assumes you already have [Gradle](http://gradle.org) installed.

```bash
% mkdir fancy-webapp
% cd fancy-webapp
% git init
Initialized empty Git repository in /usr/home/tyler/source/github/fancy-webapp/.git/
% gradle wrapper  # Create the wrappers to easily bootstrap others
wrapper

BUILD SUCCESSFUL

Total time: 6.411 secs
% git add gradle gradlew gradlew.bat
% git commit -m "Initial commit with gradle wrappers"
```

### Creating a gradle configuration file

Create a `build.gradle` file in the root of `fancy-webapp/` with the following:


```groovy
apply plugin: 'com.lookout.jruby'

buildscript {
    repositories { jcenter() }

    dependencies {
      classpath group: 'com.lookout', name: 'jruby-gradle-plugin', version: '2.1.+'
    }
}
```

You can also add Ruby gem dependencies in your `build.gradle` file under the
`gem` configuration, e.g.:

```groovy
dependencies {
    gems group: 'rubygems', name: 'sinatra', version: '1.4.5'
    gems group: 'rubygems', name: 'rake', version: '10.3.+'
}
```

In order to include Java-based dependencies in a `.war` file, declare those
dependencies under the `jrubyWar` configuration, e.g.:

```groovy
dependencies {
    jrubyWar group: 'org.apache.kafka', name: 'kafka_2.9.2', version: '0.8.+'
    jrubyWar group: 'log4j', name: 'log4j', version: '1.2.+', transitive: true
}
```

Dependencies declared under the `jrubyWar` configuration will be copied into
`.jarcache/` and `.war/WEB-INF/libs` when the archive is created.


## Default Tasks

The plugin provides the following tasks:

 * `jrubyWar` - Creates a runnable web archive file in `build/libs` for your
   project.
 * `jrubyPrepareGems` - Extract GEMs declared as dependencies under `gems` to `jruby.gemInstallDir`. This is as instance
 of `JRubyPrepareGems`.
 * `jrubyPrepare` - Call `jrubyPrepareGems`. Also copies the
   content of Java-based dependencies into `.jarcache/` for interpreted use
   (see below)
 * `jrubyClean` - Cleans up the temporary directories that tasks like
   `jrubyWar`- Creates a `war` file

## Creating a .war

Currently the Gradle tooling expects the web application to reside in
`src/main/webapp/WEB-INF`, so make sure your `config.ru` and application code
are under that root directory. It may be useful to symbolicly link this to
`app/` in your root project directory. An *full* example of this can be found in the
[ruby-gradle-example](https://github.com/rtyler/ruby-gradle-example)
repository.

Once your application is ready, you can create the `.war` by executing the `jrubyWar` task:

```bash
% ./gradlew jrubyWar  
:compileJava UP-TO-DATE
:processResources UP-TO-DATE
:classes UP-TO-DATE
:jrubyCacheJars
:jrubyCacheGems
:jrubyPrepareGems
/home/tyler/.rvm/rubies/ruby-1.9.3-p484/lib/ruby/1.9.1/yaml.rb:84:in `<top (required)>':
It seems your ruby installation is missing psych (for YAML output).
To eliminate this warning, please install libyaml and reinstall your ruby.
Successfully installed rack-1.5.2
Successfully installed rack-protection-1.5.3
2 gems installed
/home/tyler/.rvm/rubies/ruby-1.9.3-p484/lib/ruby/1.9.1/yaml.rb:84:in `<top (required)>':
It seems your ruby installation is missing psych (for YAML output).
To eliminate this warning, please install libyaml and reinstall your ruby.
Successfully installed rake-10.3.2
1 gem installed
/home/tyler/.rvm/rubies/ruby-1.9.3-p484/lib/ruby/1.9.1/yaml.rb:84:in `<top (required)>':
It seems your ruby installation is missing psych (for YAML output).
To eliminate this warning, please install libyaml and reinstall your ruby.
Successfully installed tilt-1.4.1
1 gem installed
/home/tyler/.rvm/rubies/ruby-1.9.3-p484/lib/ruby/1.9.1/yaml.rb:84:in `<top (required)>':
It seems your ruby installation is missing psych (for YAML output).
To eliminate this warning, please install libyaml and reinstall your ruby.
Successfully installed sinatra-1.4.5
1 gem installed
:jrubyPrepare
:jrubyWar

BUILD SUCCESSFUL

Total time: 1 mins 34.84 secs
%
```

Once the `.war` has been created you can find it in `build/libs` and deploy that into a servlet container such as Tomcat or Jetty.


## Using the Ruby interpreter

There are still plenty of cases, such as for local development, when you might
not want to create a full `.war` file to run some tests. In order to use the
same gems and `.jar` based dependencies, add the following to the entry point
for your application:

```ruby
# Hack our GEM_HOME to make sure that the `rubygems` support can find our
# unpacked gems in ./vendor/
vendored_gems = File.expand_path(File.dirname(__FILE__) + '/vendor')
if File.exists?(vendored_gems)
  ENV['GEM_HOME'] = vendored_gems
end

jar_cache = File.expand_path(File.dirname(__FILE__) + '/.jarcache/')
if File.exists?(jar_cache)
  # Under JRuby `require`ing a `.jar` file will result in it being added to the
  # classpath for easy importing
  Dir["#{jar_cache}/*.jar"].each { |j| require j }
end
```

**Note:** in the example above, the `.rb` file is assuming it's in the top
level of the source tree, i.e. where `build.gradle` is located

## Advanced Usage

### Using a custom Gem repository

By default the jruby plugin will use
[rubygems-proxy.torquebox.org](http://rubygems-proxy.torquebox.org) as its
source of Ruby gems. This is a server operated by the Torquebox project which
presents [rubygems.org](https://rubygems.org) as a Maven repository.

If you **do not** wish to use this repository, you can run your own Maven
proxy repository for either rubygems.org or your own gem repository by
running the [rubygems-servlets](https://github.com/torquebox/rubygems-servlets)
server.

You can then use that custom Gem repository with:

```groovy
jruby {
    defaultRepositories = false
}

repositories {
    maven { url : 'http://localhost:8989/releases' }
}

dependencies {
    gems group: 'com.lookout', name: 'custom-gem', version: '1.0.+'
}
```

## JRubyExec - Task for Executing a Ruby Script 

In a similar vein to ```JavaExec``` and ```RhinoShellExec```, the ```JRubyExec``` allows for Ruby scripts to be executed
in a Gradle script using JRuby.

```groovy
import com.lookout.jruby.JRubyExec

dependencies {
    jrubyExec 'rubygems:credit_card_validator:1.2.0'
}

task runMyScript( type: JRubyExec ) {
    script 'scripts/runme.rb'
    scriptArgs '-x', '-y'
}
```

Common methods for ```JRubyExec``` for executing a script

* ```script``` - ```Object``` (Usually File or String). Path to the script.
* ```scriptArgs``` - ```List```.  List of arguments to pass to script.
* ```workingDir``` - ```Object``` (Usually File or String).  Working directory for script.
* ```environment``` - ```Map```.  Environment to be set. Do not set ```GEM_HOME``` or ```GEM_PATH``` with this method.
* ```standardInput``` - ```InputStream```.  Set an input stream to be read by the script.
* ```standardOutput``` - ```OutputStream```.  Capture the output of the script.
* ```errorOutput``` - ```OutputStream```.  Capture the error output of the script.
* ```ignoreExitValue``` - ```Boolean```.  Ignore the JVm exit value. Exit values are only effective if the exit value of the Ruby script is correctly communicated back to the JVM.
* ```configuration``` - ```String```.  Configuration to copy gems from. (*) 
* ```classpath``` - ```List```.  Additional Jars/Directories to place on classpath.
* ```jrubyVersion``` - ```String```.  JRuby version to use if not the same as ```project.jruby.execVersion```. 

(*) If ```jRubyVersion``` has not been set, ```jrubyExec``` will used as
configuration. However, if ```jRubyVersion``` has been set, no gems will be used unless an explicit configuration has been provided

Additional ```JRubyExec``` methods for controlling the JVM instance

* ```jvmArgs``` - ```List```. See [jvmArgs](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.JavaExec.html#org.gradle.api.tasks.JavaExec:jvmArgs)
* ```allJvmArgs``` - ```List```. See [allJvmArgs](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.JavaExec.html#org.gradle.api.tasks.JavaExec:allJvmArgs)
* ```systemProperties``` - ```Map```. See [systemProperties](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.JavaExec.html#org.gradle.api.tasks.JavaExec:systemProperties)
* ```bootstrapClassPath``` - ```FileCollection``` or ```List```. See [bootstrapClassPath](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.JavaExec.html#org.gradle.api.tasks.JavaExec:bootstrapClasspath)
* ```minHeapSize``` - ```String```. See [minHeapSize](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.JavaExec.html)
* ```maxHeapSize``` - ```String```. See [maxHeapSize](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.JavaExec.html#org.gradle.api.tasks.JavaExec:maxHeapSize)
* ```defaultCharacterEncoding``` - ```String```. See [defaultCharacterEncoding](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.JavaExec.html)
* ```enableAssertions``` - ```Boolean```. See [enableAssertions](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.JavaExec.html#org.gradle.api.tasks.JavaExec:enableAssertions)
* ```debug``` - ```Boolean```. See [debug](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.JavaExec.html#org.gradle.api.tasks.JavaExec:debug)
* ```copyTo``` - ```JavaForkOptions```. See [copyTo](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.JavaExec.html)
* ```executable``` - ```Object``` (Usually ```File``` or ```String```). See [executable](http://www.gradle.org/docs/current/dsl/org.gradle.api.tasks.JavaExec.html#org.gradle.api.tasks.JavaExec:executable)

## jrubyexec extension

Similar to ```javaexec``` and ```exec``` it is possible to add the execution of a jruby script within another task

```groovy
task needSomeRubyLove {
  jrubyexec {
    script 'scripts/runme.rb'
    scriptArgs '-x', '-y'  
  }
}
```

The behaviour of `project.jrubyexec` is slightly different to that of `JRubyExec`.

* The version of `jruby-complete` is strictly tied to the `jruby.execVersion`. Therefore trying to set `jrubyVersion`
in the ```jrubyexec``` closure will cause a failure
* GEMs and additional JARs are only taken from the `jrubyExec` configuration. 
* It is not possible to supply a `configuration` parameter to the `jrubyexec` closure.
* GEMs will be installed to `jruby.gemInstallDir`. Existing gems will not be overwritten.

As with `JRubyExec`, `args`, `setArgs` and `main` are illegal within the `jrubyexec` closure.
All other methods should work.

## JRubyPrepareGems - A task for unpacking GEMs

Unpacking occurs using the default `jruby` version as set by `jruby.execVersion`.

```groovy
import com.lookout.jruby.JRubyPrepareGems

task unpackMyGems( type : JRubyPrepareGems ) {

  // Parent directory for unpacking GEMs.
  // Gems will end up in a subdirectory 'gems/GemName-GemVersion'
  outputDir buildDir
  
  // Add one or more gems
  // Can be String(s), File(s), FleCollection(s) or Configuration(s)
  gems project.configuration.gems
  
}
```
