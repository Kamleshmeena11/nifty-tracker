import time
import os
from fyers_apiv3 import fyersModel
from fyers_apiv3.FyersWebsocket import data_ws

# Fetches credentials automatically from GitHub Actions Secrets
APP_ID = os.getenv("FYERS_APP_ID")
ACCESS_TOKEN = os.getenv("FYERS_ACCESS_TOKEN")

def on_message(message):
    if "ltp" in message:
        timestamp = time.strftime('%Y-%m-%d %H:%M:%S')
        ltp = message.get("ltp")
        vol = message.get("vol", 0)
        
        # Format: Timestamp, Open, High, Low, Close, Volume
        row = f"{timestamp},{ltp},{ltp},{ltp},{ltp},{vol}\n"
        
        with open("live_1s_data.csv", "a") as f:
            f.write(row)
        print(f"📥 Captured live tick: {row.strip()}")

def on_error(message):
    print(f"⚠️ Error: {message}")

def on_close(message):
    print("🔌 Connection closed.")

def on_open():
    # Subscribing to Nifty 50 Index Spot Ticks
    data_type = "SymbolUpdate"
    symbols = ["NSE:NIFTY50-INDEX"]
    fyers_ws.subscribe(symbols=symbols, data_type=data_type)

if not APP_ID or not ACCESS_TOKEN:
    print("❌ Missing App ID or Access Token in environment secrets!")
    exit(1)

# Initialize WebSocket Connection
fyers_ws = data_ws.FyersDataSocket(
    access_token=f"{APP_ID}:{ACCESS_TOKEN}",
    log_path=os.getcwd(),
    litemode=True,
    on_connect=on_open,
    on_message=on_message,
    on_error=on_error,
    on_close=on_close
)

print("🔌 Connecting to Fyers Live Feed via Python...")
fyers_ws.connect()
