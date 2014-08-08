# JRuby Gradle plugin

The purpose of plugin is to encapsulate useful [Gradle](http://www.gradle.org/)
functionality for JRuby projects.

The Ruby gem dependency code for this project relies on the [Rubygems Maven
proxy](http://rubygems-proxy.torquebox.org/) provided by the
[Torquebox](http://torquebox.org) project.


## Usage

Add the following to your project's `build.gradle` file:

```groovy
apply plugin: 'jruby'

buildscript {
    repositories { mavenCentral() }

    dependencies {
      classpath group: 'com.lookout', name: 'jruby-gradle-plugin', version: '1.0.7-SNAPSHOT'
    }
}
```

You can also add Ruby gem dependencies in your `build.gradle` file under the
`runtime` configuration, e.g.:

```groovy
dependencies {
  runtime group: 'rubygems', name: 'sinatra', version: '1.4.5'
  runtime group: 'rubygems', name: 'rake', version: '10.3.+'
}
```


The plugin provides the following tasks:

 * `preparegems` - Extracts content of Ruby gems in `.gemcache/` into `vendor/`
   for use at runtime *or* when packaging a `.war` file.
 * `cachegems` - (semi-internal) Caches `.gem` files into `.gemcache/` for easy extraction
 * `jrubyWar`
