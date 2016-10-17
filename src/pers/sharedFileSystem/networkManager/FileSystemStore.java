package pers.sharedFileSystem.networkManager;


import pers.sharedFileSystem.communicationObject.FingerprintInfo;
import pers.sharedFileSystem.communicationObject.MessageProtocol;
import pers.sharedFileSystem.communicationObject.MessageType;
import pers.sharedFileSystem.communicationObject.RedundancyFileStoreInfo;
import pers.sharedFileSystem.configManager.Config;
import pers.sharedFileSystem.convenientUtil.CommonUtil;
import pers.sharedFileSystem.entity.*;
import pers.sharedFileSystem.logManager.LogRecord;
import pers.sharedFileSystem.systemFileManager.FingerprintAdapter;
import pers.sharedFileSystem.systemFileManager.RedundantFileAdapter;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件系统存储端，运行在每个存储服务器上面
 */
public class FileSystemStore {

    /**
     * 存储端和冗余验证服务器之间的socket连接
     */
    private static  Socket socket;

    private static SystemConfig systemConfig = Config.SYSTEMCONFIG;
    /**
     * 相对路径到冗余文件列表的映射
     */
    private static ConcurrentHashMap<String,ArrayList<FingerprintInfo>> redundancyFileMap;
    /**
     * 带文件名的相对路径到文件指纹信息的映射(方便删除物理文件的指纹信息)
     * fileReferenceInfoMap 和  fingerprintInfoMap 构成了指向某个文件指纹信息的双索引
     */
    private static ConcurrentHashMap<String,FingerprintInfo> fileReferenceInfoMap;
    /**
     * 文件MD5到文件指纹信息的映射
     */
    private static ConcurrentHashMap<String,FingerprintInfo> fingerprintInfoMap;

    /**
     * 根据文件指纹查找对应的文件指纹信息
     * @return
     */
    public static FingerprintInfo findFingerprintInfoByMD5(String md5){
        return fingerprintInfoMap.get(md5);
    }

