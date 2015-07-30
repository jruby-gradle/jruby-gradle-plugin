  package com.github.jrubygradle

import org.gradle.api.file.DuplicateFileCopyingException
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

    def "write Jars.lock"() {
        when:
            GemUtils.OverwriteAction.values().each {
                File jarsLock = new File(dest, "jars-${it}.lock")
                jarsLock.delete()
                GemUtils.writeJarsLock(jarsLock, [ 'something' ], it)
            }

        then:
            new File(dest, "jars-FAIL.lock").text =~ /something/
            new File(dest, "jars-SKIP.lock").text =~ /something/
            new File(dest, "jars-OVERWRITE.lock").text =~ /something/
    }

    def "skip write Jars.lock"() {
        when:
            File jarsLock = new File(dest, "jars.lock")
            jarsLock << ''
            GemUtils.writeJarsLock(jarsLock, [ 'something' ], GemUtils.OverwriteAction.SKIP)

        then:
            new File(dest, "jars.lock").length() == 0
    }

    def "overwrite write Jars.lock"() {
        when:
            File jarsLock = new File(dest, "jars.lock")
            jarsLock << ''
            GemUtils.writeJarsLock(jarsLock, [ 'something' ], GemUtils.OverwriteAction.OVERWRITE)

        then:
            new File(dest, "jars.lock").text =~ /something/
    }

    def "fail write Jars.lock"() {
        when:
            File jarsLock = new File(dest, "jars.lock")
            jarsLock << ''
            GemUtils.writeJarsLock(jarsLock, [ 'something' ], GemUtils.OverwriteAction.FAIL)

        then:
            thrown(DuplicateFileCopyingException)
    }

    def "rewrite jar dependency"() {
        when:
            File jars = new File(dest, 'jars')
            GemUtils.OverwriteAction.values().each {
                File jar = new File(dest, "${it}.jar")
                jar << 'something'
                GemUtils.rewriteJarDependencies(jars, [jar], ['FAIL.jar':'fail.jar', 'SKIP.jar':'skip.jar', 'OVERWRITE.jar':'over.jar'], it)
            }

        then:
            new File(jars, "fail.jar").length() == 9
            new File(jars, "skip.jar").length() == 9
            new File(jars, "over.jar").length() == 9
    }

    def "skip rewrite jars dependency"() {
        when:
            File jars = new File(dest, 'jars')
            File jar1 = new File(dest, "jar1.jar")
            File jar2 = new File(dest, "jar2.jar")
            File target1 = new File(jars, "jar1.jar")
            File target2 = new File(jars, "jar2.jar")
            jar1 << 'something'
            jar2 << 'something'
            jars.mkdir()
            target1 << ''
            target2.delete()
            GemUtils.rewriteJarDependencies(jars, [jar1, jar2],
                                            ['jar1.jar':'jar1.jar', 'jar2.jar':'jar2.jar'],
                                            GemUtils.OverwriteAction.SKIP)

        then:
            target1.length() == 0
            target2.length() == 9
    }

    def "overwrite rewrite jars dependency"() {
        when:
            File jars = new File(dest, 'jars')
            File jar1 = new File(dest, "jar1.jar")
            File jar2 = new File(dest, "jar2.jar")
            File target1 = new File(jars, "jar1.jar")
            File target2 = new File(jars, "jar2.jar")
            jar1 << 'something'
            jar2 << 'something'
            jars.mkdir()
            target1 << ''
            target2.delete()
            GemUtils.rewriteJarDependencies(jars, [jar1, jar2],
                                            ['jar1.jar':'jar1.jar', 'jar2.jar':'jar2.jar'],
                                            GemUtils.OverwriteAction.OVERWRITE)

        then:
            target1.length() == 9
            target2.length() == 9
    }

    def "fail rewrite jars dependency"() {
        when:
            File jars = new File(dest, 'jars')
            File jar = new File(dest, "jar.jar")
            File target = new File(jars, "jar.jar")
            jar << 'something'
            jars.mkdir()
            target << ''
            GemUtils.rewriteJarDependencies(jars, [jar], ['jar.jar':'jar.jar'], GemUtils.OverwriteAction.FAIL)

        then:
            thrown(DuplicateFileCopyingException)
    }

    def "gemFullNameFromFile() should prune .gem"() {
        given:
        String filename = "rake-10.3.2.gem"
        String gem_name = "rake-10.3.2"

        expect:
        gem_name == GemUtils.gemFullNameFromFile(filename)
    }
}
