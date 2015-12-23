package pers.sharedFileSystem.networkManager;


import pers.sharedFileSystem.communicationObject.FingerprintInfo;
import pers.sharedFileSystem.communicationObject.RedundancyFileStoreInfo;
import pers.sharedFileSystem.entity.FileReferenceInfo;
import pers.sharedFileSystem.logManager.LogRecord;
import pers.sharedFileSystem.systemFileManager.FileReferenceAdapter;
import pers.sharedFileSystem.systemFileManager.FingerprintAdapter;
import pers.sharedFileSystem.systemFileManager.RedundantFileAdapter;

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
     * 带文件名的相对路径到文件引用信息的映射
     */
    private static ConcurrentHashMap<String,FileReferenceInfo> fileReferenceInfoMap;
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
     * 添加冗余信息
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
     * 根据“相对路径”获取冗余文件信息
     */
    public static  ArrayList<FingerprintInfo> geRedundancyFileInfoByEssentialStorePath(String essentialStorePath){
        return redundancyFileMap.get(essentialStorePath);
    }

    /**
     * 添加文件引用
     * @param r
     */
    public static boolean addFileReferenceInfo(FileReferenceInfo r){
        FileReferenceInfo fileReferenceInfo=fileReferenceInfoMap.get(r.Path);
        Integer num;
        if(fileReferenceInfo==null){
            fileReferenceInfo=new FileReferenceInfo();
            fileReferenceInfo.Frequency=1;
        }else {
            fileReferenceInfo.Frequency=fileReferenceInfo.Frequency+1;
        }
        fileReferenceInfoMap.put(r.Path,fileReferenceInfo);
        boolean re=FileReferenceAdapter.saveFileReference(fileReferenceInfoMap);
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
        for(FingerprintInfo info:fingerprintInfos){
            fingerprintInfoMap.put(info.getMd5(),info);
        }
        LogRecord.RunningInfoLogger.info("load FingerprintInfo successful. total= "+fingerprintInfos.size());

        LogRecord.RunningInfoLogger.info("start load RedundancyFileStoreInfo.");
        List<RedundancyFileStoreInfo>redundancyFileStoreInfos=RedundantFileAdapter.getAllRedundancyFileStoreInfo();
        redundancyFileMap=new ConcurrentHashMap<String,ArrayList<FingerprintInfo>>();
        for(RedundancyFileStoreInfo r:redundancyFileStoreInfos){
            redundancyFileMap.put(r.essentialStorePath,r.otherFileInfo);
        }
        LogRecord.RunningInfoLogger.info("load RedundancyFileStoreInfo successful. total= "+redundancyFileStoreInfos.size());

        LogRecord.RunningInfoLogger.info("start load FileReferenceInfo.");
        fileReferenceInfoMap=new ConcurrentHashMap<String,FileReferenceInfo>();
        List<FileReferenceInfo>fileReferenceInfos= FileReferenceAdapter.getAllFileReferenceInfo();
        for(FileReferenceInfo info:fileReferenceInfos){
            fileReferenceInfoMap.put(info.Path,info);
        }
        LogRecord.RunningInfoLogger.info("load FileReferenceInfo successful. total= "+fileReferenceInfos.size());
    }

    public FileSystemStore() {
        init();
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        new FileSystemStore();
    }
}
