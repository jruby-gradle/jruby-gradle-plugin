package com.github.jrubygradle.internal;

import org.gradle.internal.resource.metadata.DefaultExternalResourceMetaData;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;
import org.gradle.internal.resource.transfer.ExternalResourceReadResponse;
import org.gradle.internal.resource.ExternalResource;
import org.gradle.api.Action;
import org.gradle.api.Transformer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.Date;

public class MavenGemResource implements ExternalResource,ExternalResourceReadResponse {

    private final URI uri;
    private URLConnection connection;
    private InputStream in;
    
    public MavenGemResource(URI uri) {
        this.uri = uri;
    }

    private URLConnection connection() throws IOException {
        if (connection == null) {
            connection = uri.toURL().openConnection();
        }
        return connection;
    }

    // gradle 2.3 API
    public String getName(){
        return "Hase";
    }

    public void writeTo(File destination) throws IOException{
    }

    public void writeTo(OutputStream destination) throws IOException{
    }

    public void withContent(Action<? super InputStream> readAction) throws IOException {
    }

    public <T> T withContent(Transformer<? extends T, ? super InputStream> readAction) throws IOException {
        return null;
    }

    // gradle 2.6 API
    public InputStream openStream() throws IOException {
        if (in == null) {
            in = connection().getInputStream();
        }
        return in;
    }

    // common API
    public URI getURI() {
        return uri;
    }

    public long getContentLength() {// throws IOException {
        return 0;//connection().getContentLength();
    }

    public String getContentType() {
        try {
            return connection().getContentType();
        }
        catch(IOException e) {
            return "text/plain";
        }
    }

    public Date getLastModified() {
        try {
            return new Date(connection().getLastModified());
        }
        catch(IOException e) {
            return new Date(0);
        }
    }
    
    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public ExternalResourceMetaData getMetaData() {
        return new DefaultExternalResourceMetaData(uri,
                                                   getLastModified(),
                                                   getContentLength(),
                                                   getContentType(),
                                                   null);
    }

    @Override
    public void close() throws IOException {
        if (in != null) in.close();
        this.in = null;
        this.connection = null;
    }
}
