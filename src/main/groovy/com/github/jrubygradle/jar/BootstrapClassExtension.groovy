package com.github.jrubygradle.jar

import groovy.transform.TupleConstructor
import org.gradle.api.GradleException
import org.gradle.api.Task

/** The Bootstrap extension allows for JRUBy-based Java code to be generated, compiled and
 * included into the final JAR artifact. It is meant to extend a JAR type task.
 *
 * @author Schalk W. Cronjé.
 *
 * @since 0.1.1
 */
@TupleConstructor
class BootstrapClassExtension {

    static final String BOOTSTRAP_TEMPLATE_PATH = 'META-INF/gradle-plugins/bootstrap.java.template'

    /** During construction the inputs of Task will be updated to also look here for updates
     *
     * @param t Task this extension is associated to
     */
    BootstrapClassExtension(Task t) {
        task = t

        task.inputs.property 'jrubyInitScript', { -> this.initScript}
        task.inputs.property 'jrubyCompatMode', { -> this.compatMode}
        task.inputs.property 'jrubySource',     { -> this.getSource()}
    }

    /** The task this extension instance is attached to.
     *
     * @since 0.1.1
     *
     */
    Task task

    /** The location of the Ruby initialisation/bootstrap script.
     * The default bootstrap class will look for a file in this relative location.
     * It is the user's responsibility that this script is added to the Jar.
     *
     * Prior to to 0.1.4 the default value was {@code 'META-INF/init.rb'}. As from
     * 0.1.4 the default value is {@code 'META-INF/jar-bootstrap.rb'}
     *
     * @since 0.1.1
     */
    String initScript = 'META-INF/jar-bootstrap.rb'

    /** This is the JRuby language compatibility mode.
     * @since 0.1.1
     */
    String compatMode = '1.9'

    Object source

    Object getSource() {
        if(null == source) {
            setSourceFromResource()
        }
        this.source
    }

    void setSourceFromResource() {
        Enumeration<URL> enumResources
        enumResources = this.class.classLoader.getResources( BOOTSTRAP_TEMPLATE_PATH)
        if(!enumResources.hasMoreElements()) {
            throw new GradleException ("Cannot find ${BOOTSTRAP_TEMPLATE_PATH} in classpath")
        } else {
            URI uri = enumResources.nextElement().toURI()
            String location = uri.getSchemeSpecificPart().replace('!/'+BOOTSTRAP_TEMPLATE_PATH,'')
            if(uri.scheme.startsWith('jar')) {
                location=location.replace('jar:file:','')
                source= task.project.zipTree(location)
            } else if(uri.scheme.startsWith('file')) {
                source= location.replace('file:','')
            } else {
                throw new GradleException("Cannot extract ${uri}")
            }
        }

   }

}
