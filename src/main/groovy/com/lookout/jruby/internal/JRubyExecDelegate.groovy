package com.lookout.jruby.internal

import org.gradle.api.Project

/**
 * @author Schalk W. Cronjé
 */
class JRubyExecDelegate {
    def passthrough = []
    String script

    def methodMissing(String name, args) {
        if( name == 'args' || name == 'setArgs' ) {
            throw new UnsupportedOperationException("Use jrubyArgs/scriptArgs instead")
        }
        if( name == 'main' ) {
            throw new UnsupportedOperationException("Setting main class for jruby is not a valid operation")
        }

        if(args.size() == 1) {
            passthrough.add( [ "${name}" : args[0] ] )
        } else {
            passthrough.add( [ "${name}" : args ] )
        }
    }
//
//    static def jrubyexecDelegatingClosure = { Project project, Closure cl ->
//        def proxy =  new JRubyExecProxy()
//        Closure cl2 = cl.clone()
//        cl2.delegate = proxy
//        cl2.call()
//
//        if( proxy.script == null ) {
//            throw new NullPointerException("'script' is not set")
//        }
//
//        project.javaexec {
//            proxy.passthrough.each { item ->
//                def k = item.keySet()[0]
//                def v = item.values()[0]
//                "${k}" v
//            }
//            main 'org.jruby.Main'
//        }
//    }
//
//    static void addToProject(Project project) {
//        project.ext {
//            jrubyexec = JRubyExecProxy.jrubyexecDelegatingClosure.curry(project)
//        }
//    }
}



/*
class Proxy {
    def data = []
    def methodMissing(String name, args) {
        if(args.size() == 1) {
            data.add( [ "${name}" : args[0] ] )
        } else {
            data.add( [ "${name}" : args ] )
        }
    }
}

class Mine {
    Proxy p = new Proxy()
    def nm = new NotMine()
    def runThis(Closure cl) {
        Closure cl2 = cl.clone()
        cl2.delegate = p
        cl2.call()
        nm.configure {
            p.data.each { item ->
                def k = item.keySet()[0]
                def v = item.values()[0]
                "${k}" v
            }
        }
    }

 */