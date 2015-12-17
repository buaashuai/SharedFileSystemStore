package pers.sharedFileSystem.networkManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import pers.sharedFileSystem.communicationObject.*;
import pers.sharedFileSystem.logManager.LogRecord;
import pers.sharedFileSystem.systemFileManager.FingerprintAdapter;

/**
 * 监控某个连接（客户端或者存储服务器）发来的消息
 */
public class SocketAction implements Runnable {
	/**
	 * 监听的连接
	 */
	private Socket socket;
	/**
	 * 该线程是否正在监听
	 */
	private boolean run = true;
	/**
	 * 最近一次收到客户端发来信息的时间
	 */
	private long lastReceiveTime;
	/**
	 *  接收延迟时间间隔
	 */
	private long receiveTimeDelay = 5000;
	/**
	 * 查找冗余文件消息的线程
	 */
	private FindRedundancySocketAction findRedundancySocketAction;

	public SocketAction(Socket s) {
		this.socket = s;
		lastReceiveTime = System.currentTimeMillis();
	}

	/**
	 * 启动处理查找冗余文件消息线程
	 * @param mes
	 * @return
	 */
	private MessageProtocol doFindRedundancyAction(MessageProtocol mes){
		FindRedundancyObject findRedundancyObject=(FindRedundancyObject)mes.content;
		findRedundancySocketAction=new FindRedundancySocketAction(this,findRedundancyObject.fingerprintInfo);
		Thread thread = new Thread(findRedundancySocketAction);
		thread.start();
		return null;
	}

	/**
	 * 停止查找冗余文件消息的线程
	 * @return
	 */
	private MessageProtocol doStopFindRedundancyAction(){
		findRedundancySocketAction.overThis();
		overThis();
		return null;
	}
	/**
	 * 添加冗余文件信息
	 * @return
	 */
	private MessageProtocol doAddRedundancyAction(MessageProtocol mes){
		MessageProtocol reMes=new MessageProtocol();
		RedundancyFileStoreInfo redundancyFileStoreInfo=(RedundancyFileStoreInfo)mes.content;
		boolean re=FileSystemStore.addRedundancyFileStoreInfo(redundancyFileStoreInfo);
		if(re)
			reMes.messageCode=4000;
		else
			reMes.messageCode=4003;
		reMes.messageType=MessageType.REPLY_ADD_REDUNDANCY_INFO;
		return reMes;
	}
	/**
	 * 添加指纹信息
	 * @return
	 */
	private MessageProtocol doAddFingerprintAction(MessageProtocol mes){
		MessageProtocol reMes=new MessageProtocol();
		FingerprintInfo fingerprintInfo=(FingerprintInfo)mes.content;
		boolean re=FingerprintAdapter.saveFingerprint(fingerprintInfo);
		if(re)
			reMes.messageCode=4000;
		else
			reMes.messageCode=4004;
		reMes.messageType=MessageType.REPLY_ADD_FINGERPRINTINFO;
		return reMes;
	}
	/**
	 * 收到消息之后进行分类处理
	 * @param mes
	 * @return
	 */
	private MessageProtocol doAction(MessageProtocol mes){
		switch (mes.messageType){
			case FIND_REDUNDANCY:{
				return doFindRedundancyAction(mes);
			}
			case STOP_FIND_REDUNDANCY:{
				return doStopFindRedundancyAction();
			}
			case ADD_REDUNDANCY_INFO:{
				return doAddRedundancyAction(mes);
			}
			case ADD_FINGERPRINTINFO:{
				return doAddFingerprintAction(mes);
			}
			case GET_REDUNDANCY_INFO:{
//				return doGetRedundancyAction(mes);
			}
			case KEEP_ALIVE:{
				LogRecord.RunningInfoLogger.info("receive handshake");
				return null;
			}
			case SOCKET_MONITOR:{
				return null;
			}
			default:{
				return null;
			}
		}

	}

	/**
	 * 给冗余验证服务器返回查找结果
	 * @param fInfo
	 */
	public void sendFingerprintInfoToRedundancy(FingerprintInfo fInfo){
		MessageProtocol reMessage=new MessageProtocol();
		reMessage.messageType=MessageType.REPLY_FIND_REDUNDANCY;
		reMessage.content=fInfo;
		String str="null";
		if(fInfo!=null){
			str=fInfo.toString();
		}
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(
                    socket.getOutputStream());
			oos.writeObject(reMessage);
			oos.flush();
			LogRecord.RunningInfoLogger.info("send REPLY_FIND_REDUNDANCY, FingerprintInfo="+str);
		} catch (IOException e) {
			e.printStackTrace();
		}
//		overThis();
	}
	public void run() {
		while (run) {
			// 超过接收延迟时间（毫秒）之后，终止此客户端的连接
			if (System.currentTimeMillis() - lastReceiveTime > receiveTimeDelay) {
				overThis();
			} else {
				try {
					InputStream in = socket.getInputStream();
					if (in.available() > 0) {
						ObjectInputStream ois = new ObjectInputStream(in);
						Object obj = ois.readObject();
						MessageProtocol mes = (MessageProtocol) obj;
						lastReceiveTime = System.currentTimeMillis();
						MessageProtocol out = doAction(mes);// 处理消息，并给客户端反馈
						if (out != null) {
							ObjectOutputStream oos = new ObjectOutputStream(
									socket.getOutputStream());
							oos.writeObject(out);
							oos.flush();
						}
					} else {
						Thread.sleep(10);
					}
				} catch (Exception e) {
					e.printStackTrace();
					overThis();
				}
			}
		}

	}

	/**
	 * 关闭此socket连接，停止对该连接的监控
	 */
	public void overThis() {
		if (run)
			run = false;
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		LogRecord.RunningInfoLogger.info("close " + socket.getRemoteSocketAddress());
	}

}