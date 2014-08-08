package com.lookout.gradle.jruby

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

import org.gradle.api.file.FileTree

class JRubyPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.repositories {
            maven {
                // More details here: <http://rubygems-proxy.torquebox.org/>
                url "http://rubygems-proxy.torquebox.org/releases"
            }
        }

        project.task('cachegems', type: Copy) {
            description 'Copy gems from the runtime dependencies into .gemcache/'
            from project.configurations.runtime
            into '.gemcache'
            include '**/*.gem'
        }

        project.task('preparegems') {
            description 'Prepare the gems from the runtime dependencies, extracts into vendor/'
            dependsOn project.tasks.cachegems

            doLast {
                project.fileTree(dir: '.gemcache/',
                                 include: '*.gem').each { File f ->
                    extractGem(project, f)
                }
            }
        }
    }

    /*
     * Take the given .gem filename (e.g. rake-10.3.2.gem) and just return the
     * gem "full name" (e.g. rake-10.3.2)
     */
    static String gemFullNameFromFile(String filename) {
        return filename.replaceAll(~".gem", "")
    }

    Boolean extractGem(Project p, File gem) {
        def gemname = gemFullNameFromFile(gem.getName())
        File extract_dir = new File("./vendor/gems/$gemname")

        if (extract_dir.exists()) {
            return
        }

        p.exec {
            executable "gem"
            args 'install', gem, '--install-dir=./vendor', '--no-ri', '--no-rdoc'
        }
    }
}
