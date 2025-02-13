package org.aquamarine5.brainspark

import com.qiniu.storage.Configuration
import com.qiniu.storage.Region
import com.qiniu.storage.UploadManager
import com.qiniu.util.Auth
import org.gradle.api.Plugin
import org.gradle.api.Project

class StackbricksGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create("stackbricksConfig",StackbricksConfigurationExtension)
        project.tasks.register("uploadApkByQiniu"){
            it.dependsOn "assembleRelease"
            it.doLast {
                def config=project.extensions.findByType(StackbricksConfigurationExtension)
                if(config!=null){
                    def apkFile=project.layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
                    uploadFileByQiniu(apkFile,config.qiniuConfiguration)
                }else{
                    throw new NullPointerException("QiniuConfig is required when uploadApkByQiniu is executed.")
                }
            }
        }
    }

    static void uploadFileByQiniu(File file, QiniuConfigurationExtension qiniuConfig){
        def auth=Auth.create(qiniuConfig.accessKey,qiniuConfig.secretKey)
        def uploadToken=auth.uploadToken(qiniuConfig.bucket)
        def uploadConfig= new Configuration(Region.qvmHuabei())
        uploadConfig.resumableUploadAPIVersion=Configuration.ResumableUploadAPIVersion.V2
        def uploadManager=new UploadManager(uploadConfig)
        def response=uploadManager.put(file.path,file.name,uploadToken)
        println response.bodyString()
    }

    static void updateStackbricksConfig(Project project,StackbricksConfigurationExtension stackbricksConfig) {
        def versionCode = project.android.defaultConfig.versionCode
        def versionName = project.android.defaultConfig.versionName

    }
}