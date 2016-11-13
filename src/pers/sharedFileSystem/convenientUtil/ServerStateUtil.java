package pers.sharedFileSystem.convenientUtil;

import com.sun.management.OperatingSystemMXBean;
import org.hyperic.sigar.*;
import org.hyperic.sigar.cmd.Shell;
import org.hyperic.sigar.cmd.SigarCommandBase;
import org.hyperic.sigar.shell.ShellCommandExecException;
import org.hyperic.sigar.shell.ShellCommandUsageException;
import pers.sharedFileSystem.communicationObject.ServerState;
import pers.sharedFileSystem.logManager.LogRecord;

import java.io.File;
import java.lang.management.ManagementFactory;


/**
 * 存储服务器运行状态
 */
public class ServerStateUtil extends SigarCommandBase {
    private boolean displayTimes = true;
    private static double cpu;

    /**
     * 获取当前系统的内存占用率
     * @return
     */
    public double getMemoryState(){
        OperatingSystemMXBean osmb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double totalMem= osmb.getTotalPhysicalMemorySize() / 1024 / 1024 / 1024;
        double freeMem=osmb.getFreePhysicalMemorySize() / 1024 / 1024 / 1024;
        double memPercentage= 100*(totalMem- freeMem)/totalMem;
        System.out.println("总内存："+totalMem+"GB");
        System.out.println("空闲内存："+freeMem+"GB");
        return  memPercentage;
    }

    /**
     * 获取当前系统的磁盘占用率
     * @return
     */
    public double getDiskState(){
        File[] roots = File.listRoots();//获取磁盘分区列表
        double totalDisk=0,freeDisk=0,diskPercentage;
        for (File file : roots) {
            totalDisk+= file.getTotalSpace()/1024/1024/1024;
            freeDisk+=file.getFreeSpace()/1024/1024/1024;
        }
        diskPercentage=100*(totalDisk- freeDisk)/totalDisk;
        LogRecord.RunningInfoLogger.info("总磁盘空间："+totalDisk+"GB\t"+"空闲磁盘空间："+freeDisk+"GB\t"+"磁盘占用率："+diskPercentage+"%");
        return  diskPercentage;
    }

    public ServerStateUtil(Shell shell) {
        super(shell);
    }

    public ServerStateUtil() {
        super();
    }

    private void output(CpuPerc cpu) {
        println("User Time....." + CpuPerc.format(cpu.getUser()));// 用户使用率
        println("Sys Time......" + CpuPerc.format(cpu.getSys()));// 系统CPU使用率
        println("Idle Time....." + CpuPerc.format(cpu.getIdle()));// 当前空闲率
        println("Wait Time....." + CpuPerc.format(cpu.getWait()));// 当前等待率
        println("Nice Time....." + CpuPerc.format(cpu.getNice()));
        println("Combined......" + CpuPerc.format(cpu.getCombined()));// 总的使用率
        println("Irq Time......" + CpuPerc.format(cpu.getIrq()));
        println("");
    }

    /**
     * 获取当前CPU占用率
     * @return
     */
    public double getCpuState(){
        String[] args = new String[0];
        try {
            new ServerStateUtil().processCommand(args);
        } catch (ShellCommandUsageException e) {
            e.printStackTrace();
        } catch (ShellCommandExecException e) {
            e.printStackTrace();
        }
        return cpu*100;
    }

    /**
     * 获取服务器运行状态参数
     * @return
     */
    public ServerState getServerState(){
        ServerState serverState = new ServerState();
        OperatingSystemMXBean osmb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double totalMem= osmb.getTotalPhysicalMemorySize() / 1024 / 1024 / 1024;
        double freeMem=osmb.getFreePhysicalMemorySize() / 1024 / 1024 / 1024;
        double memPercentage= 100*(totalMem- freeMem)/totalMem;
        serverState.TotalMemory = totalMem;
        serverState.FreeMemory =freeMem;
        serverState.MemoryState = memPercentage;

        File[] roots = File.listRoots();//获取磁盘分区列表
        double totalDisk=0,freeDisk=0,diskPercentage;
        for (File file : roots) {
            totalDisk+= file.getTotalSpace()/1024/1024/1024;
            freeDisk+=file.getFreeSpace()/1024/1024/1024;
        }
        diskPercentage=100*(totalDisk- freeDisk)/totalDisk;
        serverState.TotalDisk = totalDisk;
        serverState.FreeDisk = freeDisk;
        serverState.DiskState = diskPercentage;

//        serverState.CpuState = getCpuState();
        return serverState;
    }

    public void output(String[] args) throws SigarException {
        org.hyperic.sigar.CpuInfo[] infos =
                this.sigar.getCpuInfoList();
        CpuPerc[] cpus =
                this.sigar.getCpuPercList();
        org.hyperic.sigar.CpuInfo info = infos[0];
        if (!this.displayTimes) {
            return;
        }
        cpu=this.sigar.getCpuPerc().getSys();
//        output(this.sigar.getCpuPerc());
    }

    public static void main(String[] args) {
        ServerStateUtil serverState=new ServerStateUtil();
        System.out.println("内存占用率：" +serverState.getMemoryState()+ "%\n");
        System.out.println("磁盘占用率：" +serverState.getDiskState()+ "%\n");
        System.out.println("CPU占用率：" +serverState.getCpuState()+ "%");
    }
}
