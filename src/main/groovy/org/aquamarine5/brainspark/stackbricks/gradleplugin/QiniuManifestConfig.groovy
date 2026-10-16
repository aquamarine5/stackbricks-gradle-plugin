package org.aquamarine5.brainspark.stackbricks.gradleplugin

class QiniuManifestConfig extends StackbricksManifestConfig {
    QiniuManifestConfig() {
        this.authority = QiniuUploader.AUTHORITY
        this.isHttps = false
    }

    QiniuManifestConfig(Map args) {
        this.authority = QiniuUploader.AUTHORITY
        this.isHttps = false
        args?.each { k, v -> this[k as String] = v }
    }
}