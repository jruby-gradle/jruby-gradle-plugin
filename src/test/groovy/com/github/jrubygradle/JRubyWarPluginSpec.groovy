package com.github.jrubygradle.war

import org.gradle.api.Task
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.War
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import spock.lang.*


import static org.gradle.api.logging.LogLevel.LIFECYCLE
import static org.junit.Assert.assertTrue

/**
 * @author R. Tyler Croy
 *
 */
class JRubyWarPluginSpec extends Specification {

    def project
    def warTask

    void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.jruby-gradle.war'

    }

    def "basic sanity check"() {
        expect:
            project.tasks.jrubyWar.group == 'JRuby'
            project.tasks.jrubyWar instanceof War
            project.tasks.jrubyCacheJars instanceof Copy
            project.tasks.jrubyCleanJars instanceof Delete
    }

    def "jrubyPrepare should depend on jrubyCacheJars"() {
        expect:
            assertTrue(taskContainsDependency(project.tasks.jrubyPrepare,
                        'jrubyCacheJars'))
    }

    def 'clean should depend on jrubyCleanJars'() {
        expect:
            assertTrue(taskContainsDependency(project.tasks.clean, 'jrubyCleanJars'))
    }


    private Boolean taskContainsDependency(Task task, String taskName) {
        Boolean status = false
        task.taskDependencies.values.each {
            if (it instanceof Task) {
                if (taskName == it.name) {
                    status = true
                }
            }
        }
        return status
    }
}
