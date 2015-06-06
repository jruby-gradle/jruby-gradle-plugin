package com.github.jrubygradle

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.*


/**
 * @author Schalk W. Cronjé
 */
class GemUtilsSpec extends Specification {

    static final File TESTROOT = new File("${System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests'}/gus")

    def project
    File src = new File(TESTROOT,'src')
    File dest = new File(TESTROOT,'dest')
    File fakeGem = new File(src,'gems/mygem-1.0')

    void setup() {
        project = ProjectBuilder.builder().build()
        project.buildDir = TESTROOT

        if(TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        src.mkdirs()
        dest.mkdirs()

        /* Creating a folder structure that should look like
         * src
         * src/cache
         * src/cache/cache.txt
         * src/gems
         * src/gems/mygem-1.0/fake.txt
         * src/gems/mygem-1.0/test
         * src/gems/mygem-1.0/test/test.txt
         */
        new File(fakeGem,'test').mkdirs()
        new File(fakeGem,'fake.txt').text = 'fake.content'
        new File(fakeGem,'test/test.txt').text = 'test.content'

        new File(src,'cache').mkdirs()
        new File(src,'cache/cache.txt').text = 'cache.content'
    }

    def "Attach a fake gem folder to a copy command"() {
        when:
            project.copy {
                into dest

                with GemUtils.gemCopySpec(project,src)
            }

        then:
            new File(dest,'gems/mygem-1.0/fake.txt').exists()
            !new File(dest,'gems/mygem-1.0/test/test.txt').exists()
            !new File(dest,'cache').exists()
    }

    def "Attach a fake gem folder to a copy command with fullGem=false"() {
        when:
            project.copy {
                into dest

                with GemUtils.gemCopySpec(project,src,fullGem:false)
            }

        then:
            new File(dest,'gems/mygem-1.0/fake.txt').exists()
            !new File(dest,'gems/mygem-1.0/test/test.txt').exists()
            !new File(dest,'cache').exists()
    }

    def "Attach a fake gem folder to a copy command with subfolder"() {
        when:
            project.copy {
                into dest

                with GemUtils.gemCopySpec(project,src,subfolder:'foo')
            }

        then:
            !new File(dest,'gems/mygem-1.0/fake.txt').exists()
            !new File(dest,'gems/mygem-1.0/test/test.txt').exists()
            !new File(dest,'cache').exists()
            new File(dest,'foo/gems/mygem-1.0/fake.txt').exists()
            !new File(dest,'foo/gems/mygem-1.0/test/test.txt').exists()
            !new File(dest,'foo/cache').exists()
    }

    def "Attach a fake gem folder to a copy command with fullGem=true"() {
        when:
            project.copy {
                into dest

                with GemUtils.gemCopySpec( project, src, fullGem : true )
            }

        then: "Expecting test and cache folders to be copied"
            new File(dest,'gems/mygem-1.0/fake.txt').exists()
            new File(dest,'gems/mygem-1.0/test/test.txt').exists()
            new File(dest,'cache/cache.txt').exists()
    }

    def "Attach a fake gem folder to a copy command with subfolder and fullGem=true"() {
        when:
            project.copy {
                into dest

                with GemUtils.gemCopySpec(project,src,subfolder:'foo',fullGem:true)
            }

        then:
            !new File(dest,'gems/mygem-1.0/fake.txt').exists()
            !new File(dest,'gems/mygem-1.0/test/test.txt').exists()
            !new File(dest,'cache').exists()
            new File(dest,'foo/gems/mygem-1.0/fake.txt').exists()
            new File(dest,'foo/gems/mygem-1.0/test/test.txt').exists()
            new File(dest,'foo/cache').exists()
    }
}