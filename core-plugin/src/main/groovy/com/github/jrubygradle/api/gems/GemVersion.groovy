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
package com.github.jrubygradle.api.gems

import com.github.jrubygradle.internal.core.Transform
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.MatchResult
import java.util.regex.Pattern

import static com.github.jrubygradle.api.gems.GemVersion.Boundary.EXCLUSIVE
import static com.github.jrubygradle.api.gems.GemVersion.Boundary.INCLUSIVE
import static com.github.jrubygradle.api.gems.GemVersion.Boundary.OPEN_ENDED

/**
 * With rubygems almost all dependencies will be declared
 * via versions ranges and tools like Bundler are very strict on how to
 * resolve those versions - i.e. the resolved version needs to obey each given
 * constraint. Ivy does the same but Gradle and Ivy pick the latest and
 * newest version when there are more then one constraint for the same gem -
 * which can create problems when using Bundler alongside Gradle.
 *
 * When converting a GemSpec into a Ivy ivy.xml the translation of a
 * gem version range into an Ivy version range. typically '~> 1.0' from ruby
 * becomes {@code [1.0.0,2.0[} on the Ivy side. so most dependencies from
 * gem artifacts will use such version ranges.
 *
 * To help gradle to be closer to the rubygems world when resolving gem
 * artifacts, it needs to calculate intersection between version ranges
 * in maven manner.
 *
 * This class basically represents an Ivy version range with boundary
 * (exclusive vs. inclusive or open-ended) and its lower and upper bounded version and
 * allows to intersect its range with another version range.
 *
 * It also translate fixed version '1.0' to [1.0, 1.0] or the gradle notation
 * 1.2+ to [1.2, 1.99999] or 1.+ to [1.0, 1.99999] following the gemspec-to-pom
 * pattern.
 *
 * @author Christian Meier
 * @author Schalk W. Cronjé
 *
 * @since 2.0 (Moved here from base plugin where it existed since 0.4.0)
 */
@CompileStatic
@Slf4j
class GemVersion implements Comparable<GemVersion> {

    /** How versions at boundaries are defined.
     *
     */
    @SuppressWarnings('DuplicateStringLiteral')
    enum Boundary {
        /** The specified version is included on the border.
         *
         */
        INCLUSIVE('[', ']'),

        /** THe specified version is excluded on the border.
         *
         */
        EXCLUSIVE(']', '['),

        /** All values below (on the low border) or above (on the high border)
         * are acceptable
         *
         */
        OPEN_ENDED('(', ')')

        final String low
        final String high

        private Boundary(String low, String hi) {
            this.low = low
            this.high = hi
        }
    }

    public static final GemVersion NO_VERSION = new GemVersion(null, null, null, null)
    public static final GemVersion EVERYTHING = new GemVersion(OPEN_ENDED, null, null, OPEN_ENDED)

    public static final String MAX_VERSION = '99999'
    public static final String MIN_VERSION = '0.0.0'

    private static final String LOW_IN = '['
    private static final String UP_IN = ']'

    // Gradle/Ivy version patterns
    private static final Pattern DOT_PLUS = ~/^(.+?)\.\+$/
    private static final Pattern PLUS = ~/^\+$/
    private static final Pattern DIGITS_PLUS = ~/^(.+?)\.(\p{Alnum}+)\+$/
    private static final Pattern OPEN_BOTTOM = ~/^\(,(.+)(\[|\])$/
    private static final Pattern OPEN_TOP = ~/^(\[|\])(.+),\)$/
    private static final Pattern RANGE = ~/^(\[|\])(.+?),(.+?)(\[|\])$/

    private static final Pattern ONLY_DIGITS = ~/^\d+$/
    private static final Pattern DIGITS_AND_DOTS = ~/^\d+(\.\d+){1,3}(-\p{Alnum}+)?$/

