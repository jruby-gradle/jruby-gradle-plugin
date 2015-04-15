# Changelog

## 0.1.16


## 0.1.15

* [#105](https://github.com/jruby-gradle/jruby-gradle-plugin/pull/105) support
  generating a `gradle.rb` file for executing JRuby code outside of Gradle

## 0.1.14

* [#101](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/101) -
  ZipException with 0.1.12 executing jrubyStorm task. Defaulting the gem
  installation directory to `${buildDir}/gems`

## 0.1.13

* Added support for using closures in the `scriptArgs` for `JRubyExec` tasks
  which are evaluated at execution time.


## 0.1.12

### Improvements

* [#97](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/97) - Allow
  gemInstallDir to be customized
* [#100](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/100) - Bump
  default JRuby version to 1.7.19


### Breaking changes

* `jruby.gemInstallDir` is no longer of type `String`, but now of type `Object` and is of private scope. Access via
   assignment (as per previous versions) or use getter/setter methods.
* Default for `gemInstallDir` is now `"${buildDir}/gems"` instead of `"${buildDir}/vendor/gems"`

## 0.1.11

### Bugfixes

* [#83](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/83) - Installing GEMs
  on Windows
* [#93](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/93) - More consistency
  between `JRubyExec` and `project.jrubyexec` in the way the execution environment is prepared
  way

### Improvements

* [#92](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/92) - Support for building
  and testing Windows environments on Appveyor

## 0.1.10

* [#84](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/84) - Set
  sourceCompatibility to 1.6
* [#89](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/89) -
  Upgrade default JRuby version to 1.7.17
* [#90](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/90) -
  JRubyExec should overwrite/clean the environment of RVM settings
* [#91](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/91) -
  JRubyExec task not executing because it's always "up to date"

## 0.1.9

* [#73](https://github.com/jruby-gradle/jruby-gradle-plugin/issues/73) - Allow
  user to override GEM directory for `JRubyExec` and `project.jrubyexec`.
  Issues addressed that broken 0.1.6 and 0.1.7

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

