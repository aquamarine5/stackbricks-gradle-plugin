package org.aquamarine5.brainspark.stackbricks.gradleplugin

/**
 * StackbricksVersionDataV2
 *
 * This class represents the newest version data for Stackbricks.
 */
class StackbricksVersionDataV3 {
    String packageName
    int versionCode
    String versionName
    String downloadUrl
    Long releaseDate
    String changelog
    @Deprecated
    boolean forceInstall
    int forceInstallLessVersion

    StackbricksVersionDataV3(String packageName,
                             int versionCode,
                             String versionName,
                             String downloadFilename,
                             Long releaseDate,
                             String changelog,
                             boolean forceInstall,
                             int forceInstallLessVersion) {
        this.packageName = packageName
        this.versionCode = versionCode
        this.versionName = versionName
        this.downloadUrl = downloadFilename
        this.releaseDate = releaseDate
        this.changelog = changelog
        this.forceInstall = forceInstall
        this.forceInstallLessVersion = forceInstallLessVersion
    }
}