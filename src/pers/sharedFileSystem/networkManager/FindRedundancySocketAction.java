package pers.sharedFileSystem.networkManager;

import pers.sharedFileSystem.communicationObject.FingerprintInfo;
import pers.sharedFileSystem.configManager.Config;
import pers.sharedFileSystem.convenientUtil.CommonUtil;
import pers.sharedFileSystem.logManager.LogRecord;

import java.io.*;

/**
 * 查找冗余文件线程
 */
public class FindRedundancySocketAction implements Runnable {
	/**
	 * 父线程
	 */
	private SocketAction socketAction;
	/**
	 * 该线程是否正在运行
	 */
	private volatile boolean run = true;
	/**
	 * 待查找的指纹信息
	 */
	private FingerprintInfo srcFingerprintInfo;

	public FindRedundancySocketAction(SocketAction s,FingerprintInfo f) {
		this.socketAction = s;
		this.srcFingerprintInfo=f;
	}

	public void run() {
		String filePath= Config.SYSTEMCONFIG.FingerprintStorePath;//指纹信息的保存路径
		String fileName=  Config.SYSTEMCONFIG.FingerprintName;
		FileInputStream fin = null;
		BufferedInputStream bis =null;
		ObjectInputStream oip=null;
		FingerprintInfo retFingerprintInfo=null;
		if(!CommonUtil.validateString(filePath)){
			LogRecord.FileHandleErrorLogger.error("get Fingerprint error, filePath is null.");
			socketAction.sendFingerprintInfoToRedundancy(retFingerprintInfo);
			socketAction.overThis();
			return;
		}
		File file = new File(filePath);
		if (!file.isDirectory()||!new File(filePath+"/"+fileName).exists()) {
			socketAction.sendFingerprintInfoToRedundancy(retFingerprintInfo);
			socketAction.overThis();
			return;//如果系统文件夹不存在或者指纹信息文件不存在
		}
		try{
			fin = new FileInputStream(filePath+"/"+fileName);
			bis = new BufferedInputStream(fin);
			while (run) {
				try {
					oip = new ObjectInputStream(bis); // 每次重新构造对象输入流
				}catch (EOFException e) {
					// e.printStackTrace();
//                    System.out.println("已达文件末尾");// 如果到达文件末尾，则退出循环
					socketAction.sendFingerprintInfoToRedundancy(retFingerprintInfo);
					overThis();
					return;
				}
				Object object = new Object();
				object = oip.readObject();
				if (object instanceof FingerprintInfo) { // 判断对象类型
					FingerprintInfo tmp=(FingerprintInfo)object;
					if(tmp.Md5.equals(srcFingerprintInfo.Md5)) {
						socketAction.sendFingerprintInfoToRedundancy(tmp);
						socketAction.overThis();
						return;
					}
				}
			}
		} catch (FileNotFoundException e) {
			overThis();
			e.printStackTrace();
		} catch (IOException e) {
			overThis();
			e.printStackTrace();
		}catch (ClassNotFoundException e) {
			overThis();
			e.printStackTrace();
		}finally {
			overThis();
			socketAction.sendFingerprintInfoToRedundancy(retFingerprintInfo);
			socketAction.overThis();
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
	 * 停止查找
	 */
	public void overThis() {
		if (run)
			run = false;
		LogRecord.RunningInfoLogger.info("stop find fingerPrint.");
	}

}