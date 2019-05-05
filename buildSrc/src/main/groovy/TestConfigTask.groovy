import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CompileStatic
class TestConfigTask extends DefaultTask {

    TestConfigTask() {
        outputFile = project.file( "${project.buildDir}/.testconfig/jruby-gradle-testconfig.properties")

        onlyIf { testProperties.size() }
    }

    @Input
    final Map<String,String> testProperties = [:]

    void testProperties(Map<String,String> props) {
        this.testProperties.putAll(props)
    }

    @OutputFile
    File outputFile

    @TaskAction
    void exec() {
        Properties props = new Properties()
        props.putAll(testProperties)
        outputFile.withOutputStream { strm ->
            props.store(strm,'')
        }
    }
}
