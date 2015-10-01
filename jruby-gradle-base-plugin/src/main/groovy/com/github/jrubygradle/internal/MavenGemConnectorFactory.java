package com.github.jrubygradle.internal;

//import org.gradle.authentication.Authentication;
import org.gradle.internal.resource.connector.ResourceConnectorFactory;
import org.gradle.internal.resource.connector.ResourceConnectorSpecification;
import org.gradle.internal.resource.transfer.ExternalResourceConnector;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MavenGemConnectorFactory implements ResourceConnectorFactory {

    @Override
    public Set<String> getSupportedProtocols() {
        return Collections.singleton("mavengem");
    }

    //  @Override
    // public Set<Class<? extends Authentication>> getSupportedAuthentication() {
    //        return new HashSet<Class<? extends Authentication>>();
    //}

    @Override
    public ExternalResourceConnector createResourceConnector(ResourceConnectorSpecification connectionDetails) {
        return new MavenGemResourceConnector();
    }
}
