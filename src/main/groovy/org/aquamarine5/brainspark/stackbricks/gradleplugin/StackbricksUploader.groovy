package org.aquamarine5.brainspark.stackbricks.gradleplugin

interface StackbricksUploader {
    String getAuthority()

    void upload(File file, String objectKey, boolean overwrite)
}