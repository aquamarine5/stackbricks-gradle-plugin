package org.aquamarine5.brainspark.stackbricks.gradleplugin

import java.time.Instant

/**
 * StackbricksVersionData
 *
 * This class represents the version data for Stackbricks.
 */
class StackbricksVersionData {
    int versionCode;
    String versionName;
    String downloadUrl;
    Long releaseDate;
    String packageName

    StackbricksVersionData(int versionCode,
                           String versionName,
                           String downloadApkUrl,
                           Long releaseDate,
                           String packageName) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.downloadUrl = downloadApkUrl;
        this.releaseDate = releaseDate;
        this.packageName = packageName;
    }
}
