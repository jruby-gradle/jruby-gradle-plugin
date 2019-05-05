== Running Tests

This project has both unit tests and integration tests. In our context the distinguishing point is that integration tests
need network connectivity, and unit tests need no more than local file system access. We also expect unit tests to be
very quick, whereas integration tests are allowed to take a _little_ longer.

Unit tests are in 'src/test' and integration tests are in 'src/integTest'. To run integration tests you would need to 
do `./gradlew check` or `./gradlew build`. For unittests just doing `./gradlew test` is enough.

Test logging is controlled via `logback-test.xml`. Be aware that integration tests generate a lot of debug information.
Please do not commit the config file back with DEBUG turned on.

=== Running single test suites

If you only want to run the unittests in say `JRubyPlugin` then you can do `./gradlew test --tests JRubyPlugin`.
In a similar manner for integration tests one can do `./gradlew integrationTest --tests JRubyIntegrationSpec`.

=== Running tests in IntelliJ

Go to `File` -> `Settings` -> `Build, Execution, Deployment` -> `Gradle` -> `Runner`, then check `Delete build/run actions to Gradle` and select `Platform Test Runner`.

On a Mac use `IntelliJ IDEA` -> `Preferences` instead of `File/Settings`.