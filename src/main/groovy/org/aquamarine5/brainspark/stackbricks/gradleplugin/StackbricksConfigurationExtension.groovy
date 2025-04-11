package org.aquamarine5.brainspark.stackbricks.gradleplugin

import org.gradle.api.Action

class StackbricksConfigurationExtension {
    String host;
    String configJsonFilePath = "stackbricks_config_v1.json";
    QiniuConfigurationExtension qiniuConfiguration = new QiniuConfigurationExtension()
    String changelog = ""
    Boolean forceInstall = false

    void qiniuConfig(Action<QiniuConfigurationExtension> action) {
        action.execute(qiniuConfiguration)
    }
}
