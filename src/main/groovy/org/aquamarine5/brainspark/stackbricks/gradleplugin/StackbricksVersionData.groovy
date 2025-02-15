package org.aquamarine5.brainspark.stackbricks.gradleplugin

/**
 * StackbricksVersionData
 *
 * This class represents the version data for Stackbricks.
 */
class StackbricksVersionData {
    int versionCode;
    String versionName;
    String downloadUrl;
    Date releaseDate;
    String packageName

    StackbricksVersionData(int versionCode,
                           String versionName,
                           String downloadApkUrl,
                           Date releaseDate,
                           String packageName) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.downloadUrl = downloadApkUrl;
        this.releaseDate = releaseDate;
        this.packageName = packageName;
    }
}
