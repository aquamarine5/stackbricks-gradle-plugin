package org.aquamarine5.brainspark.stackbricks.gradleplugin

/**
 * StackbricksVersionDataV2
 *
 * This class represents the newest version data for Stackbricks.
 */
class StackbricksVersionDataV2 {
    String packageName
    int versionCode
    String versionName
    String downloadUrl
    Long releaseDate
    String changelog
    boolean forceInstall

    StackbricksVersionDataV2(String packageName,
                             int versionCode,
                             String versionName,
                             String downloadFilename,
                             Long releaseDate,
                             String changelog,
                             boolean forceInstall) {
        this.packageName = packageName
        this.versionCode = versionCode
        this.versionName = versionName
        this.downloadUrl = downloadFilename
        this.releaseDate = releaseDate
        this.changelog = changelog
        this.forceInstall = forceInstall
    }
}