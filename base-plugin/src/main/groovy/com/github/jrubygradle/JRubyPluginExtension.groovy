/*
 * Copyright (c) 2014-2020, R. Tyler Croy <rtyler@brokenco.de>,
 *     Schalk Cronje <ysb33r@gmail.com>, Christian Meier, Lookout, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.jrubygradle

import com.github.jrubygradle.api.core.JRubyAwareTask
import com.github.jrubygradle.api.core.RepositoryHandlerExtension
import com.github.jrubygradle.internal.JRubyPrepareUtils
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolutionStrategy
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskContainer
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.AbstractCombinedProjectTaskExtension
import org.ysb33r.grolifant.api.v4.TaskProvider

import java.util.concurrent.Callable

import static com.github.jrubygradle.JRubyPlugin.TASK_GROUP_NAME
import static org.ysb33r.grolifant.api.v4.StringUtils.stringize

/**
 * Class providing the jruby DSL extension to the Gradle build script.
 *
 * @author Schalk W. Cronjé
 * @author R Tyler Croy
 * @author Christian Meier
 *
 */
@CompileStatic
class JRubyPluginExtension extends AbstractCombinedProjectTaskExtension {
    public static final String DEFAULT_JRUBY_VERSION = '9.3.4.0'

    public static final String NAME = 'jruby'

    /** Project extension constructor.
     *
     * @param p Project to attach extension to.
     */
    JRubyPluginExtension(Project p) {
        super(p)
        this.jrubyVersion = DEFAULT_JRUBY_VERSION
        this.repositories = p.repositories
        this.dependencies = p.dependencies
        this.configurations = p.configurations
        this.providers = p.providers
        this.logger = p.logger
        this.tasks = p.tasks
        this.projectOperations = ProjectOperations.create(p)
    }

    /** Task extension constructor
     *
     * @param t Task to attach extension to.
     *
     * @since 2.0
     */
    JRubyPluginExtension(JRubyAwareTask t) {
        super(t, NAME)
        this.repositories = t.project.repositories
        this.dependencies = t.project.dependencies
        this.configurations = t.project.configurations
        this.providers = t.project.providers
        this.logger = t.project.logger
        this.tasks = t.project.tasks
        this.projectOperations = ProjectOperations.create(t.project)
    }

    /** The default version of jruby that will be used.
     *
     * @since 2.0
     */
    String getJrubyVersion() {
        if (task) {
            this.jrubyVersion ? stringize(this.jrubyVersion) : extFromProject.getJrubyVersion()
        } else {
            stringize(this.jrubyVersion)
        }
    }

    /** Set a new JRuby version to use.
     *
     * @param v New version to be used. Can be of anything that be be resolved by
     * {@link org.ysb33r.grolifant.api.v4.StringUtils#stringize ( Object o )}
     *
     * @since 2.0
     */
    void setJrubyVersion(Object v) {
        this.jrubyVersion = v
    }

    /** Set a new JRuby version to use.
     *
     * @param v New version to be used. Can be of anything that be be resolved by
     * {@link org.ysb33r.grolifant.api.v4.StringUtils#stringize ( Object o )}
     *
     * @since 2.0
     */
    void jrubyVersion(Object v) {
        this.jrubyVersion = v
    }

    /**
     *
     * @deprecated Use{@link #getJrubyVersion}
     */
    @Deprecated
    String getDefaultVersion() {
        deprecated('Use getJrubyVersion() rather than getDefaultVersion()')
        getJrubyVersion()
    }

    /**
     *
     * @deprecated Use{@link #setJrubyVersion}
     */
    @Deprecated
    void setDefaultVersion(String ver) {
        deprecated('Use setJrubyVersion(ver) rather than setDefaultVersion()')
        setJrubyVersion(ver)
    }

    /**
     * @deprecated Use{@link #jrubyVersion}
     */
    void defaultVersion(String newDefaultVersion) {
        deprecated('Use jrubyVersion(ver) rather than defaultVerion(ver)')
        setJrubyVersion(newDefaultVersion)
    }

    /** Legacy method for setting default repositories.
     *
     * @deprecated Default repositories are no longer set by default. People
     *   who still need to use this feature must explicitly call this method, but
     *   are encouraged to migrate to using the {@code repositories} block instead.
     *
     * @param value {@code true} to enabled default repositories
     *
     * @since 0.1.1
     */
    @Deprecated
    void setDefaultRepositories(boolean value) {
        if (!defaultRepositoriesCalled) {
            defaultRepositoriesCalled = true
            if (value) {
                deprecated(
                    'jruby.defaultRepositories will not be supported in a future version.' +
                        'It is recommended that you explicitly declare your repositories rather than rely on ' +
                        'this functionality.'
                )
                repositories.jcenter()
                ((ExtensionAware) (repositories)).extensions.getByType(RepositoryHandlerExtension).gems()
            } else {
                deprecated(
                    'jruby.defaultRepositories are no longer switched on by default - you can safely remove ' +
                        'this setting.'
                )
            }
        }
    }

    /** Clears the current list of resolution strategies.
     *
     * If done on a task it will break the link to any global reoslution strategies.
     *
     * @since 2.0
     */
    void clearResolutionStrategies() {
        this.resolutionsStrategies.clear()
        if (task) {
            taskResolutionStrategiesOnly = true
        }
    }

    /** Adds a resolution strategy for resolving jruby related dependencies
     *
     * @param strategy Additional resolution strategy. Takes a {@link org.gradle.api.artifacts.ResolutionStrategy} as parameter.
     *
     * @since 2.0
     */
    void resolutionStrategy(Action<ResolutionStrategy> strategy) {
        this.resolutionsStrategies.add(strategy)
    }

