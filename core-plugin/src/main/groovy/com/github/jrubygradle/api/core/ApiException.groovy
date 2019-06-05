package com.github.jrubygradle.api.core

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/** Throws when there are issues with the RubyGems REST API.
 *
 * @since 2.0
 */
@InheritConstructors
@CompileStatic
class ApiException extends Exception {
}
