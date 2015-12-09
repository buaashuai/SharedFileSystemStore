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
     * 冗余验证服务器的文件系统监听端口
     */
    public Integer RedundancyPort;
    /**
     * 冗余验证服务器ip-
     */
    public String RedundancyIp;
    /**
     * 指纹信息存储路径
     */
    public String FingerprintStorePath;
    /**
     * 冗余信息文件存储路径
     */
    public String RedundancyFileStorePath;

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
        System.out.println(tabs + "RedundancyIp: " + RedundancyIp);
        System.out.println(tabs + "RedundancyPort: " + RedundancyPort);
        System.out.println(tabs + "FingerprintStorePath: " + FingerprintStorePath);
        System.out.println(tabs + "RedundancyFileStorePath: " + RedundancyFileStorePath);
    }
}