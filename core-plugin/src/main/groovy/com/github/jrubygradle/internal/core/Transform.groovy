/*
 * Copyright (c) 2014-2019, R. Tyler Croy <rtyler@brokenco.de>,
 *     Schalk Cronje <ysb33r@gmail.com>, Christian Meier, Lookout, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.jrubygradle.internal.core

import groovy.transform.CompileStatic

import java.util.function.Function
import java.util.stream.Collectors

/** Transforms a collection to another collection.
 *
 * Deals with Groovy 2.4/2.5 backwards incompatibility.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 *
 */
@CompileStatic
class Transform {
    static <I,O> List<O> toList(final Collection<I> collection, Function<I,O> tx ) {
        collection.stream().map(tx).collect(Collectors.toList())
    }

    static <I,O> List<O> toList(final I[] collection, Function<I,O> tx ) {
        collection.toList().stream().map(tx).collect(Collectors.toList())
    }

    static <I,O> Set<O> toSet(final Collection<I> collection, Function<I,O> tx ) {
        collection.stream().map(tx).collect(Collectors.toSet())
    }

    static <I,O> Set<O> toSet(final Iterable<I> collection, Function<I,O> tx ) {
        collection.toList().stream().map(tx).collect(Collectors.toSet())
    }
}
