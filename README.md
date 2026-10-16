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
> 目前最新版本为`2.2.9`，请将`$latest_sgp_version`替换为最新版本号，详见[此](https://github.com/aquamarine5/stackbricks-gradle-plugin/packages/2402628)。

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
> 目前最新版本为`1.2`，**此源目前已不再更新**。使用时请将`$latest_sgp_version`替换为最新版本号，详见[此](https://plugins.gradle.org/plugin/io.github.aquamarine5.stackbricks-gradle-plugin)。

> [!TIP]
> 两种源的`id`不相符，但包内容一致，原因详见[Gradle Plugin Portal对于包命名的要求](https://plugins.gradle.org/docs/publish-plugin#approval)。

## 使用

- 目前`stackbricks-gradle-plugin`以及配套[Stackbricks](https://github.com/aquamarine5/Stackbricks)支持七牛云CDN和Cloudflare R2。
- 在`build.gradle`文件加入以下配置信息：
```groovy
stackbricksConfig {
    possibleConfigurationPaths = [
            new PossibleConfigurationPath("cdn2.example.com", "stackbricks_manifest_v4.json", true),
            new PossibleConfigurationPath("cdn.example.com", "stackbricks_manifest_v4.json", true),
            // ... and more, tried in order
    ]
    changelog = "lovely lonely"
    forceInstallLessVersion = 2006_12_15
    requiredManifestVersion2MigrateForceInstall = true
    uploadDestinationConfiguration {
        cloudflareUploadConfig {
            host = "cdn2.example.com"
            accountId = project.findProperty("cloudflare.accountId")
            bucketName = project.findProperty("cloudflare.bucketName")
            accessKeyId = project.findProperty("cloudflare.accessKeyId")
            secretAccessKey = project.findProperty("cloudflare.secretAccessKey")
        }
        qiniuUploadConfig {
            host = "cdn.example.com"
            accessKey = project.findProperty("qiniu.accessKey")
            secretKey = project.findProperty("qiniu.secretKey")
            bucket = project.findProperty("qiniu.bucket")
            referer = project.findProperty("qiniu.referer") ?: ""
        }
    }
}
```

- 每个`uploadDestinationConfiguration`块声明一个上传目标，块之间可以任意组合，**声明顺序即上传顺序**。
  APK和清单文件会上传到每一个目标，清单中的`possibleDownloadPath`按同样的顺序生成，
  Android端会依次尝试，第一个匹配到可用`StackbricksPackageProvider`的路径胜出，因此把更可靠的源写在前面。
- `host`值不需要`http(s)://`前缀，它是该目标对外提供下载的域名（R2需要绑定自定义域名或r2.dev）。
  Cloudflare R2的bucket必须是**公开可读**的，插件只写入、读取都走公网地址，凭证不会进入APK。
- `requiredManifestVersion2MigrateForceInstall: Boolean`  
  用于标识在更新V2版本清单文件时已被弃用的`forceInstall`的值。由于`forceInstall`已经在新版本V3清单文件中被弃用，
并使用`forceInstallLessVersion`替代。若此值为`true`，则表明在更新清单文件的同时，将`forceInstall`值设置为`true`，
**接受此清单文件的旧版本应用会被强制更新，即使你没有显式的设置强制更新**，反之则复制原清单文件的值。

> [!NOTE]
> 如果CDN未设置Referer防盗链设计，referer传入`null`或空字符串即可。  
> 关于七牛云的对象存储服务，详见[此](https://portal.qiniu.com/kodo)；R2详见[此](https://developers.cloudflare.com/r2/)。

### 迁移

`qiniuConfig { }` 和 `qiniuConfiguration` 已弃用，改用 `uploadDestinationConfiguration { qiniuUploadConfig { } }`。
未配置新字段时插件仍会把旧的 `qiniuConfiguration` 迁移成一个七牛目标（host取自 `possibleConfigurationPaths` 的第一项），
所以旧脚本无需立刻修改，但建议尽快迁移。

## 发布

```bash
gradle uploadApk
```

- 运行`uploadApk`命令后会自动执行`assembleRelease`命令，生成APK文件，上传到所有已配置的目标，更新Stackbricks清单文件。
- `uploadApkByQiniu`已弃用，仅为兼容保留，等价于只配置一个七牛目标时的`uploadApk`。
> [!IMPORTANT]
> 确保`build.gradle`正确配置了针对`buildTypes.release.signingConfig`的签名信息，避免生成`app-release-unsigned.apk`导致上传失败。

## 配置文件

> [!NOTE]
> 对于 Stackbricks Manifest 并没有指定的命名要求，但推荐使用括号后的文件名以规范命名。自`v3`软件趋于稳定后也可以使用`stackbricks_manifest.json`名称命名。

### v4 (`stackbricks_manifest_v4.json`)

```json5
{
    "manifestVersion": 4,
    "latestStable": {
        "changelog": "<string>",
        "possibleDownloadPath": [
            {
                "filename": "<string, the object key / file name>",
                "authority": "org.aquamarine5.brainspark.stackbricks.downloadpath.cloudflare",
                "additionalInformation": {
                    "baseUrl": "<string, public base url without trailing slash>",
                    "accountId": "<string, diagnostics only>",
                    "bucketName": "<string, diagnostics only>"
                }
            },
            {
                "filename": "<string, the object key / file name>",
                "authority": "org.aquamarine5.brainspark.stackbricks.downloadpath.qiniu",
                "additionalInformation": {
                    "host": "<string, qiniu cdn host, overrides the host in QiniuConfiguration>"
                }
            }
        ],
        "forceInstall": "<boolean, DEPRECATED ONLY FOR COMPATIBILITY, USE forceInstallLessVersion FIELD>",
        "packageName": "<string>",
        "releaseDate": "<long as Instant>",
        "versionCode": "<int>",
        "versionName": "<string>",
        "forceInstallLessVersion": "<long>"
    },
    "latestTest": { "... same shape as latestStable ..." }
}
```

`possibleDownloadPath`按顺序尝试，Android端选择第一个自身已注册了对应`StackbricksPackageProvider`的条目。

#### 从v3更新
- 更新`stackbricks-gradle-plugin`至2.3.0及以上，[Stackbricks](https://github.com/aquamarine5/Stackbricks)更新至2.4.22及以上。
- 单数`downloadUrl`被复数`possibleDownloadPath`替代。单源用户改用`uploadDestinationConfiguration { qiniuUploadConfig { } }`即可，
  `possibleDownloadPath`里只会有一项，`additionalInformation.host`会填入`host`。
- Stackbricks仍然接受`manifestVersion`为2和3的清单，其中`downloadUrl`作为legacy回退。
- 配置多个目标时，`possibleConfigurationPaths`里相同`configFilePath`只生成一份清单文件，
  但该文件会被上传到**每一个**目标，因此每个目标都能独立提供清单。

### v3 (`stackbricks_manifest.json`或`stackbricks_manifest_v3.json`)
```json5
{
    "manifestVersion": 3,
    "latestStable": {
        "changelog": "<string>",
        "downloadUrl": "<string, only file_name>",
        "forceInstall": "<boolean, DEPRECATED ONLY FOR COMPATIBILITY, USE forceInstallLessVersion FIELD>",
        "packageName": "<string>",
        "releaseDate": "<long as Instant>",
        "versionCode": "<int>",
        "versionName": "<string>",
        "forceInstallLessVersion": "<long>" //After 2.4.12, it changed from int to long as bugfix
    },
    "latestTest": {
        "changelog": "<string>",
        "downloadUrl": "<string, only file_name>",
        "forceInstall": "<boolean, DEPRECATED ONLY FOR COMPATIBILITY, USE forceInstallLessVersion FIELD>",
        "packageName": "<string>",
        "releaseDate": "<long as Instant>",
        "versionCode": "<int>",
        "versionName": "<string>",
        "forceInstallLessVersion": "<long>" //After 2.4.12, it changed from int to long as bugfix
    }
}
```

#### 从v2更新
- 更新stackbricks-gradle-plugin插件版本至[2.1.0](https://github.com/aquamarine5/stackbricks-gradle-plugin/releases/tag/2.1.0)及以上，[Stackbricks](https://github.com/aquamarine5/Stackbricks)更新至[2.3.1](https://github.com/aquamarine5/Stackbricks/releases/tag/2.3.1)及以上。
- `forceInstall`值被弃用，被`forceInstallLessVersion: Long`替代，在适用于v3清单文件的程序解析过程中，`forceInstall`值会被忽略。
- `forceInstallLessVersion`用于指示当程序版本低于此值时才会被标记为需要强制更新，当无版本需要强制更新时，设置为`-1`。
- 【2.1.1版本后加入】在gradle文件配置时加入了`requiredManifestVersion2MigrateForceInstall`项，用于标识在更新V2版本清单文件时已被弃用的`forceInstall`的值，值若为`true`，则表明所有应用版本还在使用v2清单文件的程序会被强制更新，即使你没有显式的设置强制更新。

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

#### 从v1更新
- 更新stac

### v1 (`stackbricks_config_v1.json`)

```json5
{
  "@version": 1,
  "@type": "stackbricks-config",
  "latest": {
    "versionCode": "<int>",
    "versionName": "<string>",
    "downloadUrl": "<string>",
    "releaseDate": "<long as Instant>",
    "packageName": "<string>"
  }, // This JSONObject as a StackbricksVersionData
  "versions": [
    "<StackbricksVersionData>"
  ]
}
```
