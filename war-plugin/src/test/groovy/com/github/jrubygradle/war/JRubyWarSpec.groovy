package com.github.jrubygradle.war

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.*
import static org.gradle.api.logging.LogLevel.*

/**
 * @author R. Tyler Croy
 *
 */
class JRubyWarSpec extends Specification {
    static final String TASK_NAME = 'WarWarTask'

    def project
    def warTask

    void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.jruby-gradle.war'
        warTask = project.task(TASK_NAME, type: JRubyWar)
    }

    def "basic sanity check"() {
        expect: "warTask to be an instance"
        warTask instanceof com.github.jrubygradle.war.JRubyWar
        project.tasks.jrubyWar.group == 'JRuby'
    }
}
