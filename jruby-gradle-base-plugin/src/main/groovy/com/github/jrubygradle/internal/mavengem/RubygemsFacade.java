package com.github.jrubygradle.internal.mavengem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jruby.embed.IsolatedScriptingContainer;
import org.jruby.embed.ScriptingContainer;
import org.sonatype.nexus.ruby.DefaultRubygemsGateway;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.GemArtifactFile;
import org.sonatype.nexus.ruby.IOUtil;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.CachingProxyStorage;
import org.sonatype.nexus.ruby.layout.ProxiedRubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.ProxyStorage;

public class RubygemsFacade {

    private static RubygemsGateway gateway = new DefaultRubygemsGateway(new IsolatedScriptingContainer());

    private static Map<URL, RubygemsFacade> facades = new HashMap<URL, RubygemsFacade>();

    public static synchronized RubygemsFacade getOrCreate(URL url) {
        RubygemsFacade result = facades.get(url);
        if (result == null) {
            result = new RubygemsFacade(url);
            facades.put(url, result);
        }
        return result;
    }

    private final ProxyStorage storage;
    private final RubygemsFileSystem files;

    private RubygemsFacade(URL url) {
        String path = ".gradle/rubygems/" + url.toString().replaceAll("[/:.]", "_");
        File cachedir =  new File(System.getProperty("user.home"), path);
        storage = new CachingProxyStorage(cachedir, url);
        files = new ProxiedRubygemsFileSystem(gateway, storage);
    }

    public InputStream getInputStream(RubygemsFile file) throws IOException {
        return this.storage.getInputStream(file);
    }

    public RubygemsFile get(String path) {
        return this.files.get(path);
    }
}

