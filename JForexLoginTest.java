import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

public class JForexLoginTest {
    private static final StringBuilder csvBuffer = new StringBuilder();
    
    static final String APP_ID       = System.getenv("FYERS_APP_ID");
    static final String ACCESS_TOKEN = System.getenv("FYERS_ACCESS_TOKEN");

    public static void main(String[] args) {
        System.out.println("🚀 Initializing Market Tracker Engine...");
        
        String configPath = "rclone.conf";
        
        // INSTANT INITIALIZATION
        try {
            File file = new File("live_1s_data.csv");
            if (!file.exists()) {
                FileWriter fw = new FileWriter(file, false);
                fw.write("Timestamp,Open,High,Low,Close,Volume\n");
                fw.close();
            }
            
            System.out.println("📁 Syncing initial file layout to Google Drive...");
            
            // Added redirectErrorStream(true) to capture rclone output directly in GitHub Logs
            ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/c", 
                "rclone --config \"" + configPath + "\" copy live_1s_data.csv gdrive:NiftyData"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            // Print the live console responses from rclone
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[rclone-log] " + line);
            }
            
            p.waitFor();
            System.out.println("✅ Instant initialization run complete!");
        } catch (Exception e) {
            System.out.println("⚠️ Initialization sync failed: " + e.getMessage());
        }

        // 1. 15-Second Background Timer
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            flushBufferToGDrive(configPath);
        }, 15, 15, TimeUnit.SECONDS);

        System.out.println("🔌 WebSocket streaming active. Recording ticks...");
    }

    public static synchronized void appendBarToBuffer(String time, double o, double h, double l, double c, int v) {
        String row = String.format("%s,%.2f,%.2f,%.2f,%.2f,%d\n", time, o, h, l, c, v);
        csvBuffer.append(row);
    }

    private static synchronized void flushBufferToGDrive(String configPath) {
        if (csvBuffer.length() == 0) return;

        try {
            String dataToUpload = csvBuffer.toString();
            csvBuffer.setLength(0); 

            FileWriter fw = new FileWriter("live_1s_data.csv", true);
            fw.write(dataToUpload);
            fw.close();

            ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/c", 
                "rclone --config \"" + configPath + "\" copy live_1s_data.csv gdrive:NiftyData"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[rclone-loop-log] " + line);
            }
            p.waitFor(); 
            
            System.out.println("☁️ Synchronized updated dataset to Google Drive.");
        } catch (Exception e) {
            System.out.println("⚠️ Google Drive sync error: " + e.getMessage());
        }
    }
}
