jruby-gradle-war-plugin
=======================

[![Build Status](https://buildhive.cloudbees.com/job/jruby-gradle/job/jruby-gradle-war-plugin/badge/icon)](https://buildhive.cloudbees.com/job/jruby-gradle/job/jruby-gradle-war-plugin/) [![Gitter chat](https://badges.gitter.im/jruby-gradle/jruby-gradle-plugin.png)](https://gitter.im/jruby-gradle/jruby-gradle-plugin)

Plugin for creating JRuby-based web archives

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