    /**
     * 将一个消息对象发送给冗余验证服务器
     *
     */
    public static void sendMessageToRedundancyServer(MessageProtocol mes) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(mes);
        oos.flush();
    }
    /**
     * 重新建立存储端和冗余验证服务器之间的socket连接
     */
    public static void restartConnectToRedundancyServer(){
        try {
            LogRecord.RunningErrorLogger.error("attempt to reconnect to redundancy server.");
            socket = new Socket(systemConfig.ClusterServerIp, systemConfig.ClusterServerPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 添加文件引用信息
     * @param r
     */
    public static boolean addRedundancyFileStoreInfo(RedundancyFileStoreInfo r){
        ArrayList<FingerprintInfo>fingerprintInfos=redundancyFileMap.get(r.essentialStorePath);
        if(fingerprintInfos==null){
            fingerprintInfos=new ArrayList<FingerprintInfo>();
        }
        for(FingerprintInfo f:r.otherFileInfo)
            fingerprintInfos.add(f);
        redundancyFileMap.put(r.essentialStorePath,fingerprintInfos);
        boolean re=RedundantFileAdapter.saveRedundancyFileStoreInfo(redundancyFileMap);
        return  re;
    }
    /**
     * 删除文件引用信息
     * @param r
     */
    public static boolean deleteRedundancyFileStoreInfo(RedundancyFileStoreInfo r){
        ArrayList<FingerprintInfo>fingerprintInfos=redundancyFileMap.get(r.essentialStorePath);
        for(FingerprintInfo f : r.otherFileInfo) {//待删除的文件
            for(FingerprintInfo ff : fingerprintInfos) {
                if(ff.getFileName().equals(f.getFileName())) {
                    fingerprintInfos.remove(ff);
                    break;
                }
            }
        }
        if(fingerprintInfos.size()<1){//如果删除之后存储服务器里面该结点的映射信息为空
            redundancyFileMap.remove(r.essentialStorePath);
        }
//        redundancyFileMap.put(r.essentialStorePath,fingerprintInfos);
        boolean re=RedundantFileAdapter.saveRedundancyFileStoreInfo(redundancyFileMap);
        return  re;
    }
    /**
     * 根据“相对路径”获取文件引用信息
     */
    public static  ArrayList<FingerprintInfo> getRedundancyFileInfoByEssentialStorePath(String essentialStorePath){
        return redundancyFileMap.get(essentialStorePath);
    }
    /**
     * 根据“相对路径”和文件名 验证文件有效性
     */
    public static  ArrayList<FingerprintInfo> validateFileNames(ArrayList<FingerprintInfo> fingerprintInfos){
        ArrayList<FingerprintInfo> res=new ArrayList<FingerprintInfo>();
        for(FingerprintInfo info : fingerprintInfos){
            FingerprintInfo f=fileReferenceInfoMap.get(info.getFilePath()+info.getFileName());
            if(f!=null&&!f.getPhysicalDeletedByTrueUserFlag()){
                res.add(info);
            }
        }
        return res;
    }

    /**
     * 添加文件引用频率
     * @param fingerprintInfo 被添加引用的文件信息
     */
    public static boolean addFileReferenceInfo(FingerprintInfo fingerprintInfo){
        FingerprintInfo fInfo=fingerprintInfoMap.get(fingerprintInfo.getMd5());
        fInfo.setFrequency(fInfo.getFrequency()+1);
        boolean re=FingerprintAdapter.saveAllFingerprint(fingerprintInfoMap);
        return  re;
    }
    /**
     * 删除文件引用频率
     * @param fingerprintInfo 被删除引用的文件信息
     */
    public static boolean deleteFileReferenceInfo(FingerprintInfo fingerprintInfo){
        FingerprintInfo fInfo=fingerprintInfoMap.get(fingerprintInfo.getMd5());
        fInfo.setFrequency(fInfo.getFrequency()-1);
        if(fInfo.getFrequency()<1){//如果删除之后存储服务器里面该结点的引用信息为0，则直接删除这条记录
            if(fInfo.getPhysicalDeletedByTrueUserFlag()){//如果isPhysicalDeletedByTrueUser=true则还需要删除物理文件
                Node n= Config.getNodeByNodeId(fingerprintInfo.getNodeId());
                DirectoryNode node = (DirectoryNode)n;
                //删除物理文件
                String filePath=node.StorePath+fingerprintInfo.getFilePath()+fingerprintInfo.getFileName();
                File file = new File(filePath);
                // 判断目录或文件是否存在
                if (!file.exists()) { // 不存在返回 false
                    LogRecord.FileHandleErrorLogger.error("file not exist: " + "127.0.0.1/"
                            + filePath);
                    return false;
                } else {
                    // 判断是否为文件
                    if (file.isFile()) { // 为文件时调用删除文件方法&&!file.getName().equals("Fingerprint.sys")
                        file.delete();
                        LogRecord.FileHandleInfoLogger.info("delete file successful: " + "127.0.0.1/"
                                +filePath);
                    }
                }
                fingerprintInfoMap.remove(fingerprintInfo.getMd5());
                fileReferenceInfoMap.remove(fingerprintInfo.getFilePath() + fingerprintInfo.getFileName());
            }

        }

        boolean re=FingerprintAdapter.saveAllFingerprint(fingerprintInfoMap);
        return  re;
    }
    /**
     * 添加文件元数据
     * @param fingerprintInfo 被添加引用的文件信息
     */
    public static boolean addFingerprintInfo(FingerprintInfo fingerprintInfo){
        fingerprintInfoMap.put(fingerprintInfo.getMd5(),fingerprintInfo);
        fileReferenceInfoMap.put(fingerprintInfo.getFilePath() + fingerprintInfo.getFileName(),fingerprintInfo);
        boolean re=FingerprintAdapter.saveAllFingerprint(fingerprintInfoMap);
        return  re;
    }
    /**
     * 删除文件指纹信息
     * @param fingerprintInfo 被删除的文件指纹信息（只包含相对路径信息和文件名）
     */
    public static boolean deleteFingerprintInfo(FingerprintInfo fingerprintInfo){
//        fingerprintInfoMap.put(fingerprintInfo.getMd5(),fingerprintInfo);
        boolean re=false;
        FingerprintInfo fInfo=fileReferenceInfoMap.get(fingerprintInfo.getFilePath() + fingerprintInfo.getFileName());
        if(fInfo!=null){
            if(fInfo.getFrequency()>0) {//如果该文件存在引用信息
                fInfo.setPhysicalDeletedByTrueUserFlag(true);
                re = FingerprintAdapter.saveAllFingerprint(fingerprintInfoMap);
            }else{//如果该文件不存在引用信息
                //删除物理文件
                Node n= Config.getNodeByNodeId(fInfo.getNodeId());
                DirectoryNode node = (DirectoryNode)n;
                String filePath=node.StorePath+fingerprintInfo.getFilePath()+fingerprintInfo.getFileName();
                File file = new File(filePath);
                // 判断目录或文件是否存在
                if (!file.exists()) { // 不存在返回 false
                    LogRecord.FileHandleErrorLogger.error("file not exist: " + "127.0.0.1/"
                            + filePath);
                    return false;
                } else {
                    // 判断是否为文件
                    if (file.isFile()) { // 为文件时调用删除文件方法&&!file.getName().equals("Fingerprint.sys")
                        file.delete();
                        LogRecord.FileHandleInfoLogger.info("delete file successful: " + "127.0.0.1/"
                                +filePath);
                    }
                }
                fingerprintInfoMap.remove(fInfo.getMd5());
                fileReferenceInfoMap.remove(fInfo.getFilePath() + fInfo.getFileName());
                re = FingerprintAdapter.saveAllFingerprint(fingerprintInfoMap);
            }
        }
        return  re;
    }

    /**
     * 把config信息发给集群管理子系统
     */
    private void sendConfigInfo(){
        MessageProtocol mes=new MessageProtocol();
        mes.messageType= MessageType.SEND_CONFIG;
        mes.senderType = SenderType.STORE;
        mes.content = Config.getConfig().elements().nextElement();// 获取配置文件中的第一个server结点
        try {
            LogRecord.RunningInfoLogger.info("send SEND_CONFIG comand to "+systemConfig.ClusterServerIp+":"+systemConfig.ClusterServerPort);
            sendMessageToRedundancyServer(mes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 给冗余验证服务器返回指纹信息列表
     */
    private void sendFingerprintListToRedundancy(ArrayList<String> info){
        MessageProtocol mes=new MessageProtocol();
        mes.messageType=MessageType.SEND_FINGERPRINT_LIST;
        mes.senderType = SenderType.STORE;
        mes.content=info;
        try {
            LogRecord.RunningInfoLogger.info("send SEND_FINGERPRINT_LIST, num="+info.size());
            sendMessageToRedundancyServer(mes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 把指纹信息发给集群管理子系统
     */
    private void sendFigureprintInfo(){
        String filePath= systemConfig.FingerprintStorePath;//指纹信息的保存路径
        String fileName=  systemConfig.FingerprintName;
        FileInputStream fin = null;
        BufferedInputStream bis =null;
        ObjectInputStream oip=null;
        ArrayList<String> fingers=new ArrayList<String>();
        int maxNum=100;//每次发送多少条
        if(!CommonUtil.validateString(filePath)){
            LogRecord.FileHandleErrorLogger.error("get Fingerprint error, filePath is null.");
            return;
        }
        File file = new File(filePath);
        if (!file.isDirectory()||!new File(filePath+"/"+fileName).exists()) {
            LogRecord.FileHandleErrorLogger.error("get Fingerprint error, can not find Fingerprint file.");
            return;
        }
        boolean run = true;
        try{
            LogRecord.RunningInfoLogger.info("start to load Fingerprint.");
            fin = new FileInputStream(filePath+"/"+fileName);
            bis = new BufferedInputStream(fin);
            while (run) {
                try {
                    oip = new ObjectInputStream(bis); // 每次重新构造对象输入流
                }catch (EOFException e) {
                    // e.printStackTrace();
//                    System.out.println("已达文件末尾");// 如果到达文件末尾，则退出循环

                    //目的是一定要把指纹信息发送完毕
                    if(fingers.size()>0) {
                        sendFingerprintListToRedundancy(fingers);
                        fingers.clear();
                    }
                    break;
                }
                Object object =oip.readObject();
                if (object instanceof FingerprintInfo) { // 判断对象类型
                    FingerprintInfo tmp=(FingerprintInfo)object;
                    fingers.add(tmp.getMd5());
                    if(fingers.size()>=maxNum){
                        sendFingerprintListToRedundancy(fingers);
                        fingers.clear();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            //此处发送的一定是空集合，目的是告诉冗余验证服务器，指纹已经发送完毕
            sendFingerprintListToRedundancy(fingers);
            try {
                if(oip!=null)
                    oip.close();
                if(bis!=null)
                    bis.close();
                if(fin!=null)
                    fin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 初始化
     */
    private void init(){
        ConnWatchDog connWatchDog = new ConnWatchDog();
        Thread connWatchDogThread = new Thread(connWatchDog);
        connWatchDogThread.start();

        LogRecord.RunningInfoLogger.info("start load FingerprintInfo.");
        List<FingerprintInfo>fingerprintInfos= FingerprintAdapter.getAllFingerprintInfo();
        fingerprintInfoMap=new ConcurrentHashMap<String,FingerprintInfo>();
        fileReferenceInfoMap=new ConcurrentHashMap<String,FingerprintInfo>();
        for(FingerprintInfo info:fingerprintInfos){
            fingerprintInfoMap.put(info.getMd5(),info);
            fileReferenceInfoMap.put(info.getFilePath()+info.getFileName(),info);
        }
        LogRecord.RunningInfoLogger.info("load FingerprintInfo successful. total= "+fingerprintInfos.size());

        LogRecord.RunningInfoLogger.info("start load RedundancyFileStoreInfo.");
        List<RedundancyFileStoreInfo>redundancyFileStoreInfos=RedundantFileAdapter.getAllRedundancyFileStoreInfo();
        redundancyFileMap=new ConcurrentHashMap<String,ArrayList<FingerprintInfo>>();
        for(RedundancyFileStoreInfo r:redundancyFileStoreInfos){
            redundancyFileMap.put(r.essentialStorePath,r.otherFileInfo);
        }
        LogRecord.RunningInfoLogger.info("load RedundancyFileStoreInfo successful. total= "+redundancyFileStoreInfos.size());

        // 与集群管理服务器建立连接
        try {
            socket = new Socket(systemConfig.ClusterServerIp, systemConfig.ClusterServerPort);
            // 与集群管理服务器建立连接之后，开启线程监听集群管理服务器发来的消息
            SocketAction socketAction = new SocketAction(socket);
            Thread thread = new Thread(socketAction);
            thread.start();
            // 把config和指纹信息发过去
            sendConfigInfo();
            // 把指纹信息发送过去
            sendFigureprintInfo();

            KeepAliveWatchRedundancy k1 = new KeepAliveWatchRedundancy();
            Thread t1 = new Thread(k1);
            t1.start();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            LogRecord.RunningErrorLogger.error(e.toString());
        }
    }

    public FileSystemStore() {
        init();
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        new FileSystemStore();
    }
}