    // GEM requirement patterns
    private static final Pattern GREATER_EQUAL = ~/^>=\s*(.+)/
    private static final Pattern GREATER = ~/^>\s*(.+)/
    private static final Pattern EQUAL = ~/^=\s*(.+)/
    private static final Pattern NOT_EQUAL = ~/^!=\s*(.+)/
    private static final Pattern LESS = ~/^<\s*(.+)/
    private static final Pattern LESS_EQUAL = ~/^<=\s*(.+)/
    private static final Pattern TWIDDLE_WAKKA = ~/^~>\s*(.+)/

    private static final String VERSION_SPLIT = '.'
    private static final String PAD_ZERO = '0'
    private static final String EMPTY = ''

    private static final String NOT_GEM_REQ = 'This does not look like a standard GEM version requirement'

    final String low
    final String high
    private final Boundary lowBoundary
    private final Boundary highBoundary

    /** Create a Gem version instance from a Gradle version requirement.
     *
     * @param singleRequirement Gradle version string.
     * @return GemVersion instance.
     *
     * @since 2.0
     */
    static GemVersion gemVersionFromGradleIvyRequirement(String singleRequirement) {
        new GemVersion(singleRequirement)
    }

    /** Takes a GEM requirement list and creates a list of GEM versions
     *
     * @param multipleRequirements Comma-separated list of GEM requirements.
     * @return List of GEM versions. Can be empty if all requirements evaluate to {@link #NO_VERSION}.
     */
    static List<GemVersion> gemVersionsFromMultipleGemRequirements(String multipleRequirements) {
        Transform.toList(multipleRequirements.split(/,\s*/)) { String it ->
            gemVersionFromGemRequirement(it.trim())
        }.findAll {
            it != NO_VERSION
        }
    }

    /** Takes a GEM requirement list and creates a single GEM version, by taking a union of
     * all requirements.
     *
     * @param multipleRequirements Comma-separated list of GEM requirements.
     * @return Unioned GEM
     */
    static GemVersion singleGemVersionFromMultipleGemRequirements(String multipleRequirements) {
        List<GemVersion> gemVersions = gemVersionsFromMultipleGemRequirements(multipleRequirements)
        if (gemVersions.empty) {
            EVERYTHING
        } else if (gemVersions.size() == 1) {
            gemVersions.first()
        } else {
            gemVersions[1..-1].inject(gemVersions.first()) { range, value ->
                range.union(value)
            }
        }
    }

    /** Create a Gem version instance from a single GEM version requirement.
     *
     * @param singleRequirement Single GEM requirement string.
     * @return GemVersion instance. Can return {@link #NO_VERSION} if the version is parseable,
     *   but not translatable to Ivy format.
     *
     * @since 2.0
     */
    @SuppressWarnings('DuplicateStringLiteral')
    static GemVersion gemVersionFromGemRequirement(String singleRequirement) {
        if (singleRequirement.matches(GREATER_EQUAL)) {
            new GemVersion(
                INCLUSIVE,
                getVersionFromRequirement(singleRequirement, GREATER_EQUAL),
                null,
                OPEN_ENDED
            )
        } else if (singleRequirement.matches(GREATER)) {
            new GemVersion(
                EXCLUSIVE,
                getVersionFromRequirement(singleRequirement, GREATER),
                null,
                OPEN_ENDED
            )
        } else if (singleRequirement.matches(EQUAL)) {
            String exact = getVersionFromRequirement(singleRequirement, EQUAL)
            new GemVersion(
                INCLUSIVE,
                exact,
                exact,
                INCLUSIVE
            )
        } else if (singleRequirement.matches(NOT_EQUAL)) {
            log.info("'${singleRequirement}' is supported by Ivy.")
            NO_VERSION
        } else if (singleRequirement.matches(LESS_EQUAL)) {
            new GemVersion(
                OPEN_ENDED,
                null,
                getVersionFromRequirement(singleRequirement, LESS_EQUAL),
                INCLUSIVE
            )
        } else if (singleRequirement.matches(LESS)) {
            new GemVersion(
                OPEN_ENDED,
                null,
                getVersionFromRequirement(singleRequirement, LESS),
                EXCLUSIVE
            )
        } else if (singleRequirement.matches(TWIDDLE_WAKKA)) {
            parseTwiddleWakka(singleRequirement)
        } else if (singleRequirement.matches(DIGITS_AND_DOTS)) {
            new GemVersion(
                INCLUSIVE,
                singleRequirement,
                singleRequirement,
                INCLUSIVE
            )
        } else {
            throw new GemVersionException("'${singleRequirement}' does not look like a GEM version requirement")
        }
    }

