package com.github.jrubygradle.internal.gems

import com.github.jrubygradle.api.gems.GemDependency
import groovy.transform.CompileStatic

/** Defining a GEM dependency.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
@CompileStatic
class DefaultGemDependency implements GemDependency {

    /** Name of transitive dependency.
     *
     */
    String name

    /** Version requirements upon this transitive dependency.
     *
     */
    String requirements
}
