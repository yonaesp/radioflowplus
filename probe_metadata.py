import urllib.request
import time

def probe_stream(url, name):
    print(f"\n--- Probando {name} ---")
    print(f"URL: {url}")
    
    req = urllib.request.Request(url)
    req.add_header('Icy-MetaData', '1')
    req.add_header('User-Agent', 'VLC/3.0.18 LibVLC/3.0.18')
    
    try:
        with urllib.request.urlopen(req, timeout=5) as response:
            print("Headers recibidos:")
            for k, v in response.headers.items():
                if 'icy' in k.lower():
                    print(f"  {k}: {v}")
            
            metaint = int(response.headers.get('icy-metaint', 0))
            if metaint == 0:
                print("[X] No icy-metaint header found. Stream might not support metadata or is HLS/AAC without ICY.")
                return

            print(f"[OK] Metadata Interval: {metaint} bytes")
            
            # Read minimal content to find first metadata chunk
            # This is a naive check; we just read a chunk and look for text
            content = response.read(metaint + 256) 
            
            # The byte after metaint bytes is length of metadata * 16
            length_byte = content[metaint]
            meta_len = length_byte * 16
            
            if meta_len > 0:
                metadata = content[metaint+1 : metaint+1+meta_len].decode('utf-8', errors='ignore')
                print(f"[MUSIC] Raw Metadata found: '{metadata}'")
            else:
                print("[WARN] Metadata length is 0 in first chunk.")
                
    except Exception as e:
        print(f"[ERROR] Error: {e}")

# Test stations from RadioStations.kt
stations = [
    ("Los 40", "https://playerservices.streamtheworld.com/api/livestream-redirect/Los40.mp3"),
    ("Rock FM", "https://rockfm-cope-rrcast.flumotion.com/cope/rockfm.mp3"),
    ("Europa FM", "https://radio-atres-live.ondacero.es/api/livestream-redirect/EFMAAC.aac") 
]

for name, url in stations:
    probe_stream(url, name)
