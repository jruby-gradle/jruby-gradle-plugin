import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer

@CompileStatic
class TestConfigPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {

        project.apply plugin: GroovyPlugin

        SourceSetContainer sourceSets = getSourceSets(project)
        TestConfigTask generateTestConfig = project.tasks.create('generateTestConfig', TestConfigTask)

        generateTestConfig.with {
            group = 'verification'
            description = 'Create property file readable by tests.'
        }

        sourceSets.all { SourceSet it ->
            configureSourceSet(it, generateTestConfig)
        }

        sourceSets.whenObjectAdded { SourceSet it ->
            configureSourceSet(it, generateTestConfig)
        }
    }

    @CompileDynamic
    SourceSetContainer getSourceSets(Project project) {
        project.sourceSets
    }

    static void configureSourceSet(SourceSet sourceSet, TestConfigTask generateTestConfig) {
        if (sourceSet.name != 'main') {
            Project project = generateTestConfig.project
            TaskContainer tasks = project.tasks
            Copy processResources = (Copy) tasks.getByName(sourceSet.getProcessResourcesTaskName())
            processResources.from generateTestConfig
        }
    }
}
