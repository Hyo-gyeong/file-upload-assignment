package com.fileupload.file.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.r2")
public record R2Properties(
    String endpoint,
    String bucket,
    String region,
    String accessKeyId,
    String secretAccessKey
) {

    @Override
    public String toString() {
        return "R2Properties[" +
            "endpoint=" + endpoint +
            ", bucket=" + bucket +
            ", region=" + region +
            ", accessKeyId=***" +
            ", secretAccessKey=***" +
            "]";
    }
}