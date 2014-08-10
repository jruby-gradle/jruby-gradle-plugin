package com.lookout.gradle.jruby

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.War

import org.gradle.api.file.FileTree

class JRubyPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.repositories {
            maven {
                // More details here: <http://rubygems-proxy.torquebox.org/>
                url "http://rubygems-proxy.torquebox.org/releases"
            }
        }

        // Set up a special configuration group for our embedding jars
        project.configurations {
            jrubyEmbeds
        }

        // In order for jrubyWar to work we'll need to pull in the warbler
        // bootstrap code from this artifact
        project.dependencies {
            jrubyEmbeds group: 'com.lookout', name: 'warbler-bootstrap', version: '1.+'
        }

        project.task('jrubyCacheGems', type: Copy) {
            description 'Copy gems from the runtime dependencies into .gemcache/'
            from project.configurations.runtime
            into '.gemcache'
            include '**/*.gem'
        }

        project.task('jrubyPrepareGems') {
            description 'Prepare the gems from the runtime dependencies, extracts into vendor/'
            dependsOn project.tasks.jrubyCacheGems

            doLast {
                project.fileTree(dir: '.gemcache/',
                                 include: '*.gem').each { File f ->
                    extractGem(project, f)
                }
            }
        }

        project.task('jrubyCacheJars', type: Copy) {
            description 'Cache .jar-based dependencies into .jarcache/'
            from project.configurations.runtime
            into ".jarcache"
            include '**/*.jar'
        }

        project.task('jrubyPrepare') {
            description 'Pre-cache and prepare all dependencies (jars and gems)'
            dependsOn project.tasks.jrubyCacheJars, project.tasks.jrubyPrepareGems
        }

        project.task('jrubyWar', type: War) {
            dependsOn project.tasks.jrubyPrepare

            from "$project.buildDir/classes/main"
            // Bring our vendored gems into the created war file
            webInf {
                from 'vendor'
                into 'gems'
            }

            // note that zipTree call is wrapped in closure so that configuration
            // is only resolved at execution time. This will take the embeds
            // from within the `jrubyEmbeds` configuration and dump them into the war
            from {
                project.configurations.jrubyEmbeds.collect { project.zipTree(it) }
            }

            // By adding the WarMain class as the main-class we can have a
            // runnable war
            manifest { attributes 'Main-Class' : 'com.lookout.jruby.WarMain' }
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
