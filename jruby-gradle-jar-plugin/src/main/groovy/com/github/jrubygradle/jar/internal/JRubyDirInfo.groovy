package com.github.jrubygradle.jar.internal

import org.gradle.api.file.RelativePath

class JRubyDirInfo {

    private static final String NEW_LINE = System.getProperty("line.separator")
    private static final File OMIT = new File('')
    private static final List<String> omissionDirs = ['META-INF', 'gems', 'specifications',
                                                      'build_info', 'cache', 'doc' ,'bin']

    private final Map dirsCache = [:]

    private final File dirInfo

    JRubyDirInfo(File dir) {
        dirInfo = dir
        dirInfo.deleteDir();
    }

    File toFile(File path, String subdir) {
        if (path == null) {
          if (omissionDirs.contains(subdir)) {
              path = OMIT
          }
          else {
              path = new File(subdir)                        
          }
        }
        else if (path != OMIT) {
            path = new File(path, subdir)
        }
        path
    }

    void process(File path) {
        boolean isRoot = path.parent == null
        File file = new File(dirInfo,
                             isRoot ? '.jrubydir' : "${path.parent}/.jrubydir")
        String name = path.name
        File dir = file.parentFile
        def dirs = dirsCache[dir]
        if (dirs == null) {
            dirs = dirsCache[dir] = []
        }
        if (!dirs.contains(name)) {
            dirs << name
            dir.mkdirs()
            if (file.exists()) {
                file.append(name + NEW_LINE)
            }
            else {
                StringBuilder buf = new StringBuilder()
                buf.append('.').append(NEW_LINE)
                if (!isRoot) {
                    buf.append('..').append(NEW_LINE)
                }
                buf.append(name).append(NEW_LINE)
                file.write(buf.toString())
            }
        }
    }

    void add( RelativePath relativePath ) {
        if (!['jar-bootstrap.rb', 'MANIFEST.MF','.jrubydir'].contains(relativePath.lastName)) {
            File path = null
            relativePath.segments.each {
                path = toFile(path, it.toString())
                if (path != OMIT) {
                    process(path)
                }
            }
        }
    }
}