    /** Is the low version specification inclusive?
     *
     * @return {@code true} if inclusive.
     *
     * @since 2.0
     */
    boolean isLowInclusive() {
        lowBoundary == INCLUSIVE
    }

    /** Is the high version specification inclusive?
     *
     * @return {@code true} if inclusive.
     *
     * @since 2.0
     */
    boolean isHighInclusive() {
        highBoundary == INCLUSIVE
    }

    /** Is the high version unspecified?
     *
     * @return {@code true} if the high version is unspecified in the original GEM specification.
     *
     * @since 2.0
     */
    boolean isHighOpenEnded() {
        highBoundary == Boundary.OPEN_ENDED
    }

    /**
     * since GemVersion is version range with lower bound and upper bound
     * this method just calculates the intersection of this version range
     * with the given other version range. it also honors whether the boundary
     * itself is included or excluded by the respective ranges.
     *
     * @param The other version range to be intersected with this version range
     * @return GemVersion the intersected version range
     */
    GemVersion intersect(String otherVersion) {
        intersect(gemVersionFromGradleIvyRequirement(otherVersion))
    }

    /**
     * since GemVersion is version range with lower bound and upper bound
     * this method just calculates the intersection of this version range
     * with the given other version range. it also honors whether the boundary
     * itself is included or excluded by the respective ranges.
     *
     * @param The other version range to be intersected with this version range
     * @return GemVersion the intersected version range
     *
     * @since 2.0
     */
    GemVersion intersect(GemVersion other) {
        Boundary newLowBoundary
        String newLow
        switch (compare(low, other.low)) {
            case -1:
                newLow = other.low
                newLowBoundary = other.lowBoundary
                break
            case 0:
                newLowBoundary = (lowBoundary == EXCLUSIVE || other.lowBoundary == EXCLUSIVE) ? EXCLUSIVE : INCLUSIVE
                newLow = low
                break
            case 1:
                newLow = low
                newLowBoundary = lowBoundary
        }

        Boundary newHighBoundary
        String newHigh

        if (!high && other.high) {
            newHigh = other.high
            newHighBoundary = other.highBoundary
        } else if (high && !other.high) {
            newHigh = high
            newHighBoundary = highBoundary
        } else if (!high && !other.high) {
            newHigh = null
            newHighBoundary = highBoundary
        } else {
            switch (compare(high, other.high)) {
                case 1:
                    newHigh = other.high
                    newHighBoundary = other.highBoundary
                    break
                case 0:
                    newHighBoundary = (highBoundary == EXCLUSIVE || other.highBoundary == EXCLUSIVE) ? EXCLUSIVE : INCLUSIVE
                    newHigh = high
                    break
                case -1:
                    newHigh = high
                    newHighBoundary = highBoundary
            }
        }
        return new GemVersion(newLowBoundary, newLow, newHigh, newHighBoundary)
    }

    /** Creates a new GEM version requirement which that the lowest of two requirements and the highest
     * of those same requirement
     *
     * @param other Other GEM to combine with
     * @return New GEM version requirement.
     *
     * @since 2.0
     */
    GemVersion union(GemVersion other) {
        List<GemVersion> pair = [this, other]
        GemVersion min = pair.min()
        GemVersion max = pair.max()

        new GemVersion(min.lowBoundary, min.low, max.high, max.highBoundary)
    }

