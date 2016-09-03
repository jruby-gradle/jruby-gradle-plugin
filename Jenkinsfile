#!/usr/bin/env groovy

node('docker') {
    stage 'Checkout source'
    checkout scm

    stage 'Build and test'
    List<String> javas = ['7', '8']
    List<String> plugins = ['base', 'war', 'jar']
    Map parallelSteps = [:]

    for (int j = 0; j < javas.size(); j++) {
        for (int i = 0; i < plugins.size(); i++) {
            def javaVersion = "${javas.get(j)}-jdk"
            def plugin = "jruby-gradle-${plugins.get(i)}-plugin"
            parallelSteps["${javaVersion}-${plugin}"] = {
                node('docker') {
                    checkout scm
                    try {
                        docker.image("openjdk:${javaVersion}").inside {
                            timeout(45) {
                                sh "./gradlew -Si ${plugin}:check ${plugin}:gradleTest ${plugin}:assemble"
                            }
                        }
                    }
                    finally {
                        junit '**/build/test-results/**/*.xml'
                        archiveArtifacts artifacts: '**/build/libs/*.jar,build/*.zip', fingerprint: true
                    }
                }
            }
        }
    }
    parallel(parallelSteps)
}
