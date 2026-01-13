
from PIL import Image, ImageOps
import os

def process_logo(input_path, output_path, bg_color=None, tolerance=40):
    try:
        print(f"Processing {input_path}...")
        if not os.path.exists(input_path):
            print(f"Skipping {input_path} (Not found)")
            return

        img = Image.open(input_path).convert("RGBA")
        
        # 1. Remove Background (if color specified)
        if bg_color:
            datas = img.getdata()
            newData = []
            
            target_r, target_g, target_b = bg_color
            
            for item in datas:
                r, g, b, a = item
                diff_r = abs(r - target_r)
                diff_g = abs(g - target_g)
                diff_b = abs(b - target_b)
                
                if diff_r <= tolerance and diff_g <= tolerance and diff_b <= tolerance:
                    newData.append((255, 255, 255, 0))
                else:
                    newData.append(item)
            
            img.putdata(newData)
            
        # Crop to content (Always do this to fix sizes)
        bbox = img.getbbox()
        if bbox:
            img = img.crop(bbox)

        # 2. Resize/Pad to 512x512
        target_size = (512, 512)
        
        # Calculate aspect-ratio preserving size
        # We want a bit of padding (e.g. 90% fill)
        target_fill = 460
        
        ratio = min(target_fill / img.width, target_fill / img.height)
        new_size = (int(img.width * ratio), int(img.height * ratio))
        
        img = img.resize(new_size, Image.Resampling.LANCZOS)
        
        # Create new transparent canvas
        new_img = Image.new("RGBA", target_size, (0, 0, 0, 0))
        
        # Paste centered
        x = (target_size[0] - new_size[0]) // 2
        y = (target_size[1] - new_size[1]) // 2
        new_img.paste(img, (x, y))
        
        # Save
        new_img.save(output_path, "WEBP", quality=90)
        print(f"Saved to {output_path}")
        
    except Exception as e:
        print(f"Error processing {input_path}: {e}")

if __name__ == "__main__":
    # Define tasks: (Input, Output, BgColorToRemove)
    tasks = [
        ("logos/originals/logo_esradio_new.png", "app/src/main/res/drawable/logo_esradio.webp", (255, 255, 255)),
        ("logos/originals/logo_canal_fiesta_new.png", "app/src/main/res/drawable/logo_canal_fiesta.webp", (230, 51, 44)),
        ("logos/originals/logo_euskadi_irratia_new.png", "app/src/main/res/drawable/logo_euskadi_irratia.webp", (228, 0, 51)),
        ("logos/originals/logo_gaztea_new.png", "app/src/main/res/drawable/logo_gaztea.webp", (63, 13, 40)),
        ("logos/originals/logo_amor_fm.png", "app/src/main/res/drawable/logo_amor_fm.webp", None) # Already transparent, just resize
    ]
    
    for input_p, output_p, color in tasks:
        process_logo(input_p, output_p, color)
