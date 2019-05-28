package com.github.jrubygradle.core

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/** Thrown when GEM version strings cannot be correctly parsed.
 *
 */
@InheritConstructors
@CompileStatic
class GemVersionException extends ApiException {
}
