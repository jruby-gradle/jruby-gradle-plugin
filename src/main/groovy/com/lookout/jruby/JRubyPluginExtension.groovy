package com.lookout.jruby

import org.gradle.api.Incubating
import org.gradle.api.Project

class JRubyPluginExtension {
    // More details here: <http://rubygems-proxy.torquebox.org/>
//    String defaultGemRepo = 'http://rubygems-proxy.torquebox.org/releases'
//    String gemrepo_url = defaultGemRepo
    String gemInstallDir = 'vendor'

    /** The default version of jruby that will be used by jrubyWar
     *
     */
    String defaultVersion = '1.7.13'

    /** The version of jruby used by jrubyexec as well as default version of jruby that will be used by JRubyExec
     *
     */
    String execVersion = defaultVersion

    /** Set this to false if you do not want the default set of repositories to be loaded.
     *
     */
    @Incubating
    boolean defaultRepositories = true

    JRubyPluginExtension(Project p) {
        project = p
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
}
