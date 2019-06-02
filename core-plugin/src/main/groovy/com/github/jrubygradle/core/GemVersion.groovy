package com.github.jrubygradle.core

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import java.util.regex.MatchResult
import java.util.regex.Pattern

import static com.github.jrubygradle.core.GemVersion.Boundary.EXCLUSIVE
import static com.github.jrubygradle.core.GemVersion.Boundary.INCLUSIVE
import static com.github.jrubygradle.core.GemVersion.Boundary.INCLUSIVE
import static com.github.jrubygradle.core.GemVersion.Boundary.OPEN_ENDED

/**
 * With rubygems almost all dependencies will be declared
 * via versions ranges and tools like Bundler are very strict on how to
 * resolve those versions - i.e. the resolved version needs to obey each given
 * constraint. Maven does the same but Gradle and Ivy pick the latest and
 * newest version when there are more then one constraint for the same gem -
 * which can create problems when using Bundler alongside Gradle.
 *
 * When converting a GemSpec into a Maven pom.xml the translation of a
 * gem version range into a maven version range. typically '~> 1.0' from ruby
 * becomes [1.0, 1.99999] on the maven side. so most dependencies from
 * gem artifacts will use such version ranges.
 *
 * to help gradle to be closer to the rubygems world when resolving gem
 * artifacts, it needs to calculate intersection between version ranges
 * in maven manner.
 *
 * this class basically represents a maven version range with boundary
 * (exclusive vs. inclusive) and its lower and upper bounded version and
 * allows to intersect its range with another version range.
 *
 * it also translate fixed version '1.0' to [1.0, 1.0] or the gradle notation
 * 1.2+ to [1.2, 1.99999] or 1.+ to [1.0, 1.99999] following the gemspec-to-pom
 * pattern.
 *
 * @author Christian Meier
 * @author Schalk W. Cronjé
 *
 * @since 2.0 (Moved here from base plugin where it existed since 0.4.0)
 */
@CompileStatic
class GemVersion implements Comparable<GemVersion> {

    /** How versions at boundaries are defined.
     *
     */
    enum Boundary {
        /** The specified version is included on the border.
         *
         */
        INCLUSIVE('[',']'),

        /** THe specified version is excluded on the border.
         *
         */
        EXCLUSIVE(']','['),

        /** All values below (on the low border) or above (on the high border)
         * are acceptable
         *
         */
        OPEN_ENDED('(',')')

        final String low
        final String high

        private Boundary( String low, String hi) {
            this.low = low
            this.high = hi
        }
    }

    public static final String MAX_VERSION = '99999'
    public static final String MIN_VERSION = '0.0.0'

    private static final String LOW_EX = '('
    private static final String LOW_IN = '['
    private static final String UP_EX = ')'
    private static final String UP_IN = ']'
    private static final String UP_IVY_EX = LOW_IN
    private static final String LOW_IVY_EX = UP_IN

    // Gradle/Ivy version patterns
    private static final Pattern DOT_PLUS = ~/^(.+?)\.\+$/
    private static final Pattern PLUS = ~/^\+$/
    private static final Pattern DIGITS_PLUS = ~/^(.+?)\.(\p{Alnum}+)\+$/
    private static final Pattern OPEN_BOTTOM = ~/^\(,(.+)(\[|\])$/
    private static final Pattern OPEN_TOP = ~/^(\[|\])(.+),\)$/
    private static final Pattern RANGE = ~/^(\[|\])(.+?),(.+?)(\[|\])$/

    private static final Pattern ONLY_DIGITS = ~/^\d+$/

    private static final Pattern HEAD = ~/^.*,\s*/
    private static final Pattern TAIL = ~/,.*$'/
    private static final Pattern FIRST = ~/^[\[\]\(]/
    private static final Pattern LAST = ~/[\]\[\)]$/
    private static final Pattern ZEROS = ~/(\.0)+$/

    // GEM requirement patterns
    private static final Pattern GREATER_EQUAL = ~/^>=\s*(.+)/
    private static final Pattern GREATER = ~/^>\s*(.+)/
    private static final Pattern EQUAL = ~/^=\s*(.+)/
    private static final Pattern LESS = ~/^<\s*(.+)/
    private static final Pattern LESS_EQUAL = ~/^<=\s*(.+)/
    private static final Pattern TWIDDLE_WAKKA = ~/^~>\s*(.+)/

    private static final String VERSION_SPLIT = '.'

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

