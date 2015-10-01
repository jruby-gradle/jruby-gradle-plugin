package com.github.jrubygradle.internal;

import org.gradle.internal.Factory;
import org.gradle.internal.resource.ExternalResource;
import org.gradle.internal.resource.local.LocalResource;
import org.gradle.internal.resource.metadata.DefaultExternalResourceMetaData;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;
import org.gradle.internal.resource.transfer.ExternalResourceConnector;
import org.gradle.internal.resource.transfer.ExternalResourceReadResponse;
import org.gradle.internal.hash.HashValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

public class MavenGemResourceConnector implements ExternalResourceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenGemResourceConnector.class);

    @Override
    public List<String> list(URI parent) {
        LOGGER.debug("Listing parent resources: {}", parent);
        return null;
    }

    public ExternalResourceReadResponse openResource(URI location) {
        LOGGER.debug("Attempting to get resource: {}", location);
        return new MavenGemResource(location);
    }

    public ExternalResource getResource(URI location) {
        LOGGER.debug("Attempting to get resource: {}", location);
        return new MavenGemResource(location);
    }

    @Override
    public ExternalResourceMetaData getMetaData(URI location) {
        LOGGER.debug("Attempting to get resource metadata: {}", location);
        return  new MavenGemResource(location).getMetaData();
        // S3Object s3Object = s3Client.getMetaData(location);
        // if (s3Object == null) {
        //     return null;
        // }
        // ObjectMetadata objectMetadata = s3Object.getObjectMetadata();
        // return new DefaultExternalResourceMetaData(location,
        //         objectMetadata.getLastModified().getTime(),
        //         objectMetadata.getContentLength(),
        //         objectMetadata.getContentType(),
        //         objectMetadata.getETag(),
        //         null); // Passing null for sha1 - TODO - consider using the etag which is an MD5 hash of the file (when less than 5Gb)
    }

    //@Override
    public void upload(LocalResource resource, URI destination) throws IOException {
        LOGGER.error("Scheme can not upload stream to : {}", destination);
    }

    public void upload(Factory<InputStream> sourceFactory, Long contentLength, URI destination) throws IOException {
    }

    @Override
    public HashValue getResourceSha1(URI uri) {
        return null;
    }
}
