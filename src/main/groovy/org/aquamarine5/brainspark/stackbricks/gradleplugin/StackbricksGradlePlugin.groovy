package org.aquamarine5.brainspark.stackbricks.gradleplugin


import com.alibaba.fastjson2.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Instant

class StackbricksGradlePlugin implements Plugin<Project> {
    private static final int LATEST_MANIFEST_VERSION = 4

    @Override
    void apply(Project project) {
        project.extensions.create("stackbricksConfig", StackbricksConfigurationExtension)

        project.tasks.register("uploadApk") {
            it.group = "stackbricks"
            it.description = "Uploads the release APK to every configured destination and updates the manifest"
            it.dependsOn "assembleRelease"
            it.doLast {
                def config = requireConfig(project)
                def destinations = config.resolveUploadDestinations()
                if (destinations.isEmpty()) {
                    throw new IllegalStateException(
                            "No upload destination is configured. Declare one inside " +
                                    "stackbricksConfig { uploadDestinationConfiguration { ... } }")
                }
                def apkFile = project.layout.buildDirectory
                        .file("outputs/apk/release/app-release.apk").get().asFile
                def versionCode = project.android.defaultConfig.versionCode as Integer
                def versionName = project.android.defaultConfig.versionName as String
                def applicationId = project.android.defaultConfig.applicationId as String
                def filename = "$versionCode-$versionName-${applicationId}.apk"

                def downloadPaths = []
                destinations.each { StackbricksUploadConfig destination ->
                    println "[uploadApk] Uploading ${filename} to ${destination.authority} (${destination.host})"
                    destination.createUploader().upload(apkFile, filename, false)
                    downloadPaths.add(destination.toDownloadPath(filename))
                }
                updateStackbricksConfig(project, downloadPaths, config, destinations)
            }
        }

        project.tasks.register("uploadApkByQiniu") {
            it.group = "stackbricks"
            it.description = "Uploads the release APK to Qiniu only"
            it.deprecated = "Use the uploadApk task with uploadDestinationConfiguration instead"
            it.dependsOn "assembleRelease"
            it.doLast {
                def config = requireConfig(project)
                def destinations = config.resolveUploadDestinations()
                if (destinations.isEmpty()) {
                    throw new NullPointerException("QiniuConfig is required when uploadApkByQiniu is executed.")
                }
                if (destinations.size() > 1) {
                    println "[uploadApkByQiniu] ${destinations.size()} destinations are configured; " +
                            "switch to the uploadApk task. Uploading the APK to all of them."
                }
                def apkFile = project.layout.buildDirectory
                        .file("outputs/apk/release/app-release.apk").get().asFile
                def versionCode = project.android.defaultConfig.versionCode as Integer
                def versionName = project.android.defaultConfig.versionName as String
                def applicationId = project.android.defaultConfig.applicationId as String
                def filename = "$versionCode-$versionName-${applicationId}.apk"

                def downloadPaths = []
                destinations.each { StackbricksUploadConfig destination ->
                    destination.createUploader().upload(apkFile, filename, false)
                    downloadPaths.add(destination.toDownloadPath(filename))
                }
                updateStackbricksConfig(project, downloadPaths, config, destinations)
            }
        }
    }

    private static StackbricksConfigurationExtension requireConfig(Project project) {
        def config = project.extensions.findByType(StackbricksConfigurationExtension)
        if (config == null) {
            throw new NullPointerException(
                    "stackbricksConfig extension is required. Apply the Stackbricks plugin to the project.")
        }
        return config
    }

    static String uploadApk(Project project, File apkFile, List<StackbricksUploadConfig> destinations) {
        def versionCode = project.android.defaultConfig.versionCode as Integer
        def versionName = project.android.defaultConfig.versionName as String
        def applicationId = project.android.defaultConfig.applicationId as String
        def filename = "$versionCode-$versionName-${applicationId}.apk"
        destinations.each { StackbricksUploadConfig destination ->
            destination.createUploader().upload(apkFile, filename, false)
        }
        return filename
    }

