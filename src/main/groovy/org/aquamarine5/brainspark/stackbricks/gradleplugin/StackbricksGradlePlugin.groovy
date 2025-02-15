package org.aquamarine5.brainspark.stackbricks.gradleplugin


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
import java.nio.file.StandardOpenOption

/**
 * StackbricksGradlePlugin implements a Gradle plugin that provides tasks for uploading APK files (by {@link QiniuConfigurationExtension} or etc.)
 * and updating the Stackbricks configuration.
 */
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
                    def filename = uploadApkByQiniu(project, apkFile, config.qiniuConfiguration)
                    updateStackbricksConfig(project, filename, config)
                } else {
                    throw new NullPointerException("QiniuConfig is required when uploadApkByQiniu is executed.")
                }
            }
        }
    }

    /**
     * Uploads a apk file to Qiniu.
     *
     * @param project the Gradle project
     * @param apkFile the apk file to upload
     * @param qiniuConfig the Qiniu configuration
     * @return the filename of the uploaded file
     * @see QiniuConfigurationExtension
     */
    static String uploadApkByQiniu(Project project, File apkFile, QiniuConfigurationExtension qiniuConfig) {
        def versionCode = project.android.defaultConfig.versionCode as Integer
        def versionName = project.android.defaultConfig.versionName as String
        def applicationId = project.android.defaultConfig.applicationId as String
        def filename = "$versionCode-$versionName-${applicationId}.apk"
        uploadFileByQiniu(apkFile, qiniuConfig, filename)
        return filename
    }

    /**
     * Uploads a file to Qiniu.
     * @param file the file to upload
     * @param qiniuConfig the Qiniu configuration
     * @param filename the filename of the uploaded file, if null, the original filename will be used
     * @see QiniuConfigurationExtension
     */
    static void uploadFileByQiniu(File file, QiniuConfigurationExtension qiniuConfig, String filename = null) {
        def auth = Auth.create(qiniuConfig.accessKey, qiniuConfig.secretKey)
        def uploadToken = auth.uploadToken(qiniuConfig.bucket)
        println(uploadToken)
        def uploadConfig = new Configuration(Region.qvmHuabei())
        uploadConfig.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2
        def uploadManager = new UploadManager(uploadConfig)
        def fileToken = filename ?: file.name
        def response = uploadManager.put(file.path, fileToken, uploadToken)
        println response.bodyString()
    }

    /**
     * Updates the Stackbricks configuration with the new version data.
     *
     * @param project the Gradle project
     * @param filename the filename of the uploaded file
     * @param stackbricksConfig the Stackbricks configuration
     */
    static void updateStackbricksConfig(Project project, String filename, StackbricksConfigurationExtension stackbricksConfig) {
        def versionCode = project.android.defaultConfig.versionCode as Integer
        def versionName = project.android.defaultConfig.versionName as String
        def applicationId = project.android.defaultConfig.applicationId as String
        def url = new URI("http://${stackbricksConfig.host}/${stackbricksConfig.configJsonFilePath}").toURL()
        def configFile = project.layout.buildDirectory.file("stackbricks_config_v1.tmp.json").get().asFile
        def connection = url.openConnection()
        if(stackbricksConfig.qiniuConfiguration.referer != null){
            connection.addRequestProperty("Referer", stackbricksConfig.qiniuConfiguration.referer)
        }
        def source = Okio.source(connection.getInputStream())
        def sink = Okio.buffer(Okio.sink(configFile))
        sink.writeAll(source)
        def str = Files.readString(configFile.toPath(), StandardCharsets.UTF_8)
        println "previously: $str"
        def json = JSONObject.parseObject(str)
        def versionData = new StackbricksVersionData(versionCode, versionName, filename, new Date(), applicationId)
        json.getJSONArray("versions").add(
                versionData
        )
        json.replace("latest", versionData)
        def newStr=json.toJSONString()
        println "new: $newStr"
        Files.writeString(configFile.toPath(), newStr, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)
        uploadFileByQiniu(configFile, stackbricksConfig.qiniuConfiguration, stackbricksConfig.configJsonFilePath)
    }
}