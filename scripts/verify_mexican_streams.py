import requests
import time

stations = [
    (201, "W Radio México", "https://playerservices.streamtheworld.com/api/livestream-redirect/XEWFMAAC.aac"),
    (202, "Los 40 México", "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_MEXICOAAC.aac"),
    (203, "EXA FM", "https://playerservices.streamtheworld.com/api/livestream-redirect/XHEXAAAC.aac"),
    (204, "La Mejor", "https://playerservices.streamtheworld.com/api/livestream-redirect/XHTIMAAC.aac"),
    (205, "Amor FM", "https://playerservices.streamtheworld.com/api/livestream-redirect/XHSHFMAAC.aac"),
    (206, "MVS Noticias", "https://20593.live.streamtheworld.com:443/XHMVSFM_SC"),
    (207, "Alfa 91.3", "https://playerservices.streamtheworld.com/api/livestream-redirect/XHFAJ_FMAAC.aac"),
    (208, "Beat 100.9", "https://playerservices.streamtheworld.com/api/livestream-redirect/XHSONFMAAC.aac"),
    (209, "Universal Stereo", "https://playerservices.streamtheworld.com/api/livestream-redirect/XHRED_FM.mp3"),
    (210, "Stereo Joya", "https://playerservices.streamtheworld.com/api/livestream-redirect/XEJP_FM.mp3"),
    (212, "La Ke Buena", "https://playerservices.streamtheworld.com/api/livestream-redirect/XEQFMAAC.aac"),
    (213, "Reactor 105.7", "http://s1.mexside.net:8002/stream"),
    (214, "Radio Fórmula 103.3", "http://stream.radiojar.com/3zcuxdmb4k0uv.mp3"),
    (215, "Mix 106.5", "https://playerservices.streamtheworld.com/api/livestream-redirect/XHDFMFM.mp3"),
    (216, "88.9 Noticias", "https://playerservices.streamtheworld.com/api/livestream-redirect/XHMFM.mp3"),
    (217, "La Z 107.3", "https://playerservices.streamtheworld.com/api/livestream-redirect/XEQR_FMAAC.aac"),
    (218, "Imagen Radio 90.5", "https://playerservices.streamtheworld.com/api/livestream-redirect/XEDA_FMAAC.aac"),
    (219, "Radio Educación", "https://s2.mexside.net/8172/stream"),
    (220, "Ibero 90.9", "https://shaincast.caster.fm:20866/listen.mp3"),
    (221, "Opus 94.5", "http://live.mystreamplayer.com/IMER"),
    (222, "El Fonógrafo", "https://playerservices.streamtheworld.com/api/livestream-redirect/XENAM.mp3")
]

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
    'Connection': 'keep-alive',
    'Icy-MetaData': '1'
}

def verify_stream(name, url):
    print(f"Testing {name} ...")
    try:
        # First attempt: HEAD/GET with stream=True to avoiding downloading everything
        r = requests.get(url, headers=headers, stream=True, timeout=10)
        
        status = r.status_code
        content_type = r.headers.get('Content-Type', '').lower()
        
        print(f"  Status: {status}")
        print(f"  Type: {content_type}")
        
        if status not in [200, 206]:
            print(f"  [X] FAILED: Bad status {status}")
            return False
            
        if 'application/vnd.apple.mpegurl' in content_type or 'application/x-mpegurl' in content_type:
            print(f"  [X] FAILED: IS HLS (User requested NO HLS)")
            return False
            
        if 'audio' not in content_type and 'mpeg' not in content_type and 'ogg' not in content_type and 'aac' not in content_type:
             print(f"  [!] WARNING: Content type {content_type} might not be audio")
             
        # Try reading a bit of data to confirm it's a real stream
        chunk = next(r.iter_content(chunk_size=1024), None)
        if not chunk:
             print(f"  [X] FAILED: No data received")
             return False
             
        print(f"  [OK] SUCCESS: Stream works")
        return True
        
    except Exception as e:
        print(f"  [X] FAILED: Exception {e}")
        return False

print("--- VERIFYING MEXICAN STREAMS ---")
failed_stations = []
for id, name, url in stations:
    if not verify_stream(name, url):
        failed_stations.append(name)
    print("-" * 30)

print(f"\nSummary: {len(failed_stations)} stations failed.")
for s in failed_stations:
    print(f"- {s}")
