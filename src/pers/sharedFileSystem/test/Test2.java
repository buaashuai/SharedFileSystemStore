package pers.sharedFileSystem.test;

import java.util.Hashtable;
import java.util.List;

import pers.sharedFileSystem.communicationObject.FingerprintInfo;
import pers.sharedFileSystem.communicationObject.RedundancyFileStoreInfo;
import pers.sharedFileSystem.configManager.Config;
import pers.sharedFileSystem.entity.FileType;
import pers.sharedFileSystem.entity.ServerNode;
import pers.sharedFileSystem.entity.SystemConfig;
import pers.sharedFileSystem.networkManager.FileSystemStore;
import pers.sharedFileSystem.systemFileManager.FingerprintAdapter;
import pers.sharedFileSystem.systemFileManager.RedundantFileAdapter;

public class Test2 {

	/**
	 * 配置文件解析测试
	 *
	 * @throws Exception
	 */
	private void configTest() throws Exception {
		Hashtable<String, ServerNode> config = Config.getConfig();
		ServerNode serverNode = config.get("tempNode");
		serverNode.print("");
		System.out.println("*****************");
		SystemConfig systemConfig = Config.SYSTEMCONFIG;
		systemConfig.print("");
		System.out.println("*****************");
	}

	/**
	 * 获取全部指纹信息测试
	 */
	private void getAllFingerprintInfoTest() {
		List<FingerprintInfo> fingerprintInfos = FingerprintAdapter
				.getAllFingerprintInfo();
		for (FingerprintInfo info : fingerprintInfos) {
			System.out.println(info);
		}
		System.out
				.println("getAllFingerprintInfoTest: ********************* num= "
						+ fingerprintInfos.size() + "\n");
	}


	/**
	 * 保存指纹信息测试
	 */
	private void saveFingerprintTest() {
		FingerprintAdapter fingerprintAdapter = new FingerprintAdapter();
		FingerprintInfo fingerprintInfo = new FingerprintInfo("123456789",
				"temp", "e:/df", "a.txt", FileType.ANY);
		FingerprintAdapter.saveFingerprint(fingerprintInfo);
	}

	/**
	 * 添加冗余信息测试
	 */
	private void addRedundancyFileStoreInfoTest() {
		RedundancyFileStoreInfo redundancyFileStoreInfo = new RedundancyFileStoreInfo();
		redundancyFileStoreInfo.essentialStorePath = "/temp/te3";
		FingerprintInfo fingerprintInfo = new FingerprintInfo("13456", "temp",
				"e:/df", "a.txt", FileType.ANY);
		redundancyFileStoreInfo.addFingerprintInfo(fingerprintInfo);
		FileSystemStore.addRedundancyFileStoreInfo(redundancyFileStoreInfo);
	}

	/**
	 * 获取全部冗余信息测试
	 */
	private void getAllRedundancyFileStoreInfoTest() {
		List<RedundancyFileStoreInfo> infos = RedundantFileAdapter
				.getAllRedundancyFileStoreInfo();
		for (RedundancyFileStoreInfo in : infos) {
			System.out.println("essentialStorePath:" + in.essentialStorePath);
			System.out.println("otherFileInfo:");
			for (FingerprintInfo fi : in.otherFileInfo) {
				System.out.println("\t" + fi);
			}
		}
		System.out
				.println("getAllRedundancyFileStoreInfoTest: ********************* num= "
						+ infos.size() + "\n");
	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		new FileSystemStore();
		Test2 test2 = new Test2();
		System.out.println("");
		test2.getAllFingerprintInfoTest();
		test2.getAllRedundancyFileStoreInfoTest();
//		test2.configTest();
	}

}
