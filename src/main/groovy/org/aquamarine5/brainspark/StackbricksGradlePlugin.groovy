package org.aquamarine5.brainspark

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.qiniu.storage.Configuration
import com.qiniu.storage.Region
import com.qiniu.storage.UploadManager
import com.qiniu.util.Auth
import okio.Okio
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class StackbricksGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create("stackbricksConfig", StackbricksConfigurationExtension)
        project.tasks.register("uploadApkByQiniu") {
            it.dependsOn "assembleRelease"
            it.doLast {
                def config = project.extensions.findByType(StackbricksConfigurationExtension)
                if (config != null) {
                    def apkFile = project.layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
                    def filename = uploadFileByQiniu(project, apkFile, config.qiniuConfiguration)
                    updateStackbricksConfig(project, filename, config)
                } else {
                    throw new NullPointerException("QiniuConfig is required when uploadApkByQiniu is executed.")
                }
            }
        }
    }

    static String uploadFileByQiniu(Project project, File file, QiniuConfigurationExtension qiniuConfig) {
        def versionCode = project.android.defaultConfig.versionCode as Integer
        def versionName = project.android.defaultConfig.versionName as String
        def applicationId = project.android.defaultConfig.applicationId as String
        def auth = Auth.create(qiniuConfig.accessKey, qiniuConfig.secretKey)
        def uploadToken = auth.uploadToken(qiniuConfig.bucket)
        def uploadConfig = new Configuration(Region.qvmHuabei())
        uploadConfig.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2
        def uploadManager = new UploadManager(uploadConfig)
        def filename = "$versionCode-$versionName-${applicationId}.apk"
        def response = uploadManager.put(file.path, file.name, uploadToken)
        println response.bodyString()
        return filename
    }

    static void updateStackbricksConfig(Project project, String filename, StackbricksConfigurationExtension stackbricksConfig) {
        def versionCode = project.android.defaultConfig.versionCode as Integer
        def versionName = project.android.defaultConfig.versionName as String
        def applicationId = project.android.defaultConfig.applicationId as String
        def url = new URI("http://${stackbricksConfig.host}/${stackbricksConfig.configJsonFilePath}").toURL()
        def configFile = project.layout.buildDirectory.file("stackbricks_config_v1.tmp.json").get().asFile
        def source = Okio.source(url.openStream())
        def sink = Okio.buffer(Okio.sink(configFile))
        sink.writeAll(source)
        def str = Files.readString(configFile.toPath(), StandardCharsets.UTF_8)
        def json = JSONObject.parseObject(str)
        def versionData = new StackbricksVersionData(versionCode, versionName, filename, new Date(), applicationId)
        json.getJSONArray("versions").add(
                versionData
        )
        json.replace("latest", versionData)
    }
}