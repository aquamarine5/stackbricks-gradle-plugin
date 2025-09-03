# stackbricks-gradle-plugin

[![wakatime](https://wakatime.com/badge/github/aquamarine5/stackbricks-gradle-plugin.svg)](https://wakatime.com/badge/github/aquamarine5/stackbricks-gradle-plugin)

## 添加依赖

### 方法1（推荐）

> [!IMPORTANT]
> 强烈建议遵循[Github官方文档](https://docs.github.com/zh/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package)进行部署。

- 在项目根目录的`setting.gradle`文件中添加如下内容：
```groovy
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/aquamarine5/stackbricks-gradle-plugin")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("GPR_USERNAME")
                password = project.findProperty("gpr.key") ?: System.getenv("GPR_TOKEN")
            }
        }
    }
}
```

> [!WARNING]
> 请确保`gpr.user`和`gpr.key`的值已经在`gradle.properties`配置，或通过环境变量配置。

- 随后在`build.gradle`文件中添加如下项：
```groovy
plugins {
    id "org.aquamarine5.brainspark.stackbricks-gradle-plugin" version "$latest_sgp_version"
}
```

> [!NOTE]
> 目前最新版本为`2.1.1`，请将`$latest_sgp_version`替换为最新版本号，详见[此](https://github.com/aquamarine5/stackbricks-gradle-plugin/packages/2402628)。

### <del>方法2</del>

> [!CAUTION]
> 暂时不打算对Gradle Plugin Portal源进行支持，可用的版本已明显落后最新版本，请不再使用此方法添加。

- 直接使用[Gradle Plugin Portal源](https://plugins.gradle.org/plugin/io.github.aquamarine5.stackbricks-gradle-plugin)
- 在`setting.gradle`内，Gradle Plugin Portal源已默认启用（`gradlePluginPortal()`），无需再自行配置。
- 在`build.gradle`文件添加以下项：
```groovy
plugins {
    id "io.github.aquamarine5.stackbricks-gradle-plugin" version "$latest_sgp_version"
}
```


> [!NOTE]
> 目前最新版本为`1.2`，此源的更新速度会略慢于Gradle源，使用时请将`$latest_sgp_version`替换为最新版本号，详见[此](https://plugins.gradle.org/plugin/io.github.aquamarine5.stackbricks-gradle-plugin)。

> [!TIP]
> 两种源的`id`不相符，但包内容一致，原因详见[Gradle Plugin Portal对于包命名的要求](https://plugins.gradle.org/docs/publish-plugin#approval)。

## 使用

- 在`build.gradle`文件加入以下配置信息：
```groovy
stackbricksConfig {
    host = "$cdn_host"
    configJsonFilePath = "stackbricks_manifest_v3.json"
    changelog = "lovely lonely"
    qiniuConfig {
        accessKey = project.findProperty("qiniu.accessKey")
        secretKey = project.findProperty("qiniu.secretKey")
        bucket = "$bucket"
        referer = project.findProperty("qiniu.referer") ?: ""
    }
}
```

> [!NOTE]
> 如果七牛云的存储空间未设置Referer防盗链设计，referer传入`null`或空字符串即可。  
> `host`值不需要`http(s)://`前缀。  
> 关于七牛云的对象存储服务，详见[此](https://portal.qiniu.com/kodo)。

## 发布

```bash
gradle uploadApkByQiniu
```

- 运行`uploadApkByQiniu`命令后会自动执行`assembleRelease`命令，生成APK文件，并上传到七牛云，更新Stackbricks配置文件。
> [!IMPORTANT]
> 确保`build.gradle`正确配置了针对`buildTypes.release.signingConfig`的签名信息，避免生成`app-release-unsigned.apk`导致上传失败。

## Stackbricks Config
### v3 (`stackbricks_manifest_v3.json`)
```json
{
    "manifestVersion": 3,
    "latestStable": {
        "changelog": "<string>",
        "downloadUrl": "<string, only file_name>",
        "forceInstall": "<boolean, BUT DEPRECATED, USE forceInstallLessVersion FIELD>",
        "packageName": "<string>",
        "releaseDate": "<long as Instant>",
        "versionCode": "<int>",
        "versionName": "<string>",
        "forceInstallLessVersion": "<int>"
    },
    "latestTest": {
        "changelog": "<string>",
        "downloadUrl": "<string, only file_name>",
        "forceInstall": "<boolean, BUT DEPRECATED, USE forceInstallLessVersion FIELD>",
        "packageName": "<string>",
        "releaseDate": "<long as Instant>",
        "versionCode": "<int>",
        "versionName": "<string>",
        "forceInstallLessVersion": "<int>"
    }
}
```

### v2 (`stackbricks_manifest_v2.json`)
```json
{
    "manifestVersion": 2,
    "latestStable": {
        "changelog": "<string>",
        "downloadUrl": "<string, only file_name>",
        "forceInstall": "<boolean>",
        "packageName": "<string>",
        "releaseDate": "<long as Instant>",
        "versionCode": "<int>",
        "versionName": "<string>"
    },
    "latestTest": {
        "changelog": "<string>",
        "downloadUrl": "<string, only file_name>",
        "forceInstall": "<boolean>",
        "packageName": "<string>",
        "releaseDate": "<long as Instant>",
        "versionCode": "<int>",
        "versionName": "<string>"
    }
}
```

### v1 (`stackbricks_config_v1.json`)

```json
{
  "@version": 1,
  "@type": "stackbricks-config",
  "latest": {
    "versionCode": "<int>",
    "versionName": "<string>",
    "downloadUrl": "<string>",
    "releaseDate": "<long as Instant>",
    "packageName": "<string>"
  },
  "versions": [
    "<StackbricksVersionData>"
  ]
}
```
