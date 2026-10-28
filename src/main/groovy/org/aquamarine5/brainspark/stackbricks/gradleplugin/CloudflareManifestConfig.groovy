package org.aquamarine5.brainspark.stackbricks.gradleplugin

class CloudflareManifestConfig extends StackbricksManifestConfig {
    CloudflareManifestConfig() {
        this.authority = CloudflareUploader.AUTHORITY
        this.isHttps = true
    }

    CloudflareManifestConfig(Map args) {
        this.authority = CloudflareUploader.AUTHORITY
        this.isHttps = true
        args?.each { k, v -> this[k as String] = v }
    }
}