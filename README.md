jruby-gradle-war-plugin
=======================

[![Build Status](https://buildhive.cloudbees.com/job/jruby-gradle/job/jruby-gradle-war-plugin/badge/icon)](https://buildhive.cloudbees.com/job/jruby-gradle/job/jruby-gradle-war-plugin/) [![Download](https://api.bintray.com/packages/jruby-gradle/plugins/jruby-gradle-war-plugin/images/download.png)](https://bintray.com/jruby-gradle/plugins/jruby-gradle-war-plugin) [![Gitter chat](https://badges.gitter.im/jruby-gradle/jruby-gradle-plugin.png)](https://gitter.im/jruby-gradle/jruby-gradle-plugin)

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
`.war/WEB-INF/libs` when the archive is created.

## Default Tasks

The plugin provides the following tasks:

 * `jrubyWar` - Creates a runnable web archive file in `build/libs` for your
   project.
 * `jrubyClean` - Cleans up the temporary directories that tasks like
   `jrubyWar`- Creates a `war` file


## Creating a .war

A *full* example of this can be found in the
[hellowarld](https://github.com/rtyler/hellowarld) repository.

Configuring the war:

```groovy
buildscript {
    repositories { jcenter() }

    dependencies {
        classpath group: 'com.github.jruby-gradle', name: 'jruby-gradle-war-plugin', version: '0.1.2+'
    }
}
apply plugin: 'com.github.jruby-gradle.war'


dependencies {
    gems 'rubygems:rake:10.0.+'
    gems 'rubygems:colorize:0.7.3'
    gems 'rubygems:sinatra:1.4.5'
}

jrubyWar {
    webInf {
        from 'Rakefile'
        from 'config.ru'
        into('app') { from 'app' }
    }
}
```

The above configuration will copy the contents of `app/`, `Rakefile`, and
`config.ru` into the `WEB-INF/` directory within the `.war` file

Once your application is ready, you can create the `.war` by executing the `jrubyWar` task:

```bash
$ ./gradlew jrubyWar
:compileJava UP-TO-DATE
:processResources UP-TO-DATE
:classes UP-TO-DATE
:jrubyPrepareGems
/usr/home/tyler/.gradle/caches/modules-2/files-2.1/org.jruby/jruby-complete/1.7.15/4d9cb332bad3633c9c23a720542f456dc0c58a81/jruby-complete-1.7.15.jar!/META-INF/jruby.home/lib/ruby/shared/rubygems/installer.rb:507 warning: executable? does not in this environment and will return a dummy value
Successfully installed tilt-1.4.1
/usr/home/tyler/.gradle/caches/modules-2/files-2.1/org.jruby/jruby-complete/1.7.15/4d9cb332bad3633c9c23a720542f456dc0c58a81/jruby-complete-1.7.15.jar!/META-INF/jruby.home/lib/ruby/shared/rubygems/installer.rb:507 warning: executable? does not in this environment and will return a dummy value
Successfully installed rack-1.5.2
Successfully installed rack-protection-1.5.3
/usr/home/tyler/.gradle/caches/modules-2/files-2.1/org.jruby/jruby-complete/1.7.15/4d9cb332bad3633c9c23a720542f456dc0c58a81/jruby-complete-1.7.15.jar!/META-INF/jruby.home/lib/ruby/shared/rubygems/installer.rb:507 warning: executable? does not in this environment and will return a dummy value
Successfully installed rake-10.0.4
Successfully installed colorize-0.7.3
Successfully installed sinatra-1.4.5
6 gems installed
:jrubyPrepare
:jrubyWar

BUILD SUCCESSFUL

Total time: 20.342 secs
%
```

Once the `.war` has been created you can find it in `build/libs` and deploy that into a servlet container such as Tomcat or Jetty.


