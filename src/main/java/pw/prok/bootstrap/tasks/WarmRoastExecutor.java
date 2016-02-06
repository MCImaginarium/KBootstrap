package pw.prok.bootstrap.tasks;

import com.sk89q.warmroast.WarmRoast;
import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.net.InetSocketAddress;

public class WarmRoastExecutor implements Runnable {
    private final String address;
    private final int port;
    private final int pid;
    private final File mappingsDir;

    public WarmRoastExecutor(String... args) {
        address = args[0];
        port = Integer.parseInt(args[1]);
        pid = Integer.parseInt(args[2]);
        mappingsDir = new File(args[3]);
    }

    public static void main(String... args) {
        new Thread(new WarmRoastExecutor(args)).start();
    }

    @Override
    public void run() {
        try {
            InetSocketAddress socketAddress = new InetSocketAddress(address, port);
            VirtualMachine vm = VirtualMachine.attach(String.valueOf(pid));
            WarmRoast warmRoast = new WarmRoast(vm, 100);
            warmRoast.getMapping().read(new File(mappingsDir, "joined.srg"), new File(mappingsDir, "methods.csv"));
            warmRoast.connect();
            warmRoast.start(socketAddress);
        } catch (Exception e) {
            new RuntimeException("Failed to run warmroast", e).printStackTrace();
        }

    }
}
