package com.github.jrubygradle.internal.gems

import com.github.jrubygradle.api.gems.JarDependency
import groovy.transform.CompileStatic

/** Defining a JAR dependency.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
@CompileStatic
class DefaultJarDependency extends DefaultGemDependency implements JarDependency {
    /** Name of group / organisation.
     *
     */
    String group
}
