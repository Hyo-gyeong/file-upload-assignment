package com.fileupload.file.storage;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@EnableConfigurationProperties(R2Properties.class)
public class R2Config {

    @Bean
    public S3Client r2S3Client(
        R2Properties properties
    ) {
        AwsBasicCredentials credentials =
            AwsBasicCredentials.create(
                properties.accessKeyId(),
                properties.secretAccessKey()
            );

        S3Configuration s3Configuration =
            S3Configuration.builder()
            // Java AWS SDK v2에서 R2를 사용할 때 chunked encoding을 비활성화하지 않으면
            // putObject에서 signature mismatch 403이 발생
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();

        return S3Client.builder()
            .endpointOverride(
                URI.create(properties.endpoint())
            )
            .region(
                Region.of(properties.region())
            )
            .credentialsProvider(
                StaticCredentialsProvider.create(credentials)
            )
            .serviceConfiguration(s3Configuration)
            .build();
    }
}