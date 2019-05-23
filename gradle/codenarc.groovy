ruleset {
    description '''
        A CodeNarc rules set to keep jruby/gradle code consistent and somewhat
        bug free :)
        '''

    AbcMetric   // Requires the GMetrics jar
    AbstractClassName
    AbstractClassWithPublicConstructor
    AbstractClassWithoutAbstractMethod
    AddEmptyString
    AssertWithinFinallyBlock
    AssignCollectionSort
    AssignCollectionUnique
    AssignmentInConditional
    AssignmentToStaticFieldFromInstanceMethod
    BigDecimalInstantiation
    BitwiseOperatorInConditional
    BlankLineBeforePackage
    BooleanGetBoolean
    BooleanMethodReturnsNull
    BracesForClass
    BracesForForLoop
    BracesForIfElse
    BracesForMethod
    BracesForTryCatchFinally
    BrokenNullCheck
    BrokenOddnessCheck
    BuilderMethodWithSideEffects
    BusyWait
    CatchArrayIndexOutOfBoundsException
    CatchError
    CatchException
    CatchIllegalMonitorStateException
    CatchIndexOutOfBoundsException
    CatchNullPointerException
    CatchRuntimeException
    CatchThrowable
    ChainedTest
    ClassForName
    ClassJavadoc {
        doNotApplyToFileNames = '*Spec.groovy,*Specification.groovy'
    }
    ClassName
    ClassNameSameAsFilename
    ClassNameSameAsSuperclass
    ClassSize
    CloneableWithoutClone
    CloseWithoutCloseable
    ClosureAsLastMethodParameter
    ClosureStatementOnOpeningLineOfMultipleLineClosure
    CollectAllIsDeprecated
    CompareToWithoutComparable
    ComparisonOfTwoConstants
    ComparisonWithSelf
    ConfusingClassNamedException
    ConfusingMultipleReturns
    ConfusingTernary
    ConsecutiveBlankLines
    ConsecutiveLiteralAppends
    ConsecutiveStringConcatenation
    ConstantAssertExpression
    ConstantIfExpression
    ConstantTernaryExpression
    ConstantsOnlyInterface
    CouldBeElvis
    CoupledTestCase
    DeadCode
    DirectConnectionManagement
    DoubleCheckedLocking
    DoubleNegative
    DuplicateCaseStatement
    DuplicateImport
    DuplicateListLiteral {
        doNotApplyToFileNames = '*Spec.groovy,*Specification.groovy'
    }
    DuplicateMapKey
    DuplicateMapLiteral {
        doNotApplyToFileNames = '*Spec.groovy,*Specification.groovy'
    }
    DuplicateSetValue
    DuplicateStringLiteral {
        doNotApplyToFileNames = '*Spec.groovy,*Specification.groovy'
    }
    ElseBlockBraces
    EmptyCatchBlock
    EmptyClass
    EmptyElseBlock
    EmptyFinallyBlock
    EmptyForStatement
    EmptyIfStatement
    EmptyInstanceInitializer
    EmptyMethod
    EmptyMethodInAbstractClass
    EmptyStaticInitializer
    EmptySwitchStatement
    EmptySynchronizedStatement
    EmptyTryBlock
    EmptyWhileStatement
    EnumCustomSerializationIgnored
    EqualsAndHashCode
    EqualsOverloaded
    ExceptionExtendsError
    ExceptionExtendsThrowable
    ExceptionNotThrown
    ExplicitArrayListInstantiation
    ExplicitCallToAndMethod
    ExplicitCallToCompareToMethod
    ExplicitCallToDivMethod
    ExplicitCallToEqualsMethod
    ExplicitCallToGetAtMethod
    ExplicitCallToLeftShiftMethod
    ExplicitCallToMinusMethod
    ExplicitCallToModMethod
    ExplicitCallToMultiplyMethod
    ExplicitCallToOrMethod
    ExplicitCallToPlusMethod
    ExplicitCallToPowerMethod
    ExplicitCallToRightShiftMethod
    ExplicitCallToXorMethod
    ExplicitGarbageCollection
    ExplicitHashMapInstantiation
    ExplicitHashSetInstantiation
    ExplicitLinkedHashMapInstantiation
    ExplicitLinkedListInstantiation
    ExplicitStackInstantiation
    ExplicitTreeSetInstantiation
    FieldName
    FileCreateTempFile
    FinalClassWithProtectedMember
    ForLoopShouldBeWhileLoop
    ForStatementBraces
    GStringAsMapKey
    GStringExpressionWithinString
    GetterMethodCouldBeProperty
    GroovyLangImmutable
    HardCodedWindowsFileSeparator
    HardCodedWindowsRootDirectory
    HashtableIsObsolete
    IfStatementBraces
    IllegalClassMember
    IllegalClassReference
    IllegalPackageReference
    IllegalRegex
    IllegalString
    IllegalSubclass
    ImplementationAsType
    ImportFromSamePackage
    ImportFromSunPackages
    InconsistentPropertyLocking
    InconsistentPropertySynchronization
    InsecureRandom
    //Instanceof
    IntegerGetInteger
    InterfaceName
    InterfaceNameSameAsSuperInterface
    InvertedIfElse

    //JavaIoPackageAccess

    // Generally we've been pretty good about this, this warning only really
    // shows up for good descriptive exception messages
    //LineLength
    LocaleSetDefault
    LoggerForDifferentClass
    LoggerWithWrongModifiers
    LoggingSwallowsStacktrace
    LongLiteralWithLowerCaseL
    MethodCount
    MethodName {
        doNotApplyToFileNames = '*Spec.groovy,*Specification.groovy'
    }
    MethodSize
    MissingBlankLineAfterImports
    MissingBlankLineAfterPackage
    MissingNewInThrowStatement
    MultipleLoggers
    MultipleUnaryOperators
    NestedBlockDepth
    NestedForLoop
    NestedSynchronization
    NoDef {
        doNotApplyToFileNames = '*Spec.groovy,*Specification.groovy'
    }
    NoWildcardImports
    NonFinalPublicField
    NonFinalSubclassOfSensitiveInterface
    ObjectFinalize
    ObjectOverrideMisspelledMethodName
    PackageName
    PackageNameMatchesFilePath
    ParameterCount
    ParameterName
    ParameterReassignment
    PrintStackTrace
    Println
    PrivateFieldCouldBeFinal
    PropertyName
    PublicFinalizeMethod
    PublicInstanceField
    RandomDoubleCoercedToZero
    RemoveAllOnSelf
    RequiredRegex
    RequiredString
    ReturnFromFinallyBlock
    ReturnNullFromCatchBlock
    ReturnsNullInsteadOfEmptyArray
    ReturnsNullInsteadOfEmptyCollection
    SerialPersistentFields
    SerialVersionUID
    SimpleDateFormatMissingLocale
    SpaceAfterCatch
    SpaceAfterClosingBrace
    SpaceAfterComma
    SpaceAfterFor
    SpaceAfterIf
    SpaceAfterOpeningBrace
    SpaceAfterSemicolon
    SpaceAfterSwitch
    SpaceAfterWhile
    SpaceAroundClosureArrow
    SpaceAroundOperator
    SpaceBeforeClosingBrace
    SpaceBeforeOpeningBrace
    SpockIgnoreRestUsed
    StatelessClass
    StatelessSingleton
    StaticCalendarField
    StaticConnection
    StaticDateFormatField
    StaticMatcherField
    StaticSimpleDateFormatField
    SwallowThreadDeath
    SynchronizedMethod
    SynchronizedOnBoxedPrimitive
    SynchronizedOnGetClass
    SynchronizedOnReentrantLock
    SynchronizedOnString
    SynchronizedOnThis
    SynchronizedReadObjectMethod
    SystemErrPrint
    SystemExit
    SystemOutPrint
    SystemRunFinalizersOnExit
    TernaryCouldBeElvis
    ThreadGroup
    ThreadLocalNotStaticFinal
    ThreadYield
    ThrowError
    ThrowException
    ThrowExceptionFromFinallyBlock
    ThrowNullPointerException
    ThrowRuntimeException
    ThrowThrowable
    ToStringReturnsNull
    TrailingWhitespace {
        doNotApplyToFileNames = '*Spec.groovy,*Specification.groovy'
    }
    UnnecessaryBigDecimalInstantiation
    UnnecessaryBigIntegerInstantiation
    UnnecessaryBooleanExpression {
        doNotApplyToFileNames = '*Spec.groovy,*Specification.groovy'
    }
    UnnecessaryBooleanInstantiation
    UnnecessaryCallForLastElement
    UnnecessaryCallToSubstring
    UnnecessaryCast
    UnnecessaryCatchBlock
    UnnecessaryCollectCall
    UnnecessaryCollectionCall
    UnnecessaryConstructor
    UnnecessaryDefInFieldDeclaration
    UnnecessaryDefInMethodDeclaration
    UnnecessaryDefInVariableDeclaration
    UnnecessaryDotClass
    UnnecessaryDoubleInstantiation
    UnnecessaryElseStatement
    UnnecessaryFail
    UnnecessaryFinalOnPrivateMethod
    UnnecessaryFloatInstantiation
    UnnecessaryGString {
        doNotApplyToFileNames = '*Spec.groovy,*Specification.groovy'
    }
    UnnecessaryGetter
    UnnecessaryGroovyImport
    UnnecessaryIfStatement
    UnnecessaryInstanceOfCheck
    UnnecessaryInstantiationToGetClass
    UnnecessaryIntegerInstantiation
    UnnecessaryLongInstantiation
    UnnecessaryModOne
    UnnecessaryNullCheck
    UnnecessaryNullCheckBeforeInstanceOf
    UnnecessaryObjectReferences
    UnnecessaryOverridingMethod
    UnnecessaryPackageReference
    UnnecessaryParenthesesForMethodCallWithClosure
    UnnecessaryPublicModifier
    UnnecessarySafeNavigationOperator
    UnnecessarySelfAssignment
    UnnecessarySemicolon
    UnnecessaryStringInstantiation
    UnnecessarySubstring
    UnnecessaryTernaryExpression
    UnnecessaryToString
    UnnecessaryTransientModifier
    UnsafeArrayDeclaration
    UnusedArray
    UnusedImport
    UnusedMethodParameter
    UnusedObject
    UnusedPrivateField
    UnusedPrivateMethod
    UnusedPrivateMethodParameter
    UnusedVariable
    UseAssertEqualsInsteadOfAssertTrue
    UseAssertFalseInsteadOfNegation
    UseAssertNullInsteadOfAssertEquals
    UseAssertSameInsteadOfAssertTrue
    UseAssertTrueInsteadOfAssertEquals
    UseAssertTrueInsteadOfNegation
    UseCollectMany
    UseCollectNested
    UseOfNotifyMethod
    VariableName {
        doNotApplyToFileNames = '*Spec.groovy,*Specification.groovy'
    }
    VectorIsObsolete
    VolatileArrayField
    VolatileLongOrDoubleField
    WaitOutsideOfWhileLoop
    WhileStatementBraces
}

// vim: ft=groovy

