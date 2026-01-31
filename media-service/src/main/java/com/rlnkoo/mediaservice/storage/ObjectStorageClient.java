package com.rlnkoo.mediaservice.storage;

public interface ObjectStorageClient {

    void putObject(String bucket, String key, byte[] bytes, String contentType);

    void deleteObject(String bucket, String key);

    boolean exists(String bucket, String key);
}