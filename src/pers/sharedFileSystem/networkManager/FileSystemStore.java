package pers.sharedFileSystem.networkManager;


import pers.sharedFileSystem.communicationObject.FingerprintInfo;
import pers.sharedFileSystem.communicationObject.RedundancyFileStoreInfo;
import pers.sharedFileSystem.entity.FileReferenceInfo;
import pers.sharedFileSystem.logManager.LogRecord;
import pers.sharedFileSystem.systemFileManager.FileReferenceAdapter;
import pers.sharedFileSystem.systemFileManager.RedundantFileAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件系统存储端，运行在每个存储服务器上面
 */
public class FileSystemStore {

    private static ConcurrentHashMap<String,ArrayList<FingerprintInfo>> redundancyFileMap;
    private static ConcurrentHashMap<String,Integer> fileReferenceInfoMap;

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
     * 添加文件引用
     * @param r
     */
    public static boolean addFileReferenceInfo(FileReferenceInfo r){
        Integer num=fileReferenceInfoMap.get(r.Path);
        if(num==null){
            num=0;
        }
        num++;
        fileReferenceInfoMap.put(r.Path,num);
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
        LogRecord.RunningInfoLogger.info("start load RedundancyFileStoreInfo.");
        List<RedundancyFileStoreInfo>redundancyFileStoreInfos=RedundantFileAdapter.getAllRedundancyFileStoreInfo();
        redundancyFileMap=new ConcurrentHashMap<String,ArrayList<FingerprintInfo>>();
        fileReferenceInfoMap=new ConcurrentHashMap<String,Integer>();
        for(RedundancyFileStoreInfo r:redundancyFileStoreInfos){
            redundancyFileMap.put(r.essentialStorePath,r.otherFileInfo);
        }
        LogRecord.RunningInfoLogger.info("load RedundancyFileStoreInfo successful. total= "+redundancyFileStoreInfos.size());
        LogRecord.RunningInfoLogger.info("start load FileReferenceInfo.");
        List<FileReferenceInfo>fileReferenceInfos= FileReferenceAdapter.getAllFileReferenceInfo();
        for(FileReferenceInfo info:fileReferenceInfos){
            fileReferenceInfoMap.put(info.Path,info.Frequency);
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
