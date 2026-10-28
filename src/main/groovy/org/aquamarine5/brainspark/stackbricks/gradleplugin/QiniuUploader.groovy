package org.aquamarine5.brainspark.stackbricks.gradleplugin

import com.qiniu.storage.Configuration
import com.qiniu.storage.Region
import com.qiniu.storage.UploadManager
import com.qiniu.util.Auth

class QiniuUploader implements StackbricksUploader {
    static final String AUTHORITY = "org.aquamarine5.brainspark.stackbricks.downloadpath.qiniu"
    static final String TAG = "QiniuUploader"

    private final QiniuUploadConfig config

    QiniuUploader(QiniuUploadConfig config) {
        this.config = config
    }

    @Override
    String getAuthority() {
        return AUTHORITY
    }

    @Override
    void upload(File file, String objectKey, boolean overwrite = false) {
        if (config.accessKey == null || config.secretKey == null || config.bucket == null) {
            throw new IllegalStateException(
                    "${TAG}: accessKey, secretKey and bucket are required for a qiniuUploadConfig")
        }
        def auth = Auth.create(config.accessKey, config.secretKey)
        def uploadToken = overwrite
                ? auth.uploadToken(config.bucket, objectKey)
                : auth.uploadToken(config.bucket)
        def uploadConfig = Configuration.create(Region.qvmHuabei())
        uploadConfig.useHttpsDomains = false
        uploadConfig.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2
        def uploadManager = new UploadManager(uploadConfig)
        def response = uploadManager.put(file.path, objectKey, uploadToken)
        try {
            println "[${TAG}] ${response.statusCode} uploading ${file.name} as ${objectKey}: ${response.bodyString()}"
        } finally {
            response.close()
        }
    }
}