    @Deprecated
    static String uploadApkByQiniu(Project project, File apkFile, QiniuConfigurationExtension qiniuConfig) {
        def paths = project.extensions
                .findByType(StackbricksConfigurationExtension)?.possibleConfigurationPaths
        def host = paths == null || paths.isEmpty() ? null : paths[0]
        def config = new QiniuUploadConfig()
        config.host = host?.host
        config.isHttps = host?.isHttps != null ? host.isHttps : true
        config.accessKey = qiniuConfig.accessKey
        config.secretKey = qiniuConfig.secretKey
        config.bucket = qiniuConfig.bucket
        config.referer = qiniuConfig.referer
        return uploadApk(project, apkFile, [config] as List<StackbricksUploadConfig>)
    }

    @Deprecated
    static void uploadFileByQiniu(File file, QiniuConfigurationExtension qiniuConfig, String filename = null, Boolean overwrite = false) {
        def config = new QiniuUploadConfig()
        config.accessKey = qiniuConfig.accessKey
        config.secretKey = qiniuConfig.secretKey
        config.bucket = qiniuConfig.bucket
        config.referer = qiniuConfig.referer
        uploadFileByQiniu(file, config, filename, overwrite)
    }

    static void uploadFileByQiniu(File file, QiniuUploadConfig config, String filename = null, Boolean overwrite = false) {
        config.createUploader().upload(file, filename ?: file.name, overwrite)
    }

    static Boolean isStable(Project project) {
        def versionName = project.android.defaultConfig.versionName as String
        return !(versionName.contains("alpha") || versionName.contains("beta") || versionName.contains("rc"))
    }

