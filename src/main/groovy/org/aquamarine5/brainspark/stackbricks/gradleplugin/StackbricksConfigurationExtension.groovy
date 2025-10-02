package org.aquamarine5.brainspark.stackbricks.gradleplugin

import org.gradle.api.Action

class StackbricksConfigurationExtension {
    @Deprecated
    String host;
    @Deprecated
    String configJsonFilePath = "stackbricks_config_v1.json";

    List<PossibleConfigurationPath> possibleConfigurationPaths = new ArrayList<>()

    QiniuConfigurationExtension qiniuConfiguration = new QiniuConfigurationExtension()

    String changelog = ""
    @Deprecated
    Boolean forceInstall = false

    Boolean requiredManifestVersion2MigrateForceInstall = true
    int forceInstallLessVersion = -1

    void qiniuConfig(Action<QiniuConfigurationExtension> action) {
        action.execute(qiniuConfiguration)
    }
}