    /** Adds a resolution strategy for resolving jruby related dependencies
     *
     * @param strategy Additional resolution strategy. Takes a {@link ResolutionStrategy} as parameter.
     *
     * @since 2.0
     */
    void resolutionStrategy(@DelegatesTo(ResolutionStrategy) Closure strategy) {
        this.resolutionsStrategies.add(strategy as Action<ResolutionStrategy>)
    }

    /** Returns a runConfiguration of the configured JRuby core dependencies.
     *
     * @return A non-attached runConfiguration.
     */
    Configuration getJrubyConfiguration() {
        final String jrubyVer = getJrubyVersion()
        final String jrubyCompleteDep = "${JRUBY_COMPLETE_DEPENDENCY}:${jrubyVer}"

        List<Dependency> deps = [createDependency(jrubyCompleteDep)]

        Configuration configuration = configurations.detachedConfiguration(
            deps.toArray() as Dependency[]
        )

        allResolutionStrategyActions.each {
            configuration.resolutionStrategy(it)
        }

        configuration
    }

    /** Sets the GEM configuration.
     *
     * @param c Configuration instance, Character sequence as configuration name, or a {@code Provider<Configuration}.
              */
    void setGemConfiguration(final Object c) {
        switch (c) {
            case Configuration:
                this.gemConfiguration = providers.provider({ -> c } as Callable<Configuration>)
                registerPrepareTask(((Configuration) c).name)
                break
            case CharSequence:
                this.gemConfiguration = projectOperations.provider(
                    { -> configurations.getByName(c.toString()) } as Callable<Configuration>
                )
                registerPrepareTask(c.toString())
                break
            case Provider:
                this.gemConfiguration = (Provider<Configuration>) c
                registerPrepareTask(((Provider<Configuration>) c).get().name)
                break
            default:
                throw new UnsupportedOperationException(
                    "${c.class.canonicalName} is not a supported type for converting to a Configuration"
                )
        }
    }

    /** Declarative way of setting the GEM configuration.
     *
     * @param c Configuration instance, Character sequence as configuration name, or a {@code Provider<Configuration}.
              */
    void gemConfiguration(final Object c) {
        setGemConfiguration(c)
    }

    /** Get configuration that is used for GEMs
     *
     * @return Configuration*
     * @since 2.0
     */
    Configuration getGemConfiguration() {
        if (!task) {
            if (this.gemConfiguration == null) {
                throw new UninitialisedParameterException(
                    "GEM configuration has not been set. Use ${NAME}.setConfiguration()"
                )
            }
            this.gemConfiguration.get()
        } else if (this.gemConfiguration) {
            this.gemConfiguration.get()
        } else {
            extFromProject.gemConfiguration
        }
    }

    /** Returns the name of the prepare task that is associated with the GEM configuration.
     *
     * @return Task name.
     */
    String getGemPrepareTaskName() {
        if (task) {
            this.gemPrepareTaskName ?: extFromProject.gemPrepareTaskName
        } else {
            this.gemPrepareTaskName
        }
    }

    /** Return all of the resolution strategies that are related to this extension.
     *
     * @return List of resolution strategy actions to perform.
     */
    protected List<Action<ResolutionStrategy>> getAllResolutionStrategyActions() {
        if (task) {
            if (taskResolutionStrategiesOnly) {
                this.resolutionsStrategies
            } else {
                extFromProject.allResolutionStrategyActions + this.resolutionsStrategies
            }
        } else {
            this.resolutionsStrategies
        }
    }

    private JRubyPluginExtension getExtFromProject() {
        task ? (JRubyPluginExtension) projectExtension : this
    }

    private void deprecated(String msg) {
        logger.info("Deprecated feature in ${NAME} extension. ${msg}")
    }

    private Dependency createDependency(final String notation, final Closure configurator = null) {
        if (configurator) {
            dependencies.create(notation, configurator)
        } else {
            dependencies.create(notation)
        }
    }

    private void registerPrepareTask(final String configurationName) {
        final String taskName = JRubyPrepareUtils.taskName(configurationName)
        final String gemDir = JRubyPrepareUtils.gemRelativePath(configurationName)

        try {
            TaskProvider.taskByName(tasks, taskName)
        } catch (UnknownTaskException e) {
            TaskProvider<JRubyPrepare> prepare = TaskProvider.registerTask(tasks, taskName, JRubyPrepare)
            ProjectOperations po = this.projectOperations
            Action<JRubyPrepare> configurator = new Action<JRubyPrepare>() {
                void execute(JRubyPrepare jp) {
                    jp.with {
                        group = TASK_GROUP_NAME
                        description = "Prepare the gems/jars from the `${configurationName}` dependencies"
                        dependencies(project.configurations.getByName(configurationName))
                        outputDir =  { ->
                            po.buildDirDescendant(gemDir)
                        }
                    }
                }
            }
            prepare.configure(configurator as Action<? extends Task>)
            prepare
        }

        this.gemPrepareTaskName = taskName//JRubyPrepareUtils.taskName(configurationName)
    }

    private static final String JRUBY_COMPLETE_DEPENDENCY = 'org.jruby:jruby-complete'
    private Object jrubyVersion

    private Provider<Configuration> gemConfiguration
    private String gemPrepareTaskName
    private boolean taskResolutionStrategiesOnly = false
    private final List<Action<ResolutionStrategy>> resolutionsStrategies = []
    private final ProjectOperations projectOperations
    private final RepositoryHandler repositories
    private final ConfigurationContainer configurations
    private final DependencyHandler dependencies
    private final ProviderFactory providers
    private final TaskContainer tasks
    private final Logger logger
    private boolean defaultRepositoriesCalled = false
}
