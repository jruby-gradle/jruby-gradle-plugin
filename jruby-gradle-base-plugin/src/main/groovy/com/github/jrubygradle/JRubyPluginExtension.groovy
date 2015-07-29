package com.github.jrubygradle

import org.gradle.api.Incubating
import org.gradle.api.Project

class JRubyPluginExtension {
    /** The default version of jruby that will be used by jrubyWar
     *
     */
    String defaultVersion = '9.0.0.0'

    /** The version of jruby used by jrubyexec as well as default version of jruby that will be used by JRubyExec
     *
     */
    String execVersion = defaultVersion

    /** Set this to false if you do not want the default set of repositories to be loaded.
     *
     * @since 0.1.1
     */
    @Incubating
    boolean defaultRepositories = true

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
     */
    Object setGemInstallDir(Object dir) {
        this.gemInstallDir = dir
    }

    /** Resolves the currently configured Jars installation directory.
     *
     * @return Install directory as an absolute path
     * @since 0.1.16
     */
    File getJarInstallDir() {
        project.file(this.jarInstallDir).absoluteFile
    }

    /** Sets the jar installation directory. Anything that can be passed to {@code project.file} can be
     * passed here as well.
     *
     * @param dir Directory (String, GString, File, Closure etc.)
     * @return The passed object.
     * @since 0.1.16
     */
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
     */
    void setExecVersion(final String newVersion) {
        execVersion = newVersion

        project.tasks.withType(JRubyExec).each { t ->
            t.jrubyVersion = newVersion
        }
    }

    private Project project

    /** Directory for jrubyPrepare to install GEM dependencies into
     */
    private Object gemInstallDir

    /** Directory for jrubyPrepare to install JAR dependencies into
     */
    private Object jarInstallDir

}
