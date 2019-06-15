package com.github.jrubygradle.api.gems

import com.github.jrubygradle.api.core.ApiException
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/** Thrown when GEM version strings cannot be correctly parsed.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
@InheritConstructors
@CompileStatic
class GemVersionException extends ApiException {
}
