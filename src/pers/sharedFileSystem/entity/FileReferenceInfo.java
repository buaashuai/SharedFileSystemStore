package pers.sharedFileSystem.entity;

import pers.sharedFileSystem.communicationObject.FingerprintInfo;

import java.io.Serializable;

/**
 * 文件外部引用信息
 */
public class FileReferenceInfo implements Serializable {
    /**
     * 文件相对路径（带文件名）
     */
    public String Path;
    /**
     * 文件被引用的频率
     */
    public int  Frequency;
    /**
     * 是否被上传这个文件的真实用户物理删除
     */
    public boolean isPhysicalDeletedByTrueUser;

    public FileReferenceInfo(){
        isPhysicalDeletedByTrueUser=false;
        Frequency=0;
    }
}