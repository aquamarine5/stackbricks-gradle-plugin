package org.aquamarine5.brainspark.stackbricks.gradleplugin

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception

class CloudflareUploader implements StackbricksUploader {
    static final String AUTHORITY = "org.aquamarine5.brainspark.stackbricks.downloadpath.cloudflare"
    static final String TAG = "CloudflareUploader"

    private final CloudflareUploadConfig config

    CloudflareUploader(CloudflareUploadConfig config) {
        this.config = config
    }

    @Override
    String getAuthority() {
        return AUTHORITY
    }

    @Override
    void upload(File file, String objectKey, boolean overwrite) {
        validate()
        def endpoint = config.s3Endpoint ?: "https://${config.accountId}.r2.cloudflarestorage.com"
        def client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(config.region ?: "auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKeyId, config.secretAccessKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()
        try {
            if (!overwrite && exists(client, objectKey)) {
                println "[${TAG}] ${objectKey} already exists in ${config.bucketName}, skipping"
                return
            }
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(config.bucketName)
                            .key(objectKey)
                            .contentType(contentTypeOf(objectKey))
                            .build(),
                    RequestBody.fromFile(file))
            println "[${TAG}] uploaded ${objectKey} to s3://${config.bucketName}/${objectKey}"
        } catch (S3Exception e) {
            throw new RuntimeException("${TAG}: failed to upload ${objectKey} to ${config.bucketName}: ${e.awsErrorDetails()?.errorMessage() ?: e.message}", e)
        } finally {
            client.close()
        }
    }

    private boolean exists(S3Client client, String objectKey) {
        try {
            client.headObject(HeadObjectRequest.builder()
                    .bucket(config.bucketName)
                    .key(objectKey)
                    .build())
            return true
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false
            throw e
        }
    }

    private void validate() {
        def missing = ["accountId", "bucketName", "accessKeyId", "secretAccessKey"].findAll {
            config[it] == null || config[it].toString().isEmpty()
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "${TAG}: ${missing.join(", ")} ${missing.size() == 1 ? "is" : "are"} required for a cloudflareUploadConfig")
        }
    }

    private static String contentTypeOf(String objectKey) {
        if (objectKey.endsWith(".apk")) return "application/vnd.android.package-archive"
        if (objectKey.endsWith(".json")) return "application/json"
        return "application/octet-stream"
    }
}