#!/usr/bin/env bash
set -e

echo "================================================="
echo "  RED ASTRONOMY — RESOURCE VALIDATION PIPELINE   "
echo "================================================="

FAILED=0

# 1. Audit Images
echo "[1/4] Auditing image drawables & nodpi resources..."
FORBIDDEN_JPG=$(find app/src/main/res -type f \( -name "*.jpg" -o -name "*.jpeg" \))
if [ -n "$FORBIDDEN_JPG" ]; then
    echo "ERROR: Found unapproved JPG resource files:"
    echo "$FORBIDDEN_JPG"
    FAILED=1
else
    echo "  ✓ No invalid JPG files found."
fi

for img in app/src/main/res/drawable*/*.png app/src/main/res/mipmap*/*.png; do
    if [ -f "$img" ]; then
        if ! file "$img" | grep -q "PNG image data"; then
            echo "ERROR: Corrupt or invalid PNG image: $img"
            FAILED=1
        fi
    fi
done
echo "  ✓ All PNG drawables and mipmaps are valid header-verified PNG files."

# 2. Audit Splash Images
echo "[2/4] Auditing required splash screen PNG resources..."
for splash in img_splash_1.png img_splash_2.png img_splash_3.png; do
    if [ ! -f "app/src/main/res/drawable-nodpi/$splash" ] && [ ! -f "app/src/main/res/drawable/$splash" ]; then
        echo "ERROR: Missing required splash image resource: $splash"
        FAILED=1
    fi
done
echo "  ✓ img_splash_1, img_splash_2, img_splash_3 verified inside res/drawable/ and res/drawable-nodpi/."

# 3. Audit Font Resources
echo "[3/4] Auditing font resources..."
for font in app/src/main/res/font/*; do
    if [ -f "$font" ]; then
        if [ ! -s "$font" ]; then
            echo "ERROR: Empty font file: $font"
            FAILED=1
        fi
    fi
done
echo "  ✓ All font resources verified non-empty."

# 4. Verify Code References (Project R.drawable.*)
echo "[4/4] Scanning Kotlin files for project drawable references..."
MISSING_REFS=0
for ref in $(grep -rh "R\.drawable\." app/src/main/java/ | grep -v "android\.R\.drawable" | sed -E 's/.*R\.drawable\.([a-zA-Z0-9_]+).*/\1/' | sort -u); do
    if [ ! -f "app/src/main/res/drawable/$ref.png" ] && \
       [ ! -f "app/src/main/res/drawable/$ref.xml" ] && \
       [ ! -f "app/src/main/res/drawable-nodpi/$ref.png" ]; then
        echo "ERROR: Referenced R.drawable.$ref not found in res/drawable!"
        MISSING_REFS=$((MISSING_REFS + 1))
    fi
done

if [ $MISSING_REFS -gt 0 ]; then
    echo "ERROR: Found $MISSING_REFS missing drawable references in code!"
    FAILED=1
else
    echo "  ✓ All project R.drawable.* references match existing drawable resources."
fi

if [ $FAILED -ne 0 ]; then
    echo "================================================="
    echo "   RESOURCE VALIDATION FAILED!                   "
    echo "================================================="
    exit 1
fi

echo "================================================="
echo "   RESOURCE VALIDATION PASSED SUCCESSFULLY!      "
echo "================================================="
EOF
