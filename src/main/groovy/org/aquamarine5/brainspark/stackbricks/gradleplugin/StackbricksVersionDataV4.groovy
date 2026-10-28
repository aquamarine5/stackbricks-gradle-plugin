package org.aquamarine5.brainspark.stackbricks.gradleplugin

import com.alibaba.fastjson2.JSONObject

class StackbricksVersionDataV4 {
    String packageName
    int versionCode
    String versionName
    List<JSONObject> possibleDownloadPath
    String downloadUrl
    Long releaseDate
    String changelog
    @Deprecated
    boolean forceInstall
    long forceInstallLessVersion

    StackbricksVersionDataV4(String packageName,
                             int versionCode,
                             String versionName,
                             List<JSONObject> possibleDownloadPath,
                             Long releaseDate,
                             String changelog,
                             boolean forceInstall,
                             long forceInstallLessVersion) {
        this.packageName = packageName
        this.versionCode = versionCode
        this.versionName = versionName
        this.possibleDownloadPath = possibleDownloadPath
        this.downloadUrl = resolveLegacyDownloadUrl(possibleDownloadPath)
        this.releaseDate = releaseDate
        this.changelog = changelog
        this.forceInstall = forceInstall
        this.forceInstallLessVersion = forceInstallLessVersion
    }

    static String resolveLegacyDownloadUrl(List<JSONObject> possibleDownloadPath) {
        return possibleDownloadPath?.find {
            it.getString("authority") == QiniuUploader.AUTHORITY
        }?.getString("filename")
    }
}