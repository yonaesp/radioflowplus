import requests

# Test Europa FM and other potentially problematic streams
streams = {
    "Europa FM": "https://23133.live.streamtheworld.com/EUROPA_FM.mp3",
}

headers = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36'
}

for name, url in streams.items():
    print(f"\n🔍 Testing: {name}")
    print(f"URL: {url}")
    try:
        response = requests.get(url, headers=headers, stream=True, timeout=10)
        print(f"✅ Status: {response.status_code}")
        print(f"Content-Type: {response.headers.get('Content-Type', 'N/A')}")
        
        # Try to read first chunk
        chunk = next(response.iter_content(chunk_size=8192), None)
        if chunk:
            print(f"✅ Stream delivers data ({len(chunk)} bytes)")
        else:
            print("❌ Stream connected but no data")
    except requests.Timeout:
        print("❌ TIMEOUT (stream too slow)")
    except Exception as e:
        print(f"❌ ERROR: {e}")
