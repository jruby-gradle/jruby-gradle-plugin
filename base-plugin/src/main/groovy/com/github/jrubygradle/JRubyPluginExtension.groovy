package com.github.jrubygradle

import com.github.jrubygradle.core.JRubyAwareTask
import com.github.jrubygradle.core.RepositoryHandlerExtension
import com.github.jrubygradle.internal.JRubyPrepareUtils
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolutionStrategy
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.ysb33r.grolifant.api.AbstractCombinedProjectTaskExtension

import java.util.concurrent.Callable

import static org.ysb33r.grolifant.api.StringUtils.stringize

/**
 * Class providing the jruby{} DSL extension to the Gradle build script
 */
@CompileStatic
class JRubyPluginExtension extends AbstractCombinedProjectTaskExtension {
    public static final String DEFAULT_JRUBY_VERSION = '9.2.7.0'

    public static final String NAME = 'jruby'

    /** Project extension constructor.
     *
     * @param p Project to attach extension to.
     */
    JRubyPluginExtension(Project p) {
        super(p)
    }

    /** Task extension constructor
     *
     * @param t Task to attach extension to.
     *
     * @since 2.0
     */
    JRubyPluginExtension(JRubyAwareTask t) {
        super(t, NAME)
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
     * @param v New version to be used. Can be of anything that be be resolved by {@link StringUtils.stringize ( Object o )}
     *
     * @since 2.0
     */
    void setJrubyVersion(Object v) {
        this.jrubyVersion = v
    }

    /** Set a new JRuby version to use.
     *
     * @param v New version to be used. Can be of anything that be be resolved by {@link StringUtils.stringize ( Object o )}
     *
     * @since 2.0
     */
    void jrubyVersion(Object v) {
        this.jrubyVersion = v
    }

//    void setDefaultVersion(String newDefaultVersion) {
//        defaultVersion = newDefaultVersion
//        defaultVersionCallbacks.each { Closure callback -> callback.call(defaultVersion) }
//        execVersionCallbacks.each { Closure callback ->
//            if (!isExecVersionModified) {
//                callback.call(defaultVersion)
//            }
//        }
//    }

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

    /** Set this to false if you do not want the default set of repositories to be loaded.
     *
     * @since 0.1.1
     */

    /** Legacy method for setting default repositories.
     *
     * @deprecated Default repositories are no longer set by default. People
     *   who still need to use this feature must explicitly call this method, but
     *   are encouraged to migrate to using the {@code repositories} block instead.
     *
     * @param value {@code true} to enabled default repositories
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
                project.repositories.jcenter()
                ((ExtensionAware) (project.repositories)).extensions.getByType(RepositoryHandlerExtension).gems()
            } else {
                deprecated(
                    'jruby.defaultRepositories are no longer switched on by default - you can safely remove ' +
                        'this setting.'
                )
            }
        }
    }

//    /** Resolves the currently configured Jars installation directory.
//     *
//     * @return Install directory as an absolute path
//     * @since 0.1.16
//     */
//    @Deprecated
//    File getJarInstallDir() {
//        project.file(this.jarInstallDir).absoluteFile
//    }
//
//    /** Sets the jar installation directory. Anything that can be passed to {@code project.file} can be
//     * passed here as well.
//     *
//     * @param dir Directory (String, GString, File, Closure etc.)
//     * @return The passed object.
//     * @deprecated Setting a custom jarInstallDir can cause dependencies to
//     *  overlap and unexpected behavior. Please use Configurations instead.
//     *
//     * @since 0.1.16
//     */
//    @Deprecated
//    Object setJarInstallDir(Object dir) {
//        this.jarInstallDir = dir
//    }

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

        Configuration configuration = project.configurations.detachedConfiguration(
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
                this.gemConfiguration = project.provider({ -> c } as Callable<Configuration>)
                registerPrepareTask(((Configuration) c).name)
                break
            case CharSequence:
                this.gemConfiguration = project.provider(
                    { -> project.configurations.getByName(c.toString()) } as Callable<Configuration>
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

//    /**
//     * Register a callback to be invoked when defaultVersion is updated
//     *
//     * NOTE: This is primarily meant for JRuby/Gradle plugin developers
//     *
//     * @param callback
//     * @since 1.1.0
//     */
//    @Incubating
//    void registerDefaultVersionCallback(Closure callback) {
//        defaultVersionCallbacks.add(callback)
//    }

    /** Return all of the resolution strategies that are related to this extension.
     *
     * @return List of resolution strategy actions to perform.
     */
    protected List<Action<ResolutionStrategy>> getAllResolutionStrategyActions() {
        if (task) {
            if (taskResolutionStrategiesOnly) {
                this.resolutionsStrategies
            } else {
                getExtFromProject().allResolutionStrategyActions + this.resolutionsStrategies
            }
        } else {
            this.resolutionsStrategies
        }
    }

    private JRubyPluginExtension getExtFromProject() {
        task ? (JRubyPluginExtension) projectExtension : this
    }

    private void deprecated(String msg) {
        project.logger.info("Deprecated feature in ${NAME} extension. ${msg}")
    }

    private Dependency createDependency(final String notation, final Closure configurator = null) {
        if (configurator) {
            project.dependencies.create(notation, configurator)
        } else {
            project.dependencies.create(notation)
        }
    }

//    /** Directory for jrubyPrepare to install GEM dependencies into */
//    @Deprecated
//    private Object gemInstallDir
//
//    /** Directory for jrubyPrepare to install JAR dependencies into */
//    @Deprecated
//    private Object jarInstallDir

//    /** List of callbacks to invoke when jruby.defaultVersion is modified */
//    private final List<Closure> defaultVersionCallbacks = []
//
//    /** List of callbacks to invoke when jruby.execVersion is modified */
//    private final List<Closure> execVersionCallbacks = []
//
//    private Boolean isExecVersionModified = false

    private void registerPrepareTask(final String configurationName) {
        JRubyPrepareUtils.registerPrepareTask(project, configurationName)
        this.gemPrepareTaskName = JRubyPrepareUtils.taskName(configurationName)
    }

    private static final String JRUBY_COMPLETE_DEPENDENCY = 'org.jruby:jruby-complete'
    private Object jrubyVersion = DEFAULT_JRUBY_VERSION

    private Provider<Configuration> gemConfiguration
    private String gemPrepareTaskName
    private boolean taskResolutionStrategiesOnly = false
    private final List<Action<ResolutionStrategy>> resolutionsStrategies = []

    private boolean defaultRepositoriesCalled = false
}
