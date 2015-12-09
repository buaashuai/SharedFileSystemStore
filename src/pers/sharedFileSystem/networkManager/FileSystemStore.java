package pers.sharedFileSystem.networkManager;


/**
 * 文件系统存储端，运行在每个存储服务器上面
 */
public class FileSystemStore {

    /**
     * 初始化
     */
    private void initServerSocket(){
        ConnWatchDog connWatchDog = new ConnWatchDog();
        Thread connWatchDogThread = new Thread(connWatchDog);
        connWatchDogThread.start();
    }

    public FileSystemStore() {
        initServerSocket();
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        new FileSystemStore();
    }
}
