package pers.sharedFileSystem.networkManager;

import pers.sharedFileSystem.communicationObject.MessageProtocol;
import pers.sharedFileSystem.communicationObject.MessageType;
import pers.sharedFileSystem.entity.SenderType;
import pers.sharedFileSystem.logManager.LogRecord;

import java.io.IOException;

/**
 * 存储端和冗余验证服务器之间的长连接维护线程
 *
 */
public class KeepAliveWatchRedundancy implements Runnable {
	private long checkDelay = 500;//线程多久循环一次
	private long keepAliveDelay = 20000;//握手协议发送时间间隔（毫秒）
	private boolean run;// 线程是否运行
	private long lastSendTime;//上次发送时间

	public KeepAliveWatchRedundancy() {
		lastSendTime=System.currentTimeMillis();
		this.run = true;
	}

	/**
	 * 终止线程
	 */
	public void stop() {
		run = false;
	}

	public void run() {
		while (run) {
			// 长时间未给服务端发送数据，维持长连接
			if (System.currentTimeMillis() - lastSendTime> keepAliveDelay) {// 长连接维护
				try {
					MessageProtocol queryMessage = new MessageProtocol();
					queryMessage.messageType = MessageType.KEEP_ALIVE;
					queryMessage.senderType = SenderType.STORE;
					FileSystemStore.sendMessageToRedundancyServer(queryMessage);
					lastSendTime=System.currentTimeMillis();
					LogRecord.RunningInfoLogger.info("send handshake to redundancyServer");
				} catch (IOException e) {
					e.printStackTrace();
					FileSystemStore.restartConnectToRedundancyServer();
					// 发送的处理方法
					LogRecord.RunningErrorLogger.error("net work error, can not connect to redundancyServer."+e.toString());
				}
			} else {
				try {
					Thread.sleep(checkDelay);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
		}
	}
}
