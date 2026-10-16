package org.aquamarine5.brainspark.stackbricks.gradleplugin

class StackbricksUploadDestinationConfiguration {
    final List<StackbricksUploadConfig> configs = new ArrayList<>()

    void qiniuUploadConfig(Closure closure) {
        def config = new QiniuUploadConfig()
        StackbricksDsl.configure(config, closure)
        configs.add(config)
    }

    void cloudflareUploadConfig(Closure closure) {
        def config = new CloudflareUploadConfig()
        StackbricksDsl.configure(config, closure)
        configs.add(config)
    }
}