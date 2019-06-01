package com.github.jrubygradle.core

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/** Thrown when GEM version strings cannot be correctly parsed.
 *
 * @since 2.0
 */
@InheritConstructors
@CompileStatic
class GemVersionException extends ApiException {
}
