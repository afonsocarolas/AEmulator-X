#!/bin/bash
# AEmulator X: launcher icon layers from docs/assets/icon-x.png (the full artwork by Chris (donkris01), 1024x1024 since
# X33) with ImageMagick 7.
#   foreground: the artwork at 224/432 of the layer, so the "×" (its farthest corner, ~0.55 of the size from the centre)
#               stays inside the 66dp safe circle; a soft circular fade lets the dashed lines melt into the background
#               colour (#FD5751, the artwork's own coral, in res/values/colors.xml ic_bg)
#   monochrome: only the white marks, same placement (Android 13 themed icons)
set -e
cd "$(dirname "$0")"
RES=../../app/src/main/res
T=$(mktemp -d)
S=224; C=$((S / 2))
magick icon-x.png -resize ${S}x${S} "$T/art.png"
# the fade starts just outside the "x" (its outer corner is ~0.52*S from the centre)
magick -size ${S}x${S} xc:black -fill white -draw "circle $C,$C $C,$((C - S * 62 / 100))" -blur 0x4 "$T/mask.png"
magick "$T/art.png" "$T/mask.png" -alpha off -compose CopyOpacity -composite "$T/fg.png"
magick "$T/art.png" -colorspace gray -level 55%,92% "$T/white.png"
magick "$T/white.png" "$T/mask.png" -compose Multiply -composite "$T/monoalpha.png"
magick -size ${S}x${S} xc:white "$T/monoalpha.png" -alpha off -compose CopyOpacity -composite "$T/mono.png"
for d in mdpi:108 hdpi:162 xhdpi:216 xxhdpi:324 xxxhdpi:432; do
  dir=${d%%:*}; px=${d##*:}
  inner=$((S * px / 432))
  magick -size 432x432 xc:none "$T/fg.png" -gravity center -composite -resize ${px}x${px} "$RES/mipmap-$dir/ic_launcher_fg.png"
  magick -size 432x432 xc:none "$T/mono.png" -gravity center -composite -resize ${px}x${px} "$RES/mipmap-$dir/ic_launcher_mono.png"
done
# in-app logo (Settings › About; the screen rounds the corners itself), 512 px
magick icon-x.png -resize 512x512 -strip PNG32:$RES/drawable-nodpi/logo.png
# README logo: the artwork with rounded corners, 256 px
magick icon-x.png -resize 256x256 \( -size 256x256 xc:black -fill white -draw "roundrectangle 0,0 255,255 56,56" \) \
  -alpha off -compose CopyOpacity -composite logo.png
rm -rf "$T"
echo done
