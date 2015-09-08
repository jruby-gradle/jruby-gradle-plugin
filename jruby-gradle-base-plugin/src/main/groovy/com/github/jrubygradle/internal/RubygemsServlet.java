package com.github.jrubygradle.internal;

import java.io.File;
import java.net.URL;
import java.util.Set;
import java.util.HashSet;

import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.webapp.*;
import org.eclipse.jetty.util.log.*;

public class RubygemsServlet {

    private final Server server = new Server();
    private final StdErrLog log = new StdErrLog();
    private final ServerConnector connector;
    private final HandlerCollection handlerCollection = new HandlerCollection();
    private final File cachedir;
    private final String rubygemsWarURI;
    private boolean enabledLogging = true;

    // static method to help groovy to create instance
    public static RubygemsServlet create(URL url){
        return new RubygemsServlet(url);
    }

    public RubygemsServlet(URL rubygemsWarURL){
        this.rubygemsWarURI = rubygemsWarURL.toString();
        this.cachedir = new File(System.getProperty("user.home"), ".gradle/rubygems");

        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setRequestHeaderSize(8192);
        this.connector = new ServerConnector(server,
                                             new HttpConnectionFactory(http_config));
        // pick random free port
        this.connector.setPort(0);
        this.connector.setHost("localhost");
        this.server.setConnectors(new Connector[] { connector });

        log.setLevel(3);
        Log.setLog(log);
    }

    private Set<String> urls = new HashSet<String>();
    public String addRepository(String url) {
        if (urls.contains(url)) {
            return null;
        }
        String path = "/" + url.replace("://", "_")
                               .replace(":", "_")
                               .replace("/", "_")
                               .replace(".", "_");
        WebAppContext context = new WebAppContext();
        context.setServer(server);
        context.setContextPath(path);
        context.setExtractWAR(false);
        context.setCopyWebInf(true);
        context.setWar(rubygemsWarURI);
        context.setInitParameter("gem-caching-proxy-url", url);
        context.setInitParameter("gem-caching-proxy-storage",
                                 new File(cachedir, path).getAbsolutePath());
        // do not setup other repos
        context.setInitParameter("gem-proxy-storage", "");
        context.setInitParameter("gem-hosted-storage", "");
        context.setInitParameter("gem-merged", "false");

        this.handlerCollection.addHandler(context);

        return path + "/caching/maven/releases";
    }

    public void enableLogging() {
        this.log.setLevel(2);
    }

    public String getURL(String path) {
        return "http://localhost:" + this.connector.getLocalPort() + path;
    }

    public void start() throws Exception {
        this.server.setHandler(handlerCollection);
        this.server.start();
    }

    public void stop() throws Exception {
        this.server.stop();
        this.server.join();
    }
}
