package com.github.jrubygradle.internal.mavengem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {

    public static String KEY = "java.protocol.handler.pkgs";
    public static String PKG = "com.github.jrubygradle.internal";
    private final String MAVEN_RELEASES = "/maven/releases";
    private final int MAVEN_RELEASES_LEN = MAVEN_RELEASES.length();

    public synchronized static boolean isMavenGemProtocolRegistered() {
        return System.getProperties().contains(KEY);
    }

    public synchronized static boolean registerMavenGemProtocol() {
        if (System.getProperties().contains(KEY)) {
            String current = System.getProperty(KEY);
            if (!current.contains(PKG)) {
                System.setProperty(KEY, current + "|" + PKG);
            }
        }
        else {
            System.setProperty(KEY, PKG);
        }
        try {
            // this url works offline as /maven/releases/ping is
            // not a remote resource. but using the protocol here
            // will register this instance Handler and other
            // classloaders will be able to use the mavengem-protocol as well
            URL url = new URL("mavengem:https://rubygems.org/maven/releases/ping");
            // TODO fix this as it is broken
            return "pong".equals(((ByteArrayInputStream) url.openStream()));
        }
        catch(IOException e) {
            return false;
        }
    }

    private String path;
    private String baseurl;
 
    @Override
    protected void parseURL(URL u, String spec, int start, int end) {
        String uri = spec.substring(start, end);
        
        int index = uri.indexOf(MAVEN_RELEASES);
        path = uri.substring( index + MAVEN_RELEASES_LEN );
        baseurl = uri.substring(0, index);
        super.parseURL(u, spec, start, end); 
    } 

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new MavenGemURLConnection(new URL(baseurl), path);
    }
}
