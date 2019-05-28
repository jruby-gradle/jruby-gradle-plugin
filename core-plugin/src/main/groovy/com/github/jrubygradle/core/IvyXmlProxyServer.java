package com.github.jrubygradle.core;

import java.net.URI;
import java.nio.file.Path;

public interface IvyXmlProxyServer extends Runnable {
    URI getBindAddress();
    Path ivyFile(String group, String name, String revision);
    void setRefreshDependencies(boolean refresh);
}
