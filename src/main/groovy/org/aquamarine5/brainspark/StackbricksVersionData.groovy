package org.aquamarine5.brainspark

class StackbricksVersionData {
    int versionCode;
    String versionName;
    String downloadApkUrl;
    Date releaseDate;
    String packageName

    StackbricksVersionData(int versionCode,
                           String versionName,
                           String downloadApkUrl,
                           Date releaseDate,
                           String packageName) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.downloadApkUrl = downloadApkUrl;
        this.releaseDate = releaseDate;
        this.packageName = packageName;
    }
}
