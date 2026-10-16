package org.aquamarine5.brainspark.stackbricks.gradleplugin

import com.alibaba.fastjson2.JSONObject

class QiniuUploadConfig extends StackbricksUploadConfig {
    String accessKey
    String secretKey
    String bucket

    QiniuUploadConfig() {
        this.authority = QiniuUploader.AUTHORITY
        this.isHttps = false
    }

    QiniuUploadConfig(Map args) {
        this.authority = QiniuUploader.AUTHORITY
        this.isHttps = false
        args?.each { k, v -> this[k as String] = v }
    }

    @Override
    StackbricksUploader createUploader() {
        return new QiniuUploader(this)
    }

    @Override
    JSONObject toDownloadPath(String filename) {
        return new JSONObject()
                .fluentPut("filename", filename)
                .fluentPut("authority", authority)
                .fluentPut("additionalInformation", new JSONObject()
                        .fluentPut("host", host)
                        .fluentPut("isHttps", isHttps))
    }
}