    static void updateStackbricksConfig(Project project,
                                        List<JSONObject> possibleDownloadPath,
                                        StackbricksConfigurationExtension stackbricksConfig,
                                        List<StackbricksUploadConfig> destinations) {
        def versionCode = project.android.defaultConfig.versionCode as Integer
        def versionName = project.android.defaultConfig.versionName as String
        def applicationId = project.android.defaultConfig.applicationId as String

        if (possibleDownloadPath.isEmpty()) {
            throw new IllegalStateException("No upload succeeded, refusing to write a manifest without download paths")
        }
        def manifestDestinations = stackbricksConfig.resolveManifestDestinations()
        if (manifestDestinations.isEmpty()) {
            throw new IllegalStateException(
                    "No manifest destination is configured. Declare one inside " +
                            "stackbricksConfig { manifestDestinationConfigurations { ... } }")
        }
        def isStableRelease = isStable(project)
        def versionData = new StackbricksVersionDataV4(
                applicationId,
                versionCode,
                versionName,
                possibleDownloadPath,
                Instant.now().toEpochMilli(),
                stackbricksConfig.changelog,
                stackbricksConfig.requiredManifestVersion2MigrateForceInstall,
                stackbricksConfig.forceInstallLessVersion
        )

        def json = fetchPreviousManifest(manifestDestinations)
        if (json == null) {
            println "[uploadApk] No previous manifest found, creating a new one."
            json = new JSONObject()
        }
        json.put("latestTest", versionData)
        if (isStableRelease) {
            json.put("latestStable", versionData)
        }
        json.put("manifestVersion", LATEST_MANIFEST_VERSION)
        ["latestStable", "latestTest"].each { String key ->
            reconcileDownloadPaths(json, key, destinations)
        }
        if (json.getIntValue("manifestVersion") != LATEST_MANIFEST_VERSION
                || json.getJSONObject("latestStable") == null
                || json.getJSONObject("latestTest") == null) {
            throw new IllegalStateException("Refusing to publish a malformed manifest: ${json}")
        }
        def newStr = json.toJSONString()
        println "[uploadApk] new manifest: ${newStr}"

        def configFile = project.layout.buildDirectory.file("stackbricks_manifest.tmp.json").get().asFile
        Files.writeString(configFile.toPath(), newStr, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        manifestDestinations.each { StackbricksManifestConfig manifestDestination ->
            def uploader = destinations.find { it.authority == manifestDestination.authority }
            if (uploader == null) {
                throw new IllegalStateException(
                        "No upload destination registered for authority ${manifestDestination.authority}, " +
                                "required by manifest destination ${manifestDestination.manifestUrl}. " +
                                "Declare it inside stackbricksConfig { uploadDestinationConfiguration { ... } }. " +
                                "If this manifest destination came from the deprecated possibleConfigurationPaths, " +
                                "which always migrates to a Qiniu manifest destination, either add a qiniuUploadConfig " +
                                "or delete possibleConfigurationPaths and declare manifestDestinationConfigurations " +
                                "explicitly.")
            }
            println "[uploadApk] Publishing manifest to ${manifestDestination.manifestUrl}"
            uploader.createUploader().upload(configFile, manifestDestination.configFilePath, true)
        }
    }

    static void reconcileDownloadPaths(JSONObject json,
                                   String versionKey,
                                   List<StackbricksUploadConfig> destinations) {
        def entry = json.getJSONObject(versionKey)
        if (entry == null) {
            return
        }
        def declared = entry.getJSONArray("possibleDownloadPath")
        def configuredAuthorities = destinations.collect { it.authority } as Set
        def referencedAuthorities = declared == null
                ? new HashSet<String>()
                : (0..<declared.size()).collect { declared.getJSONObject(it).getString("authority") } as Set

        if (referencedAuthorities.isEmpty()) {
            def legacyFilename = entry.getString("downloadUrl")
            if (legacyFilename == null || legacyFilename.isEmpty()) {
                println "[uploadApk] ${versionKey} has no download path at all, leaving it untouched"
                return
            }
            referencedAuthorities.add(QiniuUploader.AUTHORITY)
            if (configuredAuthorities.contains(QiniuUploader.AUTHORITY)) {
                return
            }
        }
        def missing = referencedAuthorities.findAll { !configuredAuthorities.contains(it) }
        if (missing.isEmpty()) {
            return
        }

        def filename = declared == null || declared.isEmpty()
                ? entry.getString("downloadUrl")
                : declared.getJSONObject(0).getString("filename")
        if (filename == null || filename.isEmpty()) {
            println "[uploadApk] ${versionKey} references an unconfigured authority " +
                    "${missing} but carries no filename, leaving it untouched"
            return
        }

        def repaired = destinations.collect { it.toDownloadPath(filename) }
        entry.put("possibleDownloadPath", repaired)
        entry.put("downloadUrl", StackbricksVersionDataV4.resolveLegacyDownloadUrl(repaired))
        println "[uploadApk] ${versionKey} referenced ${missing.join(", ")}, " +
                "which ${missing.size() == 1 ? "is" : "are"} not configured as an upload destination. " +
                "Repointed to ${destinations.collect { it.authority }.join(", ")} at ${filename}. " +
                "Clients that cannot reach the new destinations will not be able to fetch this version."
    }

    static JSONObject fetchPreviousManifest(List<StackbricksManifestConfig> manifestDestinations) {
        def okhttpClient = new OkHttpClient()
        for (int i = 0; i < manifestDestinations.size(); i++) {
            def destination = manifestDestinations[i]
            try {
                def url = URI.create(destination.getManifestUrl()).toURL()
                def response = requestManifest(okhttpClient, url, destination.referer)
                if (response.code() == 404 && (destination.referer == null || destination.referer.isEmpty())) {
                    response.close()
                    response = requestManifest(okhttpClient, url, null)
                }
                def code = response.code()
                def successful = response.successful
                def str = successful ? response.body().string() : null
                response.close()

                if (code == 404) {
                    println "[uploadApk] No manifest at ${url}, trying the next destination"
                    continue
                }
                if (!successful) {
                    throw new RuntimeException(
                            "Failed to download the previous Stackbricks configuration from ${url}: ${code}")
                }
                println "[uploadApk] previously: ${str}"
                return JSONObject.parseObject(str)
            } catch (Exception e) {
                println "[uploadApk] Failed to fetch the manifest from ${destination.host}: ${e.message}"
            }
        }
        return null
    }

    private static Response requestManifest(OkHttpClient client, URL url, String referer) {
        def requestBuilder = new Request.Builder().url(url).get()
        if (referer != null && !referer.isEmpty()) {
            requestBuilder.addHeader("Referer", referer)
        }
        return client.newCall(requestBuilder.build()).execute()
    }
}