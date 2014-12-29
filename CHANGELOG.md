# Changelog

## v0.1.4 - Roadmap

### Bugfixes

* [#14](https://github.com/jruby-gradle/jruby-gradle-jar-plugin/issues/14) - Configuration changes for `jrubyJavaBootstrap`
   did not result in a clean build.
* [#20](https://github.com/jruby-gradle/jruby-gradle-jar-plugin/issues/20) -  jrubyJavaBootstrap should set `GEM_HOME`
  and `GEM_PATH`.

### Improvements

* [#26](https://github.com/jruby-gradle/jruby-gradle-jar-plugin/issues/26) - Remove any dependency on warbler-bootstrap
* [#24](https://github.com/jruby-gradle/jruby-gradle-jar-plugin/issues/24) - Unittests are no longer going online to pull
  down dependencies
* Rolled back to support JDK 1.6

## v0.1.3

* Shadow plugin is now an implicit dependency

## v0.1.2

* [#10](https://github.com/jruby-gradle/jruby-gradle-jar-plugin/issues/10) - Shouldn't the plugin specify a dependency on base?
* [#11](https://github.com/jruby-gradle/jruby-gradle-jar-plugin/pull/11) - Correct references to the `com.github.jruby-gradle` group
* [#12](https://github.com/jruby-gradle/jruby-gradle-jar-plugin/issues/12) - jar plugin doesn't specify the jruby dependency
* [#15](https://github.com/jruby-gradle/jruby-gradle-jar-plugin/pull/15) - Explicitly set the JRuby compile-time dependency

## v0.1.1

* [#2](https://github.com/jruby-gradle/jruby-gradle-jar-plugin/issues/2) - Plugin should have a JRubyAppJar task for building runnable jars
* [#9](https://github.com/jruby-gradle/jruby-gradle-jar-plugin/pull/9) - Fixed it so that 'java-base' instead of 'java' plugin is applied

## v0.1.0

* [#1](https://github.com/jruby-gradle/jruby-gradle-jar-plugin/issues/1) - Extension to build JARs containing GEMs.
