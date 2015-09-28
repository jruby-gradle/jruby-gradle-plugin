package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecDelegate
import com.github.jrubygradle.internal.GemVersionResolver
import com.github.jrubygradle.internal.JRubyExecUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency

/**
 *
 */
class JRubyPlugin implements Plugin<Project> {
    static final String TASK_GROUP_NAME = 'JRuby'
    static final String RUBYGEMS_ORG_URL = 'https://rubygems.org'

    static final String RUBYGEMS_RELEASE_URL = 'http://rubygems.lasagna.io/proxy/maven/releases'

    void apply(Project project) {
        project.extensions.create('jruby', JRubyPluginExtension, project)

        if (project.hasProperty('jrubyVersion')) {
            project.jruby.defaultVersion project.properties.get('jrubyVersion')
        }

        setupRubygemsRepositories(project)
        setupRubygemsRelease(project)

        // Set up a special configuration group for our embedding jars
        project.configurations.create('gems')
        project.configurations.create(JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG)
        JRubyExecDelegate.addToProject(project)

        project.afterEvaluate {
            if (project.jruby.defaultRepositories) {
                project.repositories {
                    jcenter()
                    rubygemsRelease()
                }
            }
            GemVersionResolver.setup(project)
            JRubyExec.updateJRubyDependencies(project)
        }

        project.task('jrubyPrepare', type: JRubyPrepare) {
            group TASK_GROUP_NAME
            description 'Prepare the gems/jars from the `gem` dependencies, extracts the gems into jruby.installGemDir and sets up the jars in jruby.installGemDir/jars'
            dependencies project.configurations.gems
            outputDir project.jruby.gemInstallDir
        }

        project.task('generateGradleRb', type: GenerateGradleRb) {
            group TASK_GROUP_NAME
            description 'Generate a gradle.rb stub for executing Ruby binstubs'
            dependsOn project.tasks.jrubyPrepare
        }
    }

    // taken from https://github.com/Ullink/gradle-repositories-plugin/blob/master/src/main/groovy/com/ullink/RepositoriesPlugin.groovy
    boolean setupRubygemsRepositories(Project project) {
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'rubygems', String, Object)) {
            project.logger.debug 'Adding rubygems(String?) method to project RepositoryHandler'
            project.repositories.metaClass.rubygems << { String repoUrl = null ->
                repoUrl = repoUrl ?: RUBYGEMS_ORG_URL
                setupMavenGemProtocol(project)
                project.logger.info 'Adding remote rubygems repo: ' + repoUrl
                maven {
                    url {
                        // TODO put the postfix into static final String
                        "mavengem:" + repoUrl + "/maven/releases"
                    }
                }
            }
            return true
        }
    }

    boolean setupRubygemsRelease(Project project) {
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'rubygemsRelease')) {
            project.repositories.metaClass.rubygemsRelease << { ->
                maven { url RUBYGEMS_RELEASE_URL }
            }
        }
    }

    private void setupMavenGemProtocol(Project project) {
        if (! System.getProperties().contains("com.github.jrubygradle.internal")) {
            // TODO put version into private static final
            // load the mavengem protocol jars and puts them all into their
            // dedicated classloader separated from this plugin.
            Dependency dep = project.dependencies.create("org.sonatype.nexus.plugins:nexus-ruby-tools:2.11.4-01")

            // use a detached configuration since it is just for resolving
            // the jars and not needed any time later
            Configuration config = project.configurations.detachedConfiguration(dep)
            Set<File> urls = config.resolve()
            urls << new File(this.getClass().getClassLoader().getURLs().find {
                                 it.toString().contains('jruby-gradle-plugin')
                             }.path)

            // use the extension classloader as parent for our mavengem code
            ClassLoader extClassLoader = ClassLoader.getSystemClassLoader().parent
            ClassLoader cl = new URLClassLoader(urls.collect { it.toURL() }.toArray(new URL[urls.size()]), extClassLoader)

            // can not cast between different classloaders
            Object handler = cl.loadClass("com.github.jrubygradle.internal.mavengem.Handler")
            handler.registerMavenGemProtocol()
        }
    }
}
