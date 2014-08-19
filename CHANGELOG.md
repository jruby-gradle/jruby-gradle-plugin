# Changelog

## v2.1.0

 * [#16](https://github.com/rtyler/jruby-gradle-plugin/pull/16) add the
   `JRubyExec` task type for executing Ruby code with the embedded JRuby
   dependency.
 * [#14](https://github.com/rtyler/jruby-gradle-plugin/pull/14) allow a user to
   set/choose the version of JRuby they wish to use with the plugin

## v2.0.2

 *  More futzing with [bintray](http://bintray.com) release code

## v2.0.1

 * Add attributes to `build.gradle` for incorporating plugi into
   [plugins.gradle.org](http://plugins.gradle.org)

## v2.0.0

 * Switch to a fully qualified gradle plugin name: `com.lookout.jruby`
 * Add the `war` plugin as a dependency to properly build `jrubyWar`

## v1.1.1

 * [#1](https://github.com/rtyler/jruby-gradle-plugin/issues/1) added support
   for a user-changeable Gem repo URL, defaulting to
   [rubygems-proxy.torquebox.org](http://rubygems-proxy.torquebox.org) by
   default.


## v1.1.0

 * Added the `jrubyClean` task for nuking `.gemcache` and `.jarcache`
 * Added the `gems` configuration for segregating gem-based dependencies
 * [#8](https://github.com/rtyler/jruby-gradle-plugin/issues/8) properly set
   the Rubygems Maven proxy as a Maven repository
 * [#7](https://github.com/rtyler/jruby-gradle-plugin/issues/7) pin byte-code
   compatibility to Java 1.7
