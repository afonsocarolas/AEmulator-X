#!/bin/bash
# AEmulator X: the other launcher icons (Settings › Appearance › App icon) from docs/assets/icon-x.png, Chris's artwork,
# with ImageMagick 7. Each keeps the white marks of the artwork (the "æ", the "x" and the dashed lines) on its own
# background colour:
#   neon      marks in a magenta → cyan glow on near-black
#   gold      gold marks on deep brown
#   retro     green phosphor marks on black
#   christmas white marks on Christmas red
#   mono      black marks on white
# Writes res/mipmap-*/ic_launcher_<name>_fg.png (same placement as make-icon.sh), and the background colours go in
# res/values/colors.xml as ic_bg_<name>.
set -e
cd "$(dirname "$0")"
RES=../../app/src/main/res
T=$(mktemp -d)
S=224; C=$((S / 2))
magick icon-x.png -resize ${S}x${S} "$T/art.png"
# white marks only, as an alpha mask, faded out like the default icon
magick -size ${S}x${S} xc:black -fill white -draw "circle $C,$C $C,$((C - S * 62 / 100))" -blur 0x4 "$T/fade.png"
magick "$T/art.png" -colorspace gray -level 55%,92% "$T/white.png"
magick "$T/white.png" "$T/fade.png" -compose Multiply -composite "$T/alpha.png"

paint() { # name, fill image (S×S)
  magick "$2" "$T/alpha.png" -alpha off -compose CopyOpacity -composite "$T/$1.png"
  # neon gets a soft glow under its marks
  if [ "$1" = neon ]; then
    magick "$T/$1.png" \( +clone -background none -shadow 90x4+0+0 \) +swap -background none -layers merge +repage -gravity center -extent ${S}x${S} "$T/$1.png"
  fi
  for d in mdpi:108 hdpi:162 xhdpi:216 xxhdpi:324 xxxhdpi:432; do
    dir=${d%%:*}; px=${d##*:}
    magick -size 432x432 xc:none "$T/$1.png" -gravity center -composite -resize ${px}x${px} "$RES/mipmap-$dir/ic_launcher_$1_fg.png"
  done
}
magick -size ${S}x${S} gradient:'#FF2BD6-#18FFFF' -rotate 45 -gravity center -extent ${S}x${S} "$T/neon_fill.png"
magick -size ${S}x${S} gradient:'#FFE082-#FFA000' "$T/gold_fill.png"
magick -size ${S}x${S} xc:'#39FF14' "$T/retro_fill.png"
magick -size ${S}x${S} xc:white "$T/christmas_fill.png"
magick -size ${S}x${S} xc:'#111111' "$T/mono_fill.png"
for v in neon gold retro christmas mono; do paint $v "$T/${v}_fill.png"; done
rm -rf "$T"
echo done
