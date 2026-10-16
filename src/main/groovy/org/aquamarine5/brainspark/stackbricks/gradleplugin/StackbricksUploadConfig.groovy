package org.aquamarine5.brainspark.stackbricks.gradleplugin

import com.alibaba.fastjson2.JSONObject

abstract class StackbricksUploadConfig {
    String authority
    String host
    Boolean isHttps = true
    String referer

    abstract StackbricksUploader createUploader()

    abstract JSONObject toDownloadPath(String filename)

    String getPublicBaseUrl() {
        return (isHttps ? "https" : "http") + "://" + host
    }
}