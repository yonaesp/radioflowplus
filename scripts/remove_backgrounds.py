
from PIL import Image
import os
import argparse

def remove_background(input_path, color_to_remove='white', tolerance=30):
    try:
        print(f"Processing {input_path}...")
        img = Image.open(input_path).convert("RGBA")
        datas = img.getdata()
        
        newData = []
        
        target_r, target_g, target_b = (255, 255, 255) if color_to_remove == 'white' else (0, 0, 0)
        
        for item in datas:
            # item is (r,g,b,a)
            r, g, b = item[0], item[1], item[2]
            
            # Calculate distance from target color
            diff_r = abs(r - target_r)
            diff_g = abs(g - target_g)
            diff_b = abs(b - target_b)
            
            if diff_r <= tolerance and diff_g <= tolerance and diff_b <= tolerance:
                # Transparent
                newData.append((255, 255, 255, 0))
            else:
                newData.append(item)
        
        img.putdata(newData)
        
        # Trim transparency (crop)
        bbox = img.getbbox()
        if bbox:
            img = img.crop(bbox)
        
        # Save
        if input_path.endswith('.webp'):
            output_path = input_path
        else:
            output_path = os.path.splitext(input_path)[0] + ".webp"
            
        img.save(output_path, "WEBP")
        print(f"Fixed: {output_path}")
        return True
    except Exception as e:
        print(f"Error processing {input_path}: {e}")
        return False

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('files', nargs='+', help='List of files to process')
    parser.add_argument('--color', help='Color to remove (white, black, or r,g,b)')
    parser.add_argument('--tolerance', type=int, default=30, help='Color matching tolerance (0-255)')
    
    args = parser.parse_args()
    
    target_color = 'white'
    if args.color:
        if ',' in args.color:
            try:
                # Custom RGB
                parts = [int(p) for p in args.color.split(',')]
                if len(parts) == 3:
                     # Modify the function to accept tuple, or patch it here
                     pass
            except:
                print("Invalid color format. Use r,g,b")
                return
        else:
            target_color = args.color

    for f in args.files:
        if os.path.exists(f):
            # Parse color inside loop or pass args
            remove_background_v2(f, args.color, args.tolerance)
        else:
            print(f"File not found: {f}")

def remove_background_v2(input_path, color_arg, tolerance):
    try:
        img = Image.open(input_path).convert("RGBA")
        datas = img.getdata()
        
        # Determine target color
        if color_arg == 'black':
            target = (0, 0, 0)
        elif color_arg and ',' in color_arg:
            target = tuple(map(int, color_arg.split(',')))
        else:
            # Default to white
            target = (255, 255, 255)
            
        target_r, target_g, target_b = target
        print(f"Processing {input_path} (Removing {target})...")
        
        newData = []
        for item in datas:
            r, g, b = item[0], item[1], item[2]
            diff_r = abs(r - target_r)
            diff_g = abs(g - target_g)
            diff_b = abs(b - target_b)
            
            if diff_r <= tolerance and diff_g <= tolerance and diff_b <= tolerance:
                newData.append((255, 255, 255, 0))
            else:
                newData.append(item)
        
        img.putdata(newData)
        
        # Crop
        # bbox = img.getbbox()
        # if bbox:
        #    img = img.crop(bbox)
            
        # Save
        img.save(input_path, "WEBP")
        print(f"Fixed: {input_path}")
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()
