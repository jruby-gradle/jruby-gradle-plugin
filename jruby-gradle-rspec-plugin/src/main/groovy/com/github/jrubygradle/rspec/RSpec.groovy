package com.github.jrubygradle.rspec

import com.github.jrubygradle.GemUtils
import com.github.jrubygradle.internal.JRubyExecUtils
import groovy.transform.PackageScope
import org.gradle.api.Incubating
import org.gradle.api.InvalidUserDataException
import org.gradle.api.DefaultTask
import org.gradle.api.Project
//import org.gradle.api.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * @author Christian Meier
 */
class RSpec extends DefaultTask {

  static final String DEFAULT_VERSION = '3.3.0'
  
    String version

    @Input
    String jrubyVersion = project.jruby.defaultVersion

    void jrubyVersion(String version) {
      this.jrubyVersion = version
    }

    RSpec(){
      version = DEFAULT_VERSION
    }

    @TaskAction
    void run() {
        def config = project.configurations.getByName(name)
        File jrubyJar = JRubyExecUtils.jrubyJar(config)
        File gemDir = new File(project.buildDir, "${name}-gems")
        GemUtils.extractGems(project, jrubyJar, config, gemDir, GemUtils.OverwriteAction.SKIP)
        GemUtils.setupJars(config, gemDir, GemUtils.OverwriteAction.OVERWRITE)

        project.javaexec {
          // JRuby looks on the classpath inside the 'bin' directory
          // for executables
          classpath jrubyJar.absolutePath, gemDir.absolutePath
          
          main 'org.jruby.Main'

          //TODO args '-I' + JRubyExec.jarDependenciesGemLibPath(gemDir)
          args '-rjars/setup', '-S','rspec'

          environment 'GEM_HOME' : gemDir.absolutePath
          environment 'GEM_PATH' : gemDir.absolutePath
          environment 'JARS_HOME' : new File(gemDir.absolutePath, 'jars')
          environment 'JARS_LOCK' : new File(gemDir.absolutePath, 'Jars.lock')
        }
    }
}
