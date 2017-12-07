package com.github.jrubygradle

import org.gradle.api.Incubating
import org.gradle.api.Project

/**
 * Class providing the jruby{} DSL extension to the Gradle build script
 */
class JRubyPluginExtension {
    static final String DEFAULT_JRUBY_VERSION = '9.1.15.0'

    /** The default version of jruby that will be used  */
    String defaultVersion = DEFAULT_JRUBY_VERSION

    /**
     * The version of jruby used by jrubyexec as well as default version of jruby that will be used by JRubyExec
     * @deprecated Setting execVersion is not very often done and should probably
     *  be avoided. Set jrubyVersion on your JRubyExec tasks instead
     */
    @Deprecated
    String execVersion = defaultVersion

    /** Set this to false if you do not want the default set of repositories to be loaded.
     *
     * @since 0.1.1
     */
    @Incubating
    boolean defaultRepositories = true

    void setDefaultVersion(String newDefaultVersion) {
        defaultVersion = newDefaultVersion
        defaultVersionCallbacks.each { Closure callback -> callback.call(defaultVersion) }
        execVersionCallbacks.each { Closure callback ->
            if (!isExecVersionModified) {
                callback.call(defaultVersion)
            }
        }
    }

    /**
     * Set the default version of JRuby to be used by all JRuby/Gradle code
     *
     * @param newDefaultVersion
     * @since 1.1.0
     */
    void defaultVersion(String newDefaultVersion) {
        setDefaultVersion(newDefaultVersion)
    }

    /** Resolves the currently configured GEM installation directory.
     *
     * @return Install directory as an absolute path
     * @since 0.1.12
     */
    File getGemInstallDir() {
        project.file(this.gemInstallDir).absoluteFile
    }

    /** Sets the gem installation directory. Anything that can be passed to {@code project.file} can be
     * passed here as well.
     *
     * @param dir Directory (String, GString, File, Closure etc.)
     * @return The passed object.
     * @since 0.1.12
     * @deprecated Setting a custom gemInstallDir can cause dependencies to
     *  overlap and unexpected behavior. Please use Configurations instead
     */
    @Deprecated
    Object setGemInstallDir(Object dir) {
        this.gemInstallDir = dir
    }

    /** Resolves the currently configured Jars installation directory.
     *
     * @return Install directory as an absolute path
     * @since 0.1.16
     */
    @Deprecated
    File getJarInstallDir() {
        project.file(this.jarInstallDir).absoluteFile
    }

    /** Sets the jar installation directory. Anything that can be passed to {@code project.file} can be
     * passed here as well.
     *
     * @param dir Directory (String, GString, File, Closure etc.)
     * @return The passed object.
     * @since 0.1.16
     * @deprecated Setting a custom jarInstallDir can cause dependencies to
     *  overlap and unexpected behavior. Please use Configurations instead
     */
    @Deprecated
    Object setJarInstallDir(Object dir) {
        this.jarInstallDir = dir
    }

    JRubyPluginExtension(Project p) {
        project = p
        this.gemInstallDir = { new File(p.buildDir, 'gems').absolutePath }
        this.jarInstallDir = { new File(p.buildDir, 'jars').absolutePath }
    }

    /** Change the version of jruby for jrubyexec and JRubyExec
     *
     * @param newVersion
     * @deprecated Setting execVersion is not very often done and should probably
     *  be avoided. Set jrubyVersion on your JRubyExec tasks instead
     */
    @Deprecated
    void setExecVersion(final String newVersion) {
        execVersion = newVersion
        isExecVersionModified = true

        project.tasks.withType(JRubyExec).each { JRubyExec t ->
            t.jrubyVersion = newVersion
        }

        execVersionCallbacks.each { Closure callback ->
            callback.call(execVersion)
        }
    }

    /**
     * Register a callback to be invoked when defaultVersion is updated
     *
     * NOTE: This is primarily meant for JRuby/Gradle plugin developers
     *
     * @param callback
     * @since 1.1.0
     */
    @Incubating
    void registerDefaultVersionCallback(Closure callback) {
        defaultVersionCallbacks.add(callback)
    }

    /**
     * Register a callback to be invoked when execVersion is updated
    *
    * This is primarily meant for JRuby/Gradle plugin developers. You can expect
    * that this callback will be executed if defaultVersion is changed but
    * execVersion is not changed.
    *
    * @param callback
    * @since 1.1.0
    */
    @Incubating
    void registerExecVersionCallback(Closure callback) {
        execVersionCallbacks.add(callback)
    }

    private final Project project

    /** Directory for jrubyPrepare to install GEM dependencies into */
    private Object gemInstallDir

    /** Directory for jrubyPrepare to install JAR dependencies into */
    private Object jarInstallDir

    /** List of callbacks to invoke when jruby.defaultVersion is modified */
    private final List<Closure> defaultVersionCallbacks = []

    /** List of callbacks to invoke when jruby.execVersion is modified */
    private final List<Closure> execVersionCallbacks = []

    private Boolean isExecVersionModified = false
}
