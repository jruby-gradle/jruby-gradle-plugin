package com.github.jrubygradle

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/** Thrown when a parameter has not been correctly initialised.
 *
 * @since 2.0
 */
@CompileStatic
@InheritConstructors
class UninitialisedParameterException extends Exception {
}
