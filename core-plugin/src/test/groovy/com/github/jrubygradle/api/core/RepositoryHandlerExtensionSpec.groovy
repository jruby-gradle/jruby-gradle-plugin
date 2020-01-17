package com.github.jrubygradle.api.core

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification


class RepositoryHandlerExtensionSpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    void 'Add Maven repository'() {
        when:
        project.allprojects {
            apply plugin : JRubyCorePlugin

            repositories {
                ruby {
                    mavengems()
                    mavengems('https://goo1')
                    mavengems('goo2','https://goo2')
                }
            }
        }

        then:
        project.repositories.size() == 3
    }
}