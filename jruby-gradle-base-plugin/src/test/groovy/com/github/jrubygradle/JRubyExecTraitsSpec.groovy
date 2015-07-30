package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecTraits
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.*

class SpockJRubyExecTraitsTask extends DefaultTask implements JRubyExecTraits {
}

/**
 */
class JRubyExecTraitsSpec extends Specification {
    protected Project project
    protected Task task
    final String taskName = 'abstract-spock-task'

    def setup() {
        project = ProjectBuilder.builder().build()
        task = project.task(taskName, type: SpockJRubyExecTraitsTask)
    }

    def "Prepare a basic environment"() {
        when:
        Map preparedEnv = task.getPreparedEnvironment([:])

        then:
        preparedEnv.size() > 0
    }

    def "Filter out RVM environment values by default"() {
        when:
        Map preparedEnv = task.getPreparedEnvironment([
                'GEM_HOME' : '/tmp/spock',
                'RUBY_VERSION' : 'notaversion',
                'rvm_ruby_string' : 'jruby-head',
        ])

        then:
        preparedEnv['GEM_HOME'] != '/tmp/spock'
        !preparedEnv.containsKey('rvm_ruby_string')
    }

    def "Avoid filtering out the RVM environment if inheritRubyEnv == true"() {
        given:
        task.inheritRubyEnv true

        when:
        Map preparedEnv = task.getPreparedEnvironment([
                'GEM_PATH' : '/tmp/spock/invalid',
        ])

        then:
        preparedEnv.containsKey('GEM_PATH') }

    def "setting gemWorkDir should work"() {
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

    def "setting gemWorkDir with traits"() {
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
}
