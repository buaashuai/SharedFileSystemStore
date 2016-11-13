package pers.sharedFileSystem.systemFileManager;

import pers.sharedFileSystem.communicationObject.ExpandFileStoreInfo;
import pers.sharedFileSystem.configManager.Config;
import pers.sharedFileSystem.configManager.Constant;
import pers.sharedFileSystem.convenientUtil.CommonUtil;
import pers.sharedFileSystem.entity.SystemConfig;
import pers.sharedFileSystem.logManager.LogRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 结点扩容信息文件存储信息操作类
 */
public class ExpandFileAdapter {
    private static SystemConfig sysConfig=Config.SYSTEMCONFIG;
    /**
     * 按照序列化的方式将结点扩容信息文件存储信息保存到磁盘(保存全部信息)
     */
    public static boolean saveAllExpandInfo(ConcurrentHashMap<String,ArrayList<String>> expandFileMap){
        FileOutputStream fout=null;
        ObjectOutputStream sout =null;
        String filePath=sysConfig.ExpandFileStorePath;//结点扩容信息文件的保存路径
        String fileName=sysConfig.ExpandFileName;
        if(!CommonUtil.validateString(filePath)){
            LogRecord.FileHandleErrorLogger.error("save ExpandInfo error, filePath is null.");
            return false;
        }
        File file = new File(filePath);
        if (!file.exists() && !file.isDirectory()) {
            LogRecord.RunningErrorLogger.error("save ExpandInfo error, filePath illegal.");
            return false;
        }
        File oldFile=new File(filePath+"/"+fileName);
        File tempFile=new File(filePath+"/"+ Constant.ExpandTempFileName);
        if(oldFile.exists()){
            oldFile.renameTo(tempFile);
        }

        try{
            fout = new FileOutputStream(filePath + "/" + fileName, true);
            int num=0;
            for(String key:expandFileMap.keySet()) {
                sout = new ObjectOutputStream(fout);
                ExpandFileStoreInfo expandFileStoreInfo=new ExpandFileStoreInfo();
                expandFileStoreInfo.directoryNodeId=key;
                expandFileStoreInfo.expandNodeList=expandFileMap.get(key);
                sout.writeObject(expandFileStoreInfo);
                num++;
            }
            LogRecord.RunningInfoLogger.info("save ExpandFileStoreInfo successful. total="+num);
        }catch (FileNotFoundException e){
            e.printStackTrace();
            return false;
        }catch (IOException e){
            e.printStackTrace();
            File newFile=new File(filePath+"/"+fileName);
            newFile.delete();
            tempFile.renameTo(newFile);
            return false;
        }finally {
            try {
                //删除临时文件
                if(tempFile.exists()){
                    tempFile.delete();
                }
                if(fout!=null)
                    fout.close();
                if(sout!=null)
                    sout.close();
            } catch (IOException e) {
                e.printStackTrace();
                return  false;
            }
        }
        return true;
    }

    /**
     * 按照序列化的方式获取全部结点扩容信息文件存储信息
     * @return
     */
    public static List<ExpandFileStoreInfo> getAllRedundancyFileStoreInfo(){
        List<ExpandFileStoreInfo>redundancyFileStoreInfos=new ArrayList<ExpandFileStoreInfo>();
        FileInputStream fin = null;
        BufferedInputStream bis =null;
        ObjectInputStream oip=null;
        String filePath=sysConfig.ExpandFileStorePath;//结点扩容信息文件信息的保存路径
        String fileName=sysConfig.ExpandFileName;
        if(!CommonUtil.validateString(filePath)){
            LogRecord.FileHandleErrorLogger.error("get ExpandFile error, filePath is null.");
            return redundancyFileStoreInfos;
        }
        File file = new File(filePath);
        String fullFilePath = filePath+"/"+fileName;
        if (!file.isDirectory()||!new File(fullFilePath).exists()) {
            LogRecord.FileHandleErrorLogger.error("ExpandFile not found: "+fullFilePath);
            return redundancyFileStoreInfos;//如果系统文件夹不存在或者冗余信息文件不存在
        }
        try{
            fin = new FileInputStream(fullFilePath);
            bis = new BufferedInputStream(fin);
            while (true) {
                try {
                    oip = new ObjectInputStream(bis); // 每次重新构造对象输入流
                }catch (EOFException e) {
                    // e.printStackTrace();
//                    System.out.println("已达文件末尾");// 如果到达文件末尾，则退出循环
                    return redundancyFileStoreInfos;
                }
                Object object =oip.readObject();
                if (object instanceof ExpandFileStoreInfo) { // 判断对象类型
                    redundancyFileStoreInfos.add((ExpandFileStoreInfo) object);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
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
        return redundancyFileStoreInfos;
    }
}
