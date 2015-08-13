import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.wrapper.Download
import org.gradle.wrapper.Install
import org.gradle.wrapper.Logger
import org.gradle.wrapper.PathAssembler
import org.gradle.wrapper.WrapperConfiguration

/**
 * Include this in your project's buildSrc, then add a dependency to your project:
 * compile new GradleDist(project, '2.6').asFileTree
 *
 * Code courtesy of @ajoberstar
 */
class GradleDist {
    private final Project project
    final String version

    GradleDist(Project project, String version) {
        this.project = project
        this.version = version
    }

    String getPath() {
        return "https://services.gradle.org/distributions/gradle-${version}-bin.zip"
    }

    File getAsFile() {
        return project.file(getPath())
    }

    URI getAsURI() {
        return project.uri(getPath())
    }

    FileTree getAsFileTree() {
        Logger logger = new Logger(true)
        Install install = new Install(logger, new Download(logger, 'gradle', ''), new PathAssembler(project.gradle.gradleUserHomeDir))
        WrapperConfiguration config = new WrapperConfiguration()
        config.distribution = getAsURI()
        File file = install.createDist(config)
        return project.fileTree(dir:file, include:'**/*.jar')
    }
}