    /** Create a Gem version instance from a single GEM version requirement.
     *
     * @param singleRequirement Single GEM requirement string.
     * @return GemVersion instance.
     *
     * @since 2.0
     */
    @SuppressWarnings('DuplicateStringLiteral')
    static GemVersion gemVersionFromGemRequirement(String singleRequirement) {
        if (singleRequirement.matches(GREATER_EQUAL)) {
            new GemVersion(
                true,
                getVersionFromRequirement(singleRequirement, GREATER_EQUAL),
                "${MAX_VERSION.toString()}.0.0",
                true
            )
        } else if (singleRequirement.matches(GREATER)) {
            new GemVersion(
                false,
                getVersionFromRequirement(singleRequirement, GREATER),
                "${MAX_VERSION.toString()}.0.0",
                true
            )
        } else if (singleRequirement.matches(EQUAL)) {
            String exact = getVersionFromRequirement(singleRequirement, EQUAL)
            new GemVersion(
                true,
                exact,
                exact,
                true
            )
        } else if (singleRequirement.matches(LESS)) {
            new GemVersion(
                true,
                '0.0.0',
                getVersionFromRequirement(singleRequirement, LESS),
                false
            )
        } else if (singleRequirement.matches(LESS_EQUAL)) {
            new GemVersion(
                true,
                '0.0.0',
                getVersionFromRequirement(singleRequirement, LESS_EQUAL),
                true
            )
        } else if (singleRequirement.matches(TWIDDLE_WAKKA)) {
            String base = getVersionFromRequirement(singleRequirement, TWIDDLE_WAKKA)
            int adds = 3 - base.tokenize('.').size()
            if (adds < 0) {
                adds = 0
            }
            new GemVersion(
                true,
                "${base}${'.0' * adds} ",
                "${base}${('.' + MAX_VERSION) * adds}",
                true
            )
        } else {
            throw new GemVersionException("Do not not how to process ${singleRequirement} as a version string")
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

        if(!high && other.high) {
            newHigh = other.high
            newHighBoundary = other.highBoundary
        } else if (high && !other.high) {
            newHigh = high
            newHighBoundary = highBoundary
        } else if(!high && !other.high) {
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
            if(lowBoundary == OPEN_ENDED) {
                return -1
            } else if(other.lowBoundary == OPEN_ENDED) {
                return 1
            }
            return lowBoundary == INCLUSIVE ? -1 : 1
        }

        int hiCompare = compare(high, other.high)

        if (hiCompare) {
            return hiCompare
        }

        if (highBoundary != other.highBoundary) {
            if(highBoundary == OPEN_ENDED) {
                return 1
            } else if(other.highBoundary == OPEN_ENDED) {
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
        if (lowBoundary == INCLUSIVE && highBoundary == INCLUSIVE && low == high) {
            low
        } else {
            "${lowBoundary?.low ?: ''}${low ?: ''},${high ?: ''}${highBoundary?.high ?: ''}"
        }
    }

    @CompileDynamic
    @SuppressWarnings('NoDef')
    private static String getVersionFromRequirement(String gemRevision, Pattern matchPattern) {
        def matcher = gemRevision =~ matchPattern
        matcher[0][1]
    }

    private GemVersion(Boolean lowInclusive, String low, String high, Boolean highInclusive) {
        this.lowBoundary = lowInclusive ? INCLUSIVE : EXCLUSIVE
        this.low = low
        this.high = high
        this.highBoundary = highInclusive ? INCLUSIVE : EXCLUSIVE
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
        String cleanedString = gradleVersionPattern.replaceAll(~/\p{Blank}/,'')
        MatchResult dotPlus = cleanedString =~ DOT_PLUS
        MatchResult plus = cleanedString =~ PLUS
        MatchResult digitsPlus = cleanedString =~ DIGITS_PLUS
        MatchResult openBottom = cleanedString =~ OPEN_BOTTOM
        MatchResult openTop = cleanedString =~ OPEN_TOP
        MatchResult range = cleanedString =~ RANGE

        if (dotPlus.matches()) {
            String base = dotPlus[0][1]
            this.low = padVersion(base,'0')
            this.high = padVersion(base,MAX_VERSION)
            this.lowBoundary = INCLUSIVE
            this.highBoundary = INCLUSIVE
        } else if (plus.matches()) {
            this.low = MIN_VERSION
            this.lowBoundary = INCLUSIVE
            this.highBoundary = OPEN_ENDED
        } else if(digitsPlus.matches()) {
            this.lowBoundary = INCLUSIVE
            this.highBoundary = INCLUSIVE
            this.low = "${digitsPlus[0][1]}.${digitsPlus[0][2]}"
            this.high = "${digitsPlus[0][1]}.${MAX_VERSION}"
        } else if (openBottom.matches()) {
            this.lowBoundary = OPEN_ENDED
            this.high = openBottom[0][1]
            this.highBoundary = openBottom[0][2] == UP_IN ? INCLUSIVE : EXCLUSIVE
        } else if(openTop.matches()) {
            this.highBoundary = OPEN_ENDED
            this.low = openTop[0][2]
            this.lowBoundary = openTop[0][1] == LOW_IN ? INCLUSIVE : EXCLUSIVE
        } else if(range.matches()) {
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
        if(!lhs && !rhs) {
            return 0
        }

        if(!lhs && rhs) {
            return -1
        }

        if(lhs && !rhs) {
            return -1
        }

        List<String> lhsParts = lhs.tokenize(VERSION_SPLIT)
        List<String> rhsParts = rhs.tokenize(VERSION_SPLIT)

        for (int i = 0; i < lhsParts.size() && i < rhsParts.size(); i++) {
            int cmp
            boolean lhsNumerical = lhsParts[i].matches(ONLY_DIGITS)
            boolean rhsNumerical = rhsParts[i].matches(ONLY_DIGITS)

            if(lhsNumerical && rhsNumerical) {
                cmp = lhsParts[i].toInteger() <=> rhsParts[i].toInteger()
            } else if(lhsNumerical && !rhsNumerical) {
                cmp = 1
            } else if(!lhsNumerical && rhsNumerical) {
                cmp = -1
            } else {
                cmp = lhsParts[i] <=> rhsParts[i]
            }

            if(cmp != 0) {
                return cmp
            }
        }

        lhsParts.size() <=> rhsParts.size()
    }

    private String padVersion(final String base, final String padValue) {
        String pad = ".${padValue}"
        int adds = 3 - base.tokenize('.').size()
        if (adds < 0) {
            adds = 0
        }
        "${base}${pad * adds}"
    }

    private String stripNonIntegerTail(String version) {
        version?.replaceFirst(~/\.\p{Alpha}.*$/,'')
    }

}
