package com.github.jrubygradle.core.internal

import com.github.jrubygradle.core.GemDependency
import groovy.transform.CompileStatic

/** Defining a GEM dependency.
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
