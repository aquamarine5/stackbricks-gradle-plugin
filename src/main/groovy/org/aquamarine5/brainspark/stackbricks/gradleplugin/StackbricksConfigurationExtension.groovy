package org.aquamarine5.brainspark.stackbricks.gradleplugin

import org.gradle.api.Action

class StackbricksConfigurationExtension {
    String host;
    String configJsonFilePath = "stackbricks_config_v1.json";
    QiniuConfigurationExtension qiniuConfiguration = new QiniuConfigurationExtension()
    String changelog = ""
    @Deprecated
    Boolean forceInstall = false
    int forceInstallLessVersion=-1

    void qiniuConfig(Action<QiniuConfigurationExtension> action) {
        action.execute(qiniuConfiguration)
    }
}
