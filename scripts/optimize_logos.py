"""
Logo Optimization Script - ONLY resize and convert to WebP

This script does NOT attempt to remove backgrounds.
It assumes input logos already have correct transparency.

Usage:
    python scripts/optimize_logos.py --input logo.png --output logo.webp
    python scripts/optimize_logos.py --input-dir logos/originals --output-dir logos/processed
"""

import argparse
import os
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print("ERROR: Missing Pillow. Install with: pip install pillow")
    exit(1)

# Configuration
TARGET_SIZE = 512
PADDING_PERCENT = 0.12
WEBP_QUALITY = 90


def verify_transparency(img):
    """Check if image has real alpha channel."""
    if img.mode != 'RGBA':
        return False, "Not RGBA mode"
    
    # Check if any pixel has alpha < 255
    alpha = img.getchannel('A')
    if alpha.getextrema()[0] == 255:
        return False, "No transparent pixels found"
    
    return True, "Has transparency"


def crop_to_content(img):
    """Crop to non-transparent bounding box."""
    bbox = img.getbbox()
    if bbox:
        return img.crop(bbox)
    return img


def resize_and_center(img, target_size, padding_percent):
    """Resize and center on transparent canvas."""
    content_size = int(target_size * (1 - padding_percent * 2))
    
    # Scale to fit
    ratio = min(content_size / img.width, content_size / img.height)
    new_w = max(1, int(img.width * ratio))
    new_h = max(1, int(img.height * ratio))
    
    img_resized = img.resize((new_w, new_h), Image.LANCZOS)
    
    # Center on transparent canvas
    canvas = Image.new('RGBA', (target_size, target_size), (0, 0, 0, 0))
    offset_x = (target_size - new_w) // 2
    offset_y = (target_size - new_h) // 2
    canvas.paste(img_resized, (offset_x, offset_y), img_resized)
    
    return canvas


def optimize_logo(input_path, output_path, verbose=False):
    """
    Optimize a single logo:
    1. Verify transparency
    2. Crop to content
    3. Resize to 512x512 with padding
    4. Save as WebP
    """
    try:
        input_path = Path(input_path)
        output_path = Path(output_path)
        
        if verbose:
            print(f"\nProcessing: {input_path.name}")
        
        # Load and convert to RGBA
        img = Image.open(input_path).convert('RGBA')
        original_size = os.path.getsize(input_path)
        
        # Check transparency
        has_transparency, msg = verify_transparency(img)
        if verbose:
            print(f"  Transparency: {msg}")
        
        # Crop to content
        img = crop_to_content(img)
        if verbose:
            print(f"  Content size: {img.width}x{img.height}")
        
        # Resize and center
        img = resize_and_center(img, TARGET_SIZE, PADDING_PERCENT)
        
        # Save
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path = output_path.with_suffix('.webp')
        img.save(str(output_path), 'WEBP', quality=WEBP_QUALITY, method=6)
        
        new_size = os.path.getsize(output_path)
        
        status = "[OK]" if has_transparency else "[WARN]"
        print(f"{status} {input_path.name} -> {output_path.name} ({new_size//1024}KB)")
        
        return True
        
    except Exception as e:
        print(f"[ERROR] {input_path}: {e}")
        return False


def optimize_directory(input_dir, output_dir, verbose=False):
    """Optimize all logos in directory."""
    input_path = Path(input_dir)
    output_path = Path(output_dir)
    
    # Find all image files
    patterns = ['*.png', '*.jpg', '*.jpeg', '*.webp']
    files = []
    for pattern in patterns:
        files.extend(input_path.glob(f'logo_{pattern}'))
        files.extend(input_path.glob(pattern))
    
    # Deduplicate
    files = list(set(files))
    
    if not files:
        print(f"No logo files found in {input_dir}")
        return
    
    print(f"Found {len(files)} logos to optimize\n")
    
    success = 0
    for f in sorted(files):
        out_file = output_path / (f.stem + '.webp')
        if optimize_logo(str(f), str(out_file), verbose):
            success += 1
    
    print(f"\n[DONE] Optimized {success}/{len(files)} logos")


def main():
    parser = argparse.ArgumentParser(description='Optimize logos (resize + WebP)')
    
    input_group = parser.add_mutually_exclusive_group(required=True)
    input_group.add_argument('--input', '-i', help='Single input file')
    input_group.add_argument('--input-dir', '-d', help='Input directory')
    
    parser.add_argument('--output', '-o', help='Output file')
    parser.add_argument('--output-dir', help='Output directory')
    parser.add_argument('--verbose', '-v', action='store_true')
    
    args = parser.parse_args()
    
    if args.input:
        if not args.output:
            args.output = Path(args.input).with_suffix('.webp')
        optimize_logo(args.input, args.output, args.verbose)
    else:
        if not args.output_dir:
            args.output_dir = args.input_dir
        optimize_directory(args.input_dir, args.output_dir, args.verbose)


if __name__ == '__main__':
    main()
