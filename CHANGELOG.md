# Changelog

## 0.1.9 - Roadmap

* [#73](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/73) - Allow user to override GEM directory for `JRubyExec` and `project.jrubyexec`. Issues addressed that broken 0.1.6 and 0.1.7

## 0.1.8

* [#77](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/77) - 0.1.6
  regresses existing JRubyExec tasks
* [#79](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/79) - Gem
  installation should override jbundler/jar-dependencies defaults

## 0.1.7 - Broken

* [#77](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/77) - Corrects issue introduced in `0.1.6` related to missing `gemWorkDir`


## 0.1.6 - Broken

* [#73](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/73) - Allow user to override GEM directory for `JRubyExec`

## 0.1.5

* [#70](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/70) - Run executable scripts from gem dependency

## 0.1.4

* [#68](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/68) - `JRubyExec` should unset/overwrite `GEM_HOME/GEM_PATH`

## 0.1.3

* [#53](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/53) - `JRubyExec` should not overwrite gems on every run
* [#57](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/57) - Make `JRuby` 1.7.16 the default
* [#58](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/58) - Make build independent of project directory name
* [#61](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/61) - _Native_ gems are not properly supported 
* [#63](https://github.com/jruby-gradle/jruby-gradle-plugin/pull/63) - Make the `JRubyExec` `script` argument optional provided `jrubyArgs` is present
* [#64](https://github.com/jruby-gradle/jruby-gradle-plugin/pull/64) - Updates to `JRubyExec` & `project.jrubyexec` to handle `-S`

