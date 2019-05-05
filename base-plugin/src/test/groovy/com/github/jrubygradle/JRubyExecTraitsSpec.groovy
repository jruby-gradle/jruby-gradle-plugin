package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecTraits
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 */
class JRubyExecTraitsSpec extends Specification {
    Project project
    Task task
    final String taskName = 'abstract-spock-task'

    void setup() {
        project = ProjectBuilder.builder().build()
        task = project.task(taskName, type: SpockJRubyExecTraitsTask)
    }

    void "Prepare a basic environment"() {
        when:
        Map preparedEnv = task.getPreparedEnvironment([:])

        then:
        preparedEnv.size() > 0
    }

    void "Filter out RVM environment values by default"() {
        when:
        Map preparedEnv = task.getPreparedEnvironment([
                'GEM_HOME'       : '/tmp/spock',
                'RUBY_VERSION'   : 'notaversion',
                'rvm_ruby_string': 'jruby-head',
        ])

        then:
        preparedEnv['GEM_HOME'] != '/tmp/spock'
        !preparedEnv.containsKey('rvm_ruby_string')
    }

    void "Avoid filtering out the RVM environment if inheritRubyEnv == true"() {
        given:
        task.inheritRubyEnv true

        when:
        Map preparedEnv = task.getPreparedEnvironment([
                'GEM_PATH': '/tmp/spock/invalid',
        ])

        then:
        preparedEnv.containsKey('GEM_PATH')
    }

    @SuppressWarnings('UnnecessaryGetter')
    void "setting gemWorkDir should work"() {
        given:
        String workDir = 'customGemDir'

        when:
        project.configure(task) {
            gemWorkDir workDir
        }

        then:
        task.gemWorkDir == task.getGemWorkDir()
        task.gemWorkDir.absolutePath.endsWith(workDir)
    }

    @SuppressWarnings('UnnecessaryGetter')
    void "setting gemWorkDir with traits"() {
        given:
        String workDir = 'customGemDir'
        task = project.task('spock', type: SpockJRubyExecTraitsTask)

        when:
        project.configure(task) {
            gemWorkDir workDir
        }

        then:
        task.gemWorkDir == task.getGemWorkDir()
        task.gemWorkDir.absolutePath.endsWith(workDir)
    }

    static class SpockJRubyExecTraitsTask extends DefaultTask implements JRubyExecTraits {
    }

}
