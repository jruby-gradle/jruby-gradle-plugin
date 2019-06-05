package com.github.jrubygradle.api.gems

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/** Throws when there are issues installing and extracting GEMs.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
@InheritConstructors
@CompileStatic
class GemInstallException extends Exception {
}
