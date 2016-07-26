package pers.sharedFileSystem.networkManager;


import pers.sharedFileSystem.communicationObject.FingerprintInfo;
import pers.sharedFileSystem.communicationObject.RedundancyFileStoreInfo;
import pers.sharedFileSystem.configManager.Config;
import pers.sharedFileSystem.entity.DirectoryNode;
import pers.sharedFileSystem.entity.Node;
import pers.sharedFileSystem.entity.ServerNode;
import pers.sharedFileSystem.logManager.LogRecord;
import pers.sharedFileSystem.systemFileManager.FingerprintAdapter;
import pers.sharedFileSystem.systemFileManager.RedundantFileAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件系统存储端，运行在每个存储服务器上面
 */
public class FileSystemStore {

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

//        LogRecord.RunningInfoLogger.info("start load FileReferenceInfo.");
//        fileReferenceInfoMap=new ConcurrentHashMap<String,FileReferenceInfo>();
//        List<FileReferenceInfo>fileReferenceInfos= FileReferenceAdapter.getAllFileReferenceInfo();
//        for(FileReferenceInfo info:fileReferenceInfos){
//            fileReferenceInfoMap.put(info.Path,info);
//        }
//        LogRecord.RunningInfoLogger.info("load FileReferenceInfo successful. total= "+fileReferenceInfos.size());
    }

    public FileSystemStore() {
        init();
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        new FileSystemStore();
    }
}
