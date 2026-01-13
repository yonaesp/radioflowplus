"""
Logo Validation Script

Generates an HTML report showing each logo on light and dark backgrounds
to validate transparency. Also reports size and dimension issues.

Usage:
    python scripts/validate_logos.py --input-dir app/src/main/res/drawable
"""

import argparse
import base64
import os
from pathlib import Path

try:
    from PIL import Image
    import numpy as np
except ImportError:
    print("ERROR: Missing dependencies. Install with:")
    print("  pip install pillow numpy")
    exit(1)


def check_transparency(img_path: str) -> dict:
    """
    Check if image has real transparency.
    Returns dict with transparency info.
    """
    img = Image.open(img_path)
    
    if img.mode != 'RGBA':
        return {
            'has_alpha': False,
            'transparent_corners': False,
            'avg_corner_alpha': 255
        }
    
    img_array = np.array(img)
    alpha = img_array[:, :, 3]
    
    # Check corners (10x10 pixels each)
    corner_size = 10
    corners = [
        alpha[:corner_size, :corner_size],  # Top-left
        alpha[:corner_size, -corner_size:],  # Top-right
        alpha[-corner_size:, :corner_size],  # Bottom-left
        alpha[-corner_size:, -corner_size:]  # Bottom-right
    ]
    
    corner_alphas = [c.mean() for c in corners]
    avg_corner_alpha = sum(corner_alphas) / len(corner_alphas)
    
    # Transparent corners = avg alpha < 50
    transparent_corners = avg_corner_alpha < 50
    
    return {
        'has_alpha': True,
        'transparent_corners': transparent_corners,
        'avg_corner_alpha': avg_corner_alpha
    }


def get_image_info(img_path: str) -> dict:
    """Get image metadata and quality metrics."""
    img = Image.open(img_path)
    file_size = os.path.getsize(img_path)
    
    transparency = check_transparency(img_path)
    
    return {
        'path': img_path,
        'name': Path(img_path).name,
        'width': img.width,
        'height': img.height,
        'size_kb': file_size // 1024,
        'format': img.format or Path(img_path).suffix.upper(),
        **transparency,
        'issues': []
    }


def identify_issues(info: dict) -> list:
    """Identify quality issues with a logo."""
    issues = []
    
    # Size check
    if info['size_kb'] > 50:
        issues.append(f"[WARN] Large file: {info['size_kb']}KB (should be <50KB)")
    
    # Dimension check
    if info['width'] != 512 or info['height'] != 512:
        issues.append(f"[WARN] Non-standard size: {info['width']}x{info['height']} (should be 512x512)")
    
    # Transparency check
    if not info['has_alpha']:
        issues.append("[ERROR] No alpha channel (no transparency)")
    elif not info['transparent_corners']:
        issues.append(f"[ERROR] No real transparency (corner alpha: {info['avg_corner_alpha']:.0f})")
    
    return issues


def img_to_base64(img_path: str) -> str:
    """Convert image to base64 data URI."""
    with open(img_path, 'rb') as f:
        data = f.read()
    
    ext = Path(img_path).suffix.lower()
    mime = {
        '.png': 'image/png',
        '.webp': 'image/webp',
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg'
    }.get(ext, 'image/png')
    
    return f"data:{mime};base64,{base64.b64encode(data).decode()}"


