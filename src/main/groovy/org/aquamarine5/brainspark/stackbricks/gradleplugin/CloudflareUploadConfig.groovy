package org.aquamarine5.brainspark.stackbricks.gradleplugin

import com.alibaba.fastjson2.JSONObject

class CloudflareUploadConfig extends StackbricksUploadConfig {
    String accountId
    String bucketName
    String accessKeyId
    String secretAccessKey
    String region = "auto"
    String s3Endpoint

    CloudflareUploadConfig() {
        this.authority = CloudflareUploader.AUTHORITY
        this.isHttps = true
    }

    CloudflareUploadConfig(Map args) {
        this.authority = CloudflareUploader.AUTHORITY
        this.isHttps = true
        args?.each { k, v -> this[k as String] = v }
    }

    @Override
    StackbricksUploader createUploader() {
        return new CloudflareUploader(this)
    }

    @Override
    JSONObject toDownloadPath(String filename) {
        return new JSONObject()
                .fluentPut("filename", filename)
                .fluentPut("authority", authority)
                .fluentPut("additionalInformation", new JSONObject()
                        .fluentPut("baseUrl", getPublicBaseUrl())
                        .fluentPut("accountId", accountId)
                        .fluentPut("bucketName", bucketName))
    }
}