    /** Allows for versions to be compared and sorted.
     *
     * @param other Other GEM version to compare to.
     * @return -1, 0 or 1.
     *
     * @since 2.0
     */
    @Override
    int compareTo(GemVersion other) {
        int loCompare = compare(low, other.low)
        if (loCompare) {
            return loCompare
        }

        if (lowBoundary != other.lowBoundary) {
            if (lowBoundary == OPEN_ENDED) {
                return -1
            } else if (other.lowBoundary == OPEN_ENDED) {
                return 1
            }
            return lowBoundary == INCLUSIVE ? -1 : 1
        }

        int hiCompare = compare(high, other.high)

        if (hiCompare) {
            return hiCompare
        }

        if (highBoundary != other.highBoundary) {
            if (highBoundary == OPEN_ENDED) {
                return 1
            } else if (other.highBoundary == OPEN_ENDED) {
                return -1
            }
            return highBoundary == INCLUSIVE ? 1 : -1
        }

        0
    }

    /**
     * examines the version range on conflict, i.e. lower bound bigger then
     * upper bound.

     * @return boolean true if lower bound bigger then upper bound
     */
    boolean conflict() {
        compare(stripNonIntegerTail(low), stripNonIntegerTail(high)) == 1
    }

    /** String of the underlying data as Ivy version range.
     *
     * @return Gradle Ivy version range
     */
    String toString() {
        if (this == EVERYTHING) {
            '+'
        } else if (this == NO_VERSION) {
            ']0,0['
        } else if (lowBoundary == INCLUSIVE && highBoundary == INCLUSIVE && low == high) {
            low
        } else {
            "${lowBoundary?.low ?: EMPTY}${low ?: EMPTY},${high ?: EMPTY}${highBoundary?.high ?: EMPTY}"
        }
    }

    private static GemVersion parseTwiddleWakka(String singleRequirement) {
        String base = getVersionFromRequirement(singleRequirement, TWIDDLE_WAKKA)
        List<String> parts = base.tokenize(VERSION_SPLIT)
        if (1 == parts.size()) {
            if (base =~ ONLY_DIGITS) {
                return new GemVersion(
                    INCLUSIVE,
                    base,
                    null,
                    OPEN_ENDED
                )
            }

            throw new GemVersionException(
                "'${singleRequirement}' does not look like a correctly formatted GEM twiddle-wakka"
            )
        }

        String lastNumberPart = parts[0..-2].reverse().find {
            it =~ ONLY_DIGITS
        }
        if (lastNumberPart == null) {
            throw new GemVersionException("Cannot extract last number part from '${singleRequirement}'. " +
                NOT_GEM_REQ)
        }
        int bottomAdds = 3 - parts.size()
        if (bottomAdds < 0) {
            bottomAdds = 0
        }
        try {
            Integer nextUp = lastNumberPart.toInteger() + 1
            String leader = parts.size() <= 2 ? EMPTY : "${parts[0..-3].join(VERSION_SPLIT)}."
            new GemVersion(
                INCLUSIVE,
                "${base}${'.0' * bottomAdds}",
                "${leader}${nextUp}.0",
                EXCLUSIVE
            )
        } catch (NumberFormatException e) {
            throw new GemVersionException("Can extract last number part from '${singleRequirement}'. " +
                NOT_GEM_REQ, e)
        }
    }

    @CompileDynamic
    @SuppressWarnings('NoDef')
    private static String getVersionFromRequirement(String gemRevision, Pattern matchPattern) {
        def matcher = gemRevision =~ matchPattern
        matcher[0][1]
    }

    private GemVersion(Boundary pre, String low, String high, Boundary post) {
        this.lowBoundary = pre
        this.low = low
        this.high = high
        this.highBoundary = post
    }

