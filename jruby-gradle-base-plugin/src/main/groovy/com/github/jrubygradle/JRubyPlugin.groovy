package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecDelegate
import com.github.jrubygradle.internal.GemVersionResolver
import com.github.jrubygradle.internal.JRubyExecUtils
import com.github.jrubygradle.internal.RubygemsServlet
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *
 */
class JRubyPlugin implements Plugin<Project> {
    static final String TASK_GROUP_NAME = 'JRuby'
    static final String RUBYGEMS_ORG_URL = 'https://rubygems.org'
    static final String RUBYGEMS_RELEASE_URL = 'http://rubygems.lasagna.io/proxy/maven/releases'
    static final String VERSION_PROPERTY = 'jrubyVersion'

    void apply(Project project) {
        project.extensions.create('jruby', JRubyPluginExtension, project)

        if (project.hasProperty(VERSION_PROPERTY)) {
            project.jruby.defaultVersion project.properties.get(VERSION_PROPERTY)
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
                String localUrl = repoUrl ?: RUBYGEMS_ORG_URL
                // can not cast
                Object embedded = embeddedServer()
                String path = embedded.addRepository(localUrl)
                project.logger.info( 'Adding remote rubygems repo: {}', localUrl)
                maven {
                    url {
                        startEmbeddedServer(project)
                        embedded.getURL(path)
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

    // can not cast same object from different classloaders
    private Object server
    private Object embeddedServer() {
        if (server == null) {
            // TODO maybe things will work now without cloning
            // clone the current classloader without its parent
            //
            // assume we run inside an URLClassLoader which might
            // not always be the case (OSGi, J2EE, etc)
            List<URL> urls = [] as Queue
            URL warFileURL
            RubygemsServlet.classLoader.URLs.each {
                if (it.file.endsWith('.war') ) {
                    warFileURL = it
                }
                // for integTest we need to filter some jars here
                else if (!it.file.contains('bcprov-jdk15')) {
                    urls.add(it)
                }
            }
            ClassLoader extClassLoader = this.class.classLoader.systemClassLoader.parent
            ClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), extClassLoader)
            // can not cast
            Object servlet = cl.loadClass(RubygemsServlet.name)
            server = servlet.create(warFileURL)
        }
        server
    }

    private boolean serverStarted = false
    private void startEmbeddedServer(Project project) {
        if (server != null && !serverStarted) {
            // we need to set the current thread context class loader
            // for starting up the webapps
            ClassLoader current = Thread.currentThread().contextClassLoader
            try {
                Thread.currentThread().setContextClassLoader(server.class.classLoader)
                if (project.logger.isDebugEnabled()) {
                    server.enableLogging()
                }
                server.start()
                serverStarted = true
                project.gradle.buildFinished {
                    server.stop()
                    server.class.classLoader.close()
                    server = null
                }
            }
            finally {
                Thread.currentThread().setContextClassLoader(current)
            }
        }
    }
}
