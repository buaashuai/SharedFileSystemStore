package pers.sharedFileSystem.networkManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import pers.sharedFileSystem.communicationObject.FindRedundancyObject;
import pers.sharedFileSystem.communicationObject.MessageProtocol;
import pers.sharedFileSystem.communicationObject.MessageType;
import pers.sharedFileSystem.communicationObject.FingerprintInfo;
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
		FindRedundancySocketAction findRedundancySocketAction=new FindRedundancySocketAction(this,findRedundancyObject.fingerprintInfo);
		Thread thread = new Thread(findRedundancySocketAction);
		thread.start();
		return null;
	}

	private MessageProtocol doStopFindRedundancyAction(){

		overThis();
		return null;
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
//				return doAddFingerprintAction(mes);
			}
			case ADD_FINGERPRINTINFO:{
//				return doAddFingerprintAction(mes);
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
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(
                    socket.getOutputStream());
			oos.writeObject(reMessage);
			oos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		overThis();
	}
	public void run() {
		while (run) {
				try {
					InputStream in = socket.getInputStream();
					if (in.available() > 0) {
						ObjectInputStream ois = new ObjectInputStream(in);
						Object obj = ois.readObject();
						MessageProtocol mes=(MessageProtocol)obj;
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