def generate_html_report(logos: list, output_path: str):
    """Generate HTML validation report."""
    
    # Count issues
    total = len(logos)
    with_issues = sum(1 for l in logos if l['issues'])
    no_transparency = sum(1 for l in logos if not l['transparent_corners'])
    oversized = sum(1 for l in logos if l['size_kb'] > 50)
    
    html = f"""<!DOCTYPE html>
<html>
<head>
    <title>Logo Validation Report</title>
    <style>
        body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 20px; background: #f5f5f5; }}
        h1 {{ color: #333; }}
        .summary {{ background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }}
        .summary-item {{ display: inline-block; margin-right: 30px; }}
        .summary-item .value {{ font-size: 24px; font-weight: bold; }}
        .summary-item .label {{ color: #666; font-size: 14px; }}
        .ok {{ color: #22c55e; }}
        .warning {{ color: #f59e0b; }}
        .error {{ color: #ef4444; }}
        .grid {{ display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }}
        .card {{ background: white; border-radius: 12px; padding: 16px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }}
        .card.has-issues {{ border: 2px solid #ef4444; }}
        .card-header {{ font-weight: bold; margin-bottom: 12px; font-size: 14px; word-break: break-all; }}
        .preview-row {{ display: flex; gap: 8px; margin-bottom: 12px; }}
        .preview {{ width: 100px; height: 100px; display: flex; align-items: center; justify-content: center; border-radius: 8px; }}
        .preview img {{ max-width: 80px; max-height: 80px; }}
        .preview.light {{ background: #f5f5f5; border: 1px solid #ddd; }}
        .preview.dark {{ background: #1a1a1a; }}
        .meta {{ font-size: 12px; color: #666; margin-bottom: 8px; }}
        .issues {{ font-size: 12px; }}
        .issues li {{ margin: 4px 0; }}
    </style>
</head>
<body>
    <h1>🖼️ Logo Validation Report</h1>
    
    <div class="summary">
        <div class="summary-item">
            <div class="value">{total}</div>
            <div class="label">Total Logos</div>
        </div>
        <div class="summary-item">
            <div class="value {'ok' if with_issues == 0 else 'error'}">{total - with_issues}</div>
            <div class="label">Passing</div>
        </div>
        <div class="summary-item">
            <div class="value {'ok' if no_transparency == 0 else 'error'}">{no_transparency}</div>
            <div class="label">No Transparency</div>
        </div>
        <div class="summary-item">
            <div class="value {'ok' if oversized == 0 else 'warning'}">{oversized}</div>
            <div class="label">Oversized (>50KB)</div>
        </div>
    </div>
    
    <div class="grid">
"""
    
    # Sort: issues first
    logos_sorted = sorted(logos, key=lambda x: (not x['issues'], x['name']))
    
    for logo in logos_sorted:
        has_issues = bool(logo['issues'])
        card_class = 'card has-issues' if has_issues else 'card'
        
        try:
            img_data = img_to_base64(logo['path'])
        except:
            img_data = ''
        
        issues_html = ''
        if logo['issues']:
            issues_html = '<ul class="issues">' + ''.join(f'<li>{i}</li>' for i in logo['issues']) + '</ul>'
        
        html += f"""
        <div class="{card_class}">
            <div class="card-header">{logo['name']}</div>
            <div class="preview-row">
                <div class="preview light">
                    <img src="{img_data}" alt="{logo['name']}">
                </div>
                <div class="preview dark">
                    <img src="{img_data}" alt="{logo['name']}">
                </div>
            </div>
            <div class="meta">
                {logo['width']}x{logo['height']} • {logo['size_kb']}KB • {logo['format']}
            </div>
            {issues_html}
        </div>
"""
    
    html += """
    </div>
</body>
</html>
"""
    
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(html)
    
    print(f"[OK] Report saved to: {output_path}")


def main():
    parser = argparse.ArgumentParser(description='Validate radio station logos')
    parser.add_argument('--input-dir', '-d', required=True, help='Directory of logos')
    parser.add_argument('--output', '-o', default='validation_report.html', help='Output HTML file')
    
    args = parser.parse_args()
    
    input_path = Path(args.input_dir)
    
    # Find logos
    patterns = ['logo_*.png', 'logo_*.webp']
    logo_files = []
    for pattern in patterns:
        logo_files.extend(input_path.glob(pattern))
    
    if not logo_files:
        print(f"No logo files found in {args.input_dir}")
        return
    
    print(f"Found {len(logo_files)} logos to validate")
    
    logos = []
    for logo_file in sorted(logo_files):
        info = get_image_info(str(logo_file))
        info['issues'] = identify_issues(info)
        logos.append(info)
        
        status = "[OK]" if not info['issues'] else "[X]"
        print(f"{status} {logo_file.name}")
    
    generate_html_report(logos, args.output)
    
    # Summary
    with_issues = sum(1 for l in logos if l['issues'])
    print(f"\n{'='*40}")
    print(f"Total: {len(logos)} logos")
    print(f"Passing: {len(logos) - with_issues}")
    print(f"With issues: {with_issues}")


if __name__ == '__main__':
    main()
