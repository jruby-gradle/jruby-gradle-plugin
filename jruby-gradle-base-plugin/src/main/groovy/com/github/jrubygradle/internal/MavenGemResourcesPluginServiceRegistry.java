package com.github.jrubygradle.internal;

import org.gradle.internal.resource.connector.ResourceConnectorFactory;
import org.gradle.internal.service.ServiceRegistration;
import org.gradle.internal.service.scopes.PluginServiceRegistry;

public class MavenGemResourcesPluginServiceRegistry implements PluginServiceRegistry {

    public void registerGlobalServices(ServiceRegistration registration) {
        registration.addProvider(new GlobalScopeServices());
    }

    public void registerBuildSessionServices(ServiceRegistration registration) {
    }

    public void registerBuildServices(ServiceRegistration registration) {
    }

    public void registerGradleServices(ServiceRegistration registration) {
    }

    public void registerProjectServices(ServiceRegistration registration) {
    }

    private static class GlobalScopeServices {
        ResourceConnectorFactory createMavenGemConnectorFactory() {
            return new MavenGemConnectorFactory();
        }
    }
}
