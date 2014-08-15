# Changelog


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
