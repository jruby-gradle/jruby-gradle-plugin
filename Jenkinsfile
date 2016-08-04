#!/usr/bin/env groovy

node {
    stage 'Checkout source'
    checkout scm

    stage 'Build and test'
    List<String> plugins = ['base', 'war', 'jar']
    Map parallelSteps = [:]

    for (int i = 0; i < plugins.size(); i++) {
        def plugin = "jruby-gradle-${plugins.get(i)}-plugin"
        parallelSteps[plugin] = {
            node('docker') {
                checkout scm
                docker.image('java:8-jdk').inside {
                    timeout(30) {
                        sh "./gradlew -Si ${plugin}:check ${plugin}:gradleTest ${plugin}:assemble"
                    }
                }
                junit 'build/test-results/**/*.xml'
                archiveArtifacts artifacts: 'build/libs/*.jar,build/*.zip', fingerprint: true
            }
        }
    }
    parallel(parallelSteps)
}
