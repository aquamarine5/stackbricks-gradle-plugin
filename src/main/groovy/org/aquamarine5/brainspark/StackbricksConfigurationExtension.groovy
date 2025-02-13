package org.aquamarine5.brainspark

import org.gradle.api.Action

class StackbricksConfigurationExtension {
    String configJsonFilePath="stackbricks_config_v1.json";
    QiniuConfigurationExtension qiniuConfiguration=new QiniuConfigurationExtension()
    void qiniuConfig(Action<QiniuConfigurationExtension> action){
        action.execute(qiniuConfiguration)
    }
}
