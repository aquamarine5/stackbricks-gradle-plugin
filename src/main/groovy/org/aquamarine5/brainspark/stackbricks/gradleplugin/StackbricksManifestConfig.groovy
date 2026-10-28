package org.aquamarine5.brainspark.stackbricks.gradleplugin

abstract class StackbricksManifestConfig {
    String authority
    String host
    Boolean isHttps = true
    String configFilePath
    String referer

    String getManifestUrl() {
        return (isHttps ? "https" : "http") + "://" + host + "/" + configFilePath
    }
}