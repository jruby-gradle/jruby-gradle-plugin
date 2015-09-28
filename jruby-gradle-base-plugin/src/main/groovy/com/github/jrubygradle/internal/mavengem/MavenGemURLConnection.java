package com.github.jrubygradle.internal.mavengem;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.GemArtifactFile;
import org.sonatype.nexus.ruby.IOUtil;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.Storage;

public class MavenGemURLConnection extends URLConnection {

    private final String PING = "/ping";

    private InputStream in;

    // package private for testing
    final URL baseurl;
    final String path;

    protected MavenGemURLConnection(URL baseurl, String path) {
        super(baseurl);
        this.baseurl = baseurl;
        this.path = path;
    }

    @Override
    synchronized public InputStream getInputStream() throws IOException {
        if (in == null) {
            connect();
        }
        return in;
    }

    private int counter = 12; // seconds
    @Override
    synchronized public void connect() throws IOException {
        connect(RubygemsFacade.getOrCreate(baseurl));
    }

    private void connect(RubygemsFacade facade) throws IOException {
        RubygemsFile file = facade.get(path);
        switch( file.state() )
        {
        case FORBIDDEN:
            throw new IOException("forbidden: " + path);
        case NOT_EXISTS:
            if (path.equals(PING)) {
                in = new ByteArrayInputStream("pong".getBytes());
                break;
            }
            throw new FileNotFoundException(path);
        case NO_PAYLOAD:
            switch( file.type() )
            {
            case GEM_ARTIFACT:
                // we can pass in null as dependenciesData since we have already the gem
                in = new URL(baseurl + "/gems/" + ((GemArtifactFile) file ).gem( null ).filename() + ".gem" ).openStream();
            case GEM:
                in = new URL(baseurl + "/" +  file.remotePath()).openStream();
            default:
                throw new FileNotFoundException(path + " has no view - not implemented");
            }
        case ERROR:
            throw new IOException(file.getException());
        case TEMP_UNAVAILABLE:
            try {
                Thread.currentThread().sleep(1000);
            }
            catch(InterruptedException ignore) {
            }
            if (--counter > 0) {
                connect(facade);
            }
            break;
        case PAYLOAD:
            in = facade.getInputStream(file);
            break;
        case NEW_INSTANCE:
        default:
            throw new RuntimeException("BUG: should never reach here");
        }
    } 
} 

