package org.aquamarine5.brainspark.stackbricks.gradleplugin

class StackbricksConfigurationExtension {
    @Deprecated
    String host;
    @Deprecated
    String configJsonFilePath = "stackbricks_config_v1.json";

    @Deprecated
    List<PossibleConfigurationPath> possibleConfigurationPaths = new ArrayList<>()

    List<StackbricksUploadConfig> uploadDestinationConfiguration = new ArrayList<>()

    List<StackbricksManifestConfig> manifestDestinationConfigurations = new ArrayList<>()

    @Deprecated
    QiniuConfigurationExtension qiniuConfiguration = new QiniuConfigurationExtension()

    @Deprecated
    void qiniuConfig(Closure closure) {
        StackbricksDsl.configure(qiniuConfiguration, closure)
        legacyQiniuConfigured = true
    }

    boolean legacyQiniuConfigured = false

    String changelog = ""
    @Deprecated
    Boolean forceInstall = false

    Boolean requiredManifestVersion2MigrateForceInstall = true
    long forceInstallLessVersion = -1

    void uploadDestinationConfiguration(Closure closure) {
        def builder = new StackbricksUploadDestinationConfiguration()
        StackbricksDsl.configure(builder, closure)
        uploadDestinationConfiguration.addAll(builder.configs)
    }

    void manifestDestinationConfigurations(Closure closure) {
        def builder = new StackbricksManifestDestinationConfiguration()
        StackbricksDsl.configure(builder, closure)
        manifestDestinationConfigurations.addAll(builder.configs)
    }

    void qiniuUploadConfig(Closure closure) {
        def config = new QiniuUploadConfig()
        StackbricksDsl.configure(config, closure)
        uploadDestinationConfiguration.add(config)
    }

    void cloudflareUploadConfig(Closure closure) {
        def config = new CloudflareUploadConfig()
        StackbricksDsl.configure(config, closure)
        uploadDestinationConfiguration.add(config)
    }

    void qiniuManifestConfig(Closure closure) {
        def config = new QiniuManifestConfig()
        StackbricksDsl.configure(config, closure)
        manifestDestinationConfigurations.add(config)
    }

    void cloudflareManifestConfig(Closure closure) {
        def config = new CloudflareManifestConfig()
        StackbricksDsl.configure(config, closure)
        manifestDestinationConfigurations.add(config)
    }

    List<StackbricksUploadConfig> resolveUploadDestinations() {
        if (!uploadDestinationConfiguration.isEmpty()) {
            return uploadDestinationConfiguration
        }
        if (legacyQiniuConfigured || qiniuConfiguration.accessKey != null) {
            def legacyFirst = legacyFirstPossibleConfigurationPath()
            def config = new QiniuUploadConfig()
            config.host = host ?: legacyFirst?.host
            config.isHttps = legacyFirst?.isHttps != null ? legacyFirst.isHttps : true
            config.accessKey = qiniuConfiguration.accessKey
            config.secretKey = qiniuConfiguration.secretKey
            config.bucket = qiniuConfiguration.bucket
            config.referer = qiniuConfiguration.referer
            return [config] as List<StackbricksUploadConfig>
        }
        return new ArrayList<StackbricksUploadConfig>()
    }

    List<StackbricksManifestConfig> resolveManifestDestinations() {
        if (!manifestDestinationConfigurations.isEmpty()) {
            return manifestDestinationConfigurations
        }
        if (possibleConfigurationPaths == null || possibleConfigurationPaths.isEmpty()) {
            return new ArrayList<StackbricksManifestConfig>()
        }
        def referer = qiniuConfiguration.referer
        return possibleConfigurationPaths.collect { PossibleConfigurationPath path ->
            def config = new QiniuManifestConfig()
            config.host = path.host
            config.isHttps = path.isHttps
            config.configFilePath = path.configFilePath
            config.referer = referer
            return config
        } as List<StackbricksManifestConfig>
    }

    private PossibleConfigurationPath legacyFirstPossibleConfigurationPath() {
        return possibleConfigurationPaths == null || possibleConfigurationPaths.isEmpty()
                ? null : possibleConfigurationPaths[0]
    }
}