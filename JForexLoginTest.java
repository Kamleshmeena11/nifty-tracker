import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.FileWriter;
import java.io.IOException;

public class JForexLoginTest {
    // This string buffer holds our 1-second bars temporarily in RAM
    private static final StringBuilder csvBuffer = new StringBuilder();
    
    // Read secure credentials from the GitHub Cloud Environment variables
    static final String APP_ID       = System.getenv("FYERS_APP_ID");
    static final String ACCESS_TOKEN = System.getenv("FYERS_ACCESS_TOKEN");

    public static void main(String[] args) {
        System.out.println("🚀 Initializing Market Tracker Engine...");
        
        // 1. Start a parallel background timer that runs every 15 seconds
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            flushBufferToGDrive();
        }, 15, 15, TimeUnit.SECONDS);

        // 2. Insert your existing WebSocket Connection logic here:
        // Connect to wss://api.fyers.in/socket/v2/data/ using APP_ID and ACCESS_TOKEN
        // When your 1-second bar closes, call: appendBarToBuffer(timeStr, o, h, l, c, vol);
        
        System.out.println("🔌 WebSocket streaming active. Recording ticks...");
    }

    // Helper method your WebSocket loop calls every time a 1-second candle finishes
    public static synchronized void appendBarToBuffer(String time, double o, double h, double l, double c, int v) {
        String row = String.format("%s,%.2f,%.2f,%.2f,%.2f,%d\n", time, o, h, l, c, v);
        csvBuffer.append(row);
    }

    // This method saves data locally and pushes a cumulative copy to Google Drive
    private static synchronized void flushBufferToGDrive() {
        if (csvBuffer.length() == 0) return;

        try {
            // Take data out of RAM and clear the buffer immediately for incoming ticks
            String dataToUpload = csvBuffer.toString();
            csvBuffer.setLength(0); 

            // 1. APPEND the new 15-second batch to your running local file
            FileWriter fw = new FileWriter("live_1s_data.csv", true); // 'true' enables appending
            fw.write(dataToUpload);
            fw.close();

            // 2. Synchronize the growing file directly into your Google Drive folder
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "rclone copy live_1s_data.csv gdrive:/NiftyData/");
            Process p = pb.start();
            p.waitFor(); // Wait briefly for transmission to finish safely
            
            System.out.println("☁️ Synchronized updated dataset to Google Drive.");
        } catch (Exception e) {
            System.out.println("⚠️ Google Drive sync error: " + e.getMessage());
        }
    }
}
