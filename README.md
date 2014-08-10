# JRuby Gradle plugin

[![Download](https://api.bintray.com/packages/rtyler/jruby/jruby-gradle-plugin/images/download.png)](https://bintray.com/rtyler/jruby/jruby-gradle-plugin/_latestVersion)

The purpose of plugin is to encapsulate useful [Gradle](http://www.gradle.org/)
functionality for JRuby projects. Use of this plugin replaces the need for both
[Bundler](http://bundler.io/) and [Warbler](https://github.com/jruby/warbler)
in JRuby projects.


The Ruby gem dependency code for this project relies on the [Rubygems Maven
proxy](http://rubygems-proxy.torquebox.org/) provided by the
[Torquebox](http://torquebox.org) project.


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
% git add gradle gradlew gradle.bat
% git commit -m "Initial commit with gradle wrappers"
```

### Creating a gradle configuration file

Create a `build.gradle` file in the root of `fancy-webapp/` with the following:


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

 * `jrubyWar` - Creates a runnable web archive file in `build/libs` for your
   project.
 * `jrubyPrepare` - Extracts content of Ruby gems in `.gemcache/` into `vendor/`
   for use at runtime *or* when packaging a `.war` file. Also copies the
   content of Java-based dependencies into `.jarcache/` for interpreted use
   (see below)

### Creating a .war

Currently the Gradle tooling expects the web application to reside in `src/main/webapp/WEB-INF`, so make sure your `config.ru` and application code are under that root directory. It may be useful to symbolicly link this to `app/` in your root project directory. An example of this can be found in the [ruby-gradle-example](https://github.com/rtyler/ruby-gradle-example) repository.

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
