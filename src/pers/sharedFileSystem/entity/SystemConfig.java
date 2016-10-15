package pers.sharedFileSystem.entity;

/**
 * 文件系统运行配置类
 */
public class SystemConfig {
    /**
     * 本地存储服务器的文件系统监听端口
     */
    public Integer Port;
    /**
     * 集群管理子系统的IP地址
     */
    public String ClusterServerIp;
    /**
     * 集群管理子系统的端口
     */
    public Integer ClusterServerPort;
    /**
     * 指纹信息存储路径
     */
    public String FingerprintStorePath;
    /**
     * 指纹信息文件名
     */
    public String FingerprintName;
    /**
     * 冗余信息文件存储路径
     */
    public String RedundancyFileStorePath;
    /**
     * 冗余信息文件名
     */
    public String RedundancyFileName;

    public SystemConfig(){

    }
    /**
     * 打印系统配置信息
     *
     * @param tabs
     *            缩进tab
     */
    public void print(String tabs) {
        System.out.println(tabs + "Port: " + Port);
        System.out.println(tabs + "FingerprintStorePath: " + FingerprintStorePath);
        System.out.println(tabs + "FingerprintName: " + FingerprintName);
        System.out.println(tabs + "RedundancyFileStorePath: " + RedundancyFileStorePath);
        System.out.println(tabs + "RedundancyFileName: " + RedundancyFileName);
        System.out.println(tabs + "ClusterServerIp: " + ClusterServerIp);
        System.out.println(tabs + "ClusterServerPort: " + ClusterServerPort);
    }
}