import json
import urllib.request
import sys

URL = "https://www.tdtchannels.com/lists/radio.json"

TARGETS = [
    "Los 40", "Cadena SER", "COPE", "Cadena 100", "Europa FM", "Rock FM", 
    "Cadena Dial", "Kiss FM", "Onda Cero", "RAC1", "LOS40 Dance", 
    "RNE", "Radio Nacional", # RNE variations
    "BBC Radio 1"
]

def main():
    print(f"Fetching {URL}...")
    try:
        req = urllib.request.Request(URL, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req) as response:
            data = json.loads(response.read().decode())
    except Exception as e:
        print(f"Download failed: {e}")
        return

    print("Parsing stations...")
    
    for country in data['countries']:
        country_name = country['name']
        # print(f"Scanning {country_name}...")
        
        for ambit in country['ambits']:
            for channel in ambit['channels']:
                name = channel['name']
                
                # Check against targets
                matched = False
                for t in TARGETS:
                    # Generic loose match
                    if t.lower() in name.lower():
                        # Refine matching
                        if t == "RNE" and "RNE" in name and "Radio Nacional" not in name and "RNE 1" not in name:
                             continue # Skip RNE 3, 5 if searching for RNE generics
                        matched = True
                        break
                
                if matched:
                    print(f"\n--- {name} ({country_name}) ---")
                    for opt in channel['options']:
                        fmt = opt.get('format', 'unknown')
                        url = opt.get('url', 'no-url')
                        print(f"  [{fmt}] {url}")

if __name__ == "__main__":
    main()