    /**
     * converts the given string to a version range with inclusive or
     * exclusive boundaries.
     *
     * @param String gradleVersionPattern
     */
    @CompileDynamic
    private GemVersion(final String gradleVersionPattern) {
        String cleanedString = gradleVersionPattern.replaceAll(~/\p{Blank}/, '')
        MatchResult dotPlus = cleanedString =~ DOT_PLUS
        MatchResult plus = cleanedString =~ PLUS
        MatchResult digitsPlus = cleanedString =~ DIGITS_PLUS
        MatchResult openBottom = cleanedString =~ OPEN_BOTTOM
        MatchResult openTop = cleanedString =~ OPEN_TOP
        MatchResult range = cleanedString =~ RANGE

        if (dotPlus.matches()) {
            String base = dotPlus[0][1]
            this.low = padVersion(base, PAD_ZERO)
            this.high = padVersion(base, MAX_VERSION)
            this.lowBoundary = INCLUSIVE
            this.highBoundary = INCLUSIVE
        } else if (plus.matches()) {
            this.low = MIN_VERSION
            this.lowBoundary = INCLUSIVE
            this.highBoundary = OPEN_ENDED
        } else if (digitsPlus.matches()) {
            this.lowBoundary = INCLUSIVE
            this.highBoundary = INCLUSIVE
            this.low = "${digitsPlus[0][1]}.${digitsPlus[0][2]}"
            this.high = "${digitsPlus[0][1]}.${MAX_VERSION}"
        } else if (openBottom.matches()) {
            this.lowBoundary = OPEN_ENDED
            this.high = openBottom[0][1]
            this.highBoundary = openBottom[0][2] == UP_IN ? INCLUSIVE : EXCLUSIVE
        } else if (openTop.matches()) {
            this.highBoundary = OPEN_ENDED
            this.low = openTop[0][2]
            this.lowBoundary = openTop[0][1] == LOW_IN ? INCLUSIVE : EXCLUSIVE
        } else if (range.matches()) {
            this.lowBoundary = range[0][1] == LOW_IN ? INCLUSIVE : EXCLUSIVE
            this.highBoundary = range[0][4] == UP_IN ? INCLUSIVE : EXCLUSIVE
            this.low = range[0][2]
            this.high = range[0][3]
        } else {
            this.low = cleanedString
            this.high = cleanedString
            this.lowBoundary = INCLUSIVE
            this.highBoundary = INCLUSIVE
        }
    }

    /**
     * compares two version strings. first it splits the version
     * into parts on their ".". if one version has more parts then
     * the other, then the number of parts is used for comparison.
     * otherwise we find a part which differs between the versions
     * and compare them. this last comparision converts the parts to
     * integers if both contains only digits. otherwise a lexical
     * string comparision is used.
     *
     * @param lhs first version
     * @param rhs second version
     * @return lexicographical comparison. Any part containing alpha characters will always be less than
     * a part with pure digits.
     */
    private int compare(String lhs, String rhs) {
        if (!lhs && !rhs) {
            return 0
        }

        if (!lhs && rhs) {
            return -1
        }

        if (lhs && !rhs) {
            return -1
        }

        List<String> lhsParts = lhs.tokenize(VERSION_SPLIT)
        List<String> rhsParts = rhs.tokenize(VERSION_SPLIT)

        for (int i = 0; i < lhsParts.size() && i < rhsParts.size(); i++) {
            int cmp
            boolean lhsNumerical = lhsParts[i].matches(ONLY_DIGITS)
            boolean rhsNumerical = rhsParts[i].matches(ONLY_DIGITS)

            if (lhsNumerical && rhsNumerical) {
                cmp = lhsParts[i].toInteger() <=> rhsParts[i].toInteger()
            } else if (lhsNumerical && !rhsNumerical) {
                cmp = 1
            } else if (!lhsNumerical && rhsNumerical) {
                cmp = -1
            } else {
                cmp = lhsParts[i] <=> rhsParts[i]
            }

            if (cmp != 0) {
                return cmp
            }
        }

        lhsParts.size() <=> rhsParts.size()
    }

    private String padVersion(final String base, final String padValue) {
        String pad = ".${padValue}"
        int adds = 3 - base.tokenize(VERSION_SPLIT).size()
        if (adds < 0) {
            adds = 0
        }
        "${base}${pad * adds}"
    }

    private String stripNonIntegerTail(String version) {
        version?.replaceFirst(~/\.\p{Alpha}.*$/, '')
    }

}
