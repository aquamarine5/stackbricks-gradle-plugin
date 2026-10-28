package org.aquamarine5.brainspark.stackbricks.gradleplugin

class StackbricksManifestDestinationConfiguration {
    final List<StackbricksManifestConfig> configs = new ArrayList<>()

    void qiniuManifestConfig(Closure closure) {
        def config = new QiniuManifestConfig()
        StackbricksDsl.configure(config, closure)
        configs.add(config)
    }

    void cloudflareManifestConfig(Closure closure) {
        def config = new CloudflareManifestConfig()
        StackbricksDsl.configure(config, closure)
        configs.add(config)
    }
}