package pers.sharedFileSystem.communicationObject;

import pers.sharedFileSystem.entity.FileType;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 指纹信息类
 */
public class FingerprintInfo  implements Serializable {
    /**
     * 文件指纹
     */
    public String Md5;
    /**
     *文件存储的节点编号
     */
    public String NodeId;
    /**
     * 文件相对路径
     */
    public String FilePath;
    /**
     * 文件名（带后缀）
     */
    public String FileName;
    /**
     * 文件类型
     */
    public FileType fileType;

    /**
     * 添加日期
     */
    public Date UpdateTime;

    public FingerprintInfo(){}

    public  FingerprintInfo(String md5,String nodeId,String filePath,String fileName,FileType type){
        this.Md5=md5;
        this.NodeId=nodeId;
        this.FilePath=filePath;
        this.FileName=fileName;
        this.fileType=type;
        this.UpdateTime=new Date();
    }

    public  FingerprintInfo(String md5,FileType type){
        this.Md5=md5;
        this.fileType=type;
    }

    public String toString(){
        String str="";
        SimpleDateFormat dateFm = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化当前系统日期
        String time = dateFm.format(new Date());
        str+="UpdateTime: "+time+" , ";
        str+="Md5: "+Md5+" , ";
        str+="NodeId: "+NodeId+" , ";
        str+="FilePath: "+FilePath+" , ";
        str+="FileName: "+FileName+" , ";
        str+="fileType: "+fileType;
        return str;
    }
}