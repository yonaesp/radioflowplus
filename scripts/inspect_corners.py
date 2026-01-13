
from PIL import Image
import sys

def inspect(path):
    try:
        img = Image.open(path).convert("RGBA")
        # Check 4 corners
        corners = [
            (0, 0),
            (img.width-1, 0),
            (0, img.height-1),
            (img.width-1, img.height-1)
        ]
        print(f"File: {path}")
        for x, y in corners:
            pixel = img.getpixel((x, y))
            print(f"  Corner ({x},{y}): {pixel}")
            
    except Exception as e:
        print(f"Error {path}: {e}")

if __name__ == "__main__":
    for f in sys.argv[1:]:
        inspect(f)
