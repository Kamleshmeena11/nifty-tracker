import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.*;

public class JForexLoginTest {
    private static final StringBuilder csvBuffer = new StringBuilder();
    static final String CONFIG_PATH = "rclone.conf";

    public static void main(String[] args) {
        System.out.println("🚀 Initializing Market Tracker Engine...");
        
        try {
            File file = new File("live_1s_data.csv");
            if (!file.exists()) {
                try (FileWriter fw = new FileWriter(file, false)) {
                    fw.write("Timestamp,Open,High,Low,Close,Volume\n");
                }
            }
            
            // Initial Sync
            runRclone();
            
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(JForexLoginTest::flushBufferToGDrive, 15, 15, TimeUnit.SECONDS);
            
            System.out.println("🔌 WebSocket streaming active. Recording ticks...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static synchronized void flushBufferToGDrive() {
        if (csvBuffer.length() == 0) return;

        try {
            // Write buffer to file and ensure it is saved
            try (FileWriter fw = new FileWriter("live_1s_data.csv", true)) {
                fw.write(csvBuffer.toString());
                fw.flush();
            }
            csvBuffer.setLength(0); 

            runRclone();
            System.out.println("☁️ Synchronized updated dataset to Google Drive.");
        } catch (Exception e) {
            System.out.println("⚠️ Sync error: " + e.getMessage());
        }
    }

    private static void runRclone() throws Exception {
        // OS-independent command execution
        ProcessBuilder pb = new ProcessBuilder("rclone", "--config", CONFIG_PATH, "copy", "live_1s_data.csv", "gdrive:NiftyData");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[rclone] " + line);
            }
        }
        p.waitFor();
    }

    public static synchronized void appendBarToBuffer(String time, double o, double h, double l, double c, int v) {
        csvBuffer.append(String.format("%s,%.2f,%.2f,%.2f,%.2f,%d\n", time, o, h, l, c, v));
    }
}
