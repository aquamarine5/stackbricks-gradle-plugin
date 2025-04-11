package org.aquamarine5.brainspark.stackbricks.gradleplugin


import com.alibaba.fastjson2.JSONObject
import com.qiniu.storage.Configuration
import com.qiniu.storage.Region
import com.qiniu.storage.UploadManager
import com.qiniu.util.Auth
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Instant

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
    static void uploadFileByQiniu(File file, QiniuConfigurationExtension qiniuConfig, String filename = null, Boolean overwrite = false) {
        def auth = Auth.create(qiniuConfig.accessKey, qiniuConfig.secretKey)
        def uploadToken =
                overwrite ? auth.uploadToken(qiniuConfig.bucket, filename ?: file.name) : auth.uploadToken(qiniuConfig.bucket)
        println(uploadToken)
        def uploadConfig = new Configuration(Region.qvmHuabei())
        uploadConfig.useHttpsDomains = false
        uploadConfig.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2
        def uploadManager = new UploadManager(uploadConfig)
        def fileToken = filename ?: file.name
        def response = uploadManager.put(file.path, fileToken, uploadToken)
        println response.bodyString()
    }

    static Boolean isStable(Project project) {
        def versionName = project.android.defaultConfig.versionName as String
        return !(versionName.contains("alpha") || versionName.contains("beta") || versionName.contains("rc"))
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
        def isStable = isStable(project)
        def forceInstall = stackbricksConfig.forceInstall
        def url = new URI("http://${stackbricksConfig.host}/${stackbricksConfig.configJsonFilePath}").toURL()
        println url
        def configFile = project.layout.buildDirectory.file("stackbricks_manifest_v2.tmp.json").get().asFile
        def okhttpClient = new OkHttpClient()
        def requestBuilder = new Request.Builder().url(url).get()
        if (stackbricksConfig.qiniuConfiguration.referer != null
                && stackbricksConfig.qiniuConfiguration.referer != "") {
            requestBuilder.addHeader("Referer", stackbricksConfig.qiniuConfiguration.referer)
        }
        def request = requestBuilder.build()
        def response = okhttpClient.newCall(request).execute()
        println response.code()
        println response.message()
        if (!response.successful) {
            throw new RuntimeException("Failed to download the Stackbricks configuration file.")
        }
        def str = response.body().string()
        println "previously: $str"
        def json = JSONObject.parseObject(str)
        def versionData = new StackbricksVersionDataV2(
                applicationId,
                versionCode,
                versionName,
                filename,
                Instant.now().toEpochMilli(),
                stackbricksConfig.changelog,
                forceInstall
        )
        if (isStable)
            json.replace("latestStable", versionData)
        else
            json.replace("latestTest", versionData)
        def newStr = json.toJSONString()
        println "new: $newStr"
        Files.writeString(configFile.toPath(), newStr, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        uploadFileByQiniu(configFile, stackbricksConfig.qiniuConfiguration, stackbricksConfig.configJsonFilePath, true)
    }
}