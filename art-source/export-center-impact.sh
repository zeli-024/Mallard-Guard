#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
particle_dir=../src/main/resources/assets/mallardguard/textures/particle/center_impact
mkdir -p "$particle_dir/animation" "$particle_dir/tint"
for i in 0 1 2 3 4 5; do
    convert center-impact.png -crop "64x64+$((i * 64))+0" +repage "PNG32:$particle_dir/animation/frame_$i.png"
    # This is a tint-selection mask, not a blurred sprite edge. The central highlight stays white.
    convert "$particle_dir/animation/frame_$i.png" -alpha extract \
        -fx 'u*max(0,min(1,(hypot(i-32,j-32)-3)/12))' "tint-alpha-$i.png"
    convert -size 64x64 xc:white "tint-alpha-$i.png" -alpha off -compose CopyOpacity -composite \
        "PNG32:$particle_dir/tint/frame_$i.png"
    rm -f -- "tint-alpha-$i.png"
done
