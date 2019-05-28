package com.github.jrubygradle.core

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/** Throws when there are issues with the RubyGems REST API.
 *
 */
@InheritConstructors
@CompileStatic
class ApiException extends Exception {
}
