package org.aquamarine5.brainspark.stackbricks.gradleplugin

class PossibleConfigurationPath {
    String host;
    String configFilePath;
    Boolean isHttps = false;

    PossibleConfigurationPath(String host,String configFilePath,Boolean isHttps=false){
        this.host=host
        this.configFilePath=configFilePath
        this.isHttps=isHttps
    }
}
