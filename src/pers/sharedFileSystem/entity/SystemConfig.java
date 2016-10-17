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
    /**
     * 结点扩容信息文件保存路径
     */
    public String ExpandFileStorePath;
    /**
     * 结点扩容信息文件名
     */
    public String ExpandFileName;
    /**
     * 存储目录扩容临界值，磁盘的存储空间利用率在大于此值的时候对给磁盘下的存储目录进行逻辑扩容
     */
    public double ExpandCriticalValue;

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
        System.out.println(tabs + "ExpandFileStorePath: " + ExpandFileStorePath);
        System.out.println(tabs + "ExpandFileName: " + ExpandFileName);
        System.out.println(tabs + "ExpandCriticalValue: " + ExpandCriticalValue);
    }
}