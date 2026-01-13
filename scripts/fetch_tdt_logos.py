
import requests
import json
import os

def fetch_and_extract():
    url = "https://www.tdtchannels.com/lists/radio.json"
    print(f"Fetching {url}...")
    try:
        r = requests.get(url)
        data = r.json()
        
        targets = {
            "Canal Fiesta": ["canal fiesta"],
            "esRadio": ["esradio"],
            "Euskadi Irratia": ["euskadi irratia"],
            "Gaztea": ["gaztea", "eitb gaztea"]
        }
        
        found = {}
        
        for country in data['countries']:
            for amb in country['ambits']:
                for station in amb['channels']:
                    name = station['name'].lower()
                    for target_key, keywords in targets.items():
                        if target_key not in found:
                            for kw in keywords:
                                if kw in name:
                                    print(f"Found {target_key}: {station['logo']}")
                                    found[target_key] = station['logo']
                                    
        return found
    except Exception as e:
        print(f"Error: {e}")
        return {}

def download_file(url, filename):
    try:
        r = requests.get(url)
        if r.status_code == 200:
            with open(filename, 'wb') as f:
                f.write(r.content)
            print(f"Downloaded {filename}")
            return True
    except Exception as e:
        print(f"Failed to download {url}: {e}")
    return False

if __name__ == "__main__":
    logos = fetch_and_extract()
    
    # Manual add for Amor FM if not found (it's Mexican, might be in there or not)
    # TDTChannels is mostly Spain, but has some intl.
    
    # Mapping to filenames
    mapping = {
        "Canal Fiesta": "logos/originals/logo_canal_fiesta_new.png",
        "esRadio": "logos/originals/logo_esradio_new.png",
        "Euskadi Irratia": "logos/originals/logo_euskadi_irratia_new.png",
        "Gaztea": "logos/originals/logo_gaztea_new.png"
    }
    
    if not os.path.exists("logos/originals"):
        os.makedirs("logos/originals")

    for name, url in logos.items():
        if name in mapping:
            download_file(url, mapping[name])
            
    # Amor FM (Mexico) - finding a specific URL
    # Using a known good source or the one from search if available
    # Search result 5: iHeartRadio is a partner.
    # Let's try to specific URL from the previous sources.md if it existed, or a new one.
    # Previous sources.md had: https://commons.wikimedia.org/wiki/File:XHSH-FM.png for Amor FM
    # But that might have been the one the user disliked?
    # Let's try searching Wikimedia for "Amor FM 95.3 logo"
    
    amor_url = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c2/XHSH-FM.png/600px-XHSH-FM.png" # Constructing typical Wikimedia URL
    # Or actually, the user said "Amor FM".
    # Let's download the one from the previous sources.md entry if it was good, or try to find one.
    # The previous conversation said: "logo_amor_fm | Wikimedia Commons | https://commons.wikimedia.org/wiki/File:XHSH-FM.png"
    # Wikimedia usually has transparent backgrounds.
    # I will download that one (using the raw URL).
    # Raw URL for File:XHSH-FM.png is usually https://upload.wikimedia.org/wikipedia/commons/f/f6/XHSH-FM.png (need to resolve actual path)
    
    # Better: Use the iHeart one if possible?
    # Let's stick to the 4 TDTChannels ones first.
