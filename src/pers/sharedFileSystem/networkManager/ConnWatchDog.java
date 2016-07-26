package pers.sharedFileSystem.networkManager;

import pers.sharedFileSystem.configManager.Config;
import pers.sharedFileSystem.entity.SystemConfig;
import pers.sharedFileSystem.logManager.LogRecord;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

/**
 * 监听新客户端连接的线程
 * 
 */
public class ConnWatchDog implements Runnable {
	/**
	 * 已连接的客户端集合
	 */
	private Hashtable<SocketAction, Thread> threads = new Hashtable<SocketAction, Thread>();

	/**
	 * 服务端配置文件
	 */
	private SystemConfig systemConfig = Config.SYSTEMCONFIG;

	public ConnWatchDog() {
		run=true;
	}

	/**
	 * 冗余验证服务器是否运行
	 */
	private volatile boolean run;

	private ServerSocket serverSocket;

	/**
	 * 停止服务端监听
	 */
	public void shutDownSocket() {
		run=false;
		if (serverSocket != null)
			try {
				serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		for (SocketAction socketAction : threads.keySet()) {
			socketAction.overThis();
		}
		threads.clear();
	}

	public void run() {
		serverSocket = null;
		try {
			serverSocket = new ServerSocket(systemConfig.Port, 100);
			LogRecord.RunningInfoLogger.info("storage server started at port "+systemConfig.Port);
			while(run) {
				Socket s = serverSocket.accept();
				LogRecord.RunningInfoLogger.info("new client connect [" + s.getInetAddress() +":"+s.getLocalPort()+ "]");
				SocketAction socketAction = new SocketAction(s);
				Thread thread = new Thread(socketAction);
//				threads.put(socketAction, thread);
				thread.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (serverSocket != null)
				try {
					serverSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
}
