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

 * `jrubyPrepare` - Extracts content of Ruby gems in `.gemcache/` into `vendor/`
   for use at runtime *or* when packaging a `.war` file. Also copies the
   content of Java-based dependencies into `.jarcache/` for interpreted use
   (see below)
 * `jrubyWar`


### Using the Ruby interpreter

The primary motivation for this plugin is to replace the use of both
[Bundler](http://bundler.io/) and [Warbler](https://github.com/jruby/warbler)
for JRuby projects.

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
