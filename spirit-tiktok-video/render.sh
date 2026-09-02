#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE="${1:-$ROOT_DIR/assets/erleuchtung.png}"
AUDIO="${2:-$ROOT_DIR/assets/spirit.mp3}"
OUTPUT="${3:-$ROOT_DIR/Spirit_TikTok_1080x1920.mp4}"
FONT="/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
POSTER="${TMPDIR:-/tmp}/spirit-tiktok-poster-$$.png"
trap 'rm -f "$POSTER"' EXIT

ffmpeg -y -i "$IMAGE" -filter_complex "\
  [0:v]split=2[bg][card];\
  [bg]scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,boxblur=32:12,eq=brightness=-0.24:saturation=1.08[blur];\
  [card]scale=1000:1000[cover];\
  [blur][cover]overlay=(W-w)/2:250,\
  drawtext=fontfile='$FONT':text='SPIRIT':fontcolor=white:fontsize=72:x=(w-text_w)/2:y=1350:shadowcolor=black@0.9:shadowx=4:shadowy=4,\
  drawtext=fontfile='$FONT':text='SebA_4.2.0':fontcolor=0xFFD54A:fontsize=38:x=(w-text_w)/2:y=1440:shadowcolor=black@0.9:shadowx=3:shadowy=3,\
  drawtext=fontfile='$FONT':text='Glück liegt im Verstehen, im Mitgefühl und in der Liebe.':fontcolor=white:fontsize=30:x=(w-text_w)/2:y=1740:shadowcolor=black@0.9:shadowx=3:shadowy=3,\
  drawtext=fontfile='$FONT':text='@dj_seba96':fontcolor=0xFFD54A:fontsize=30:x=(w-text_w)/2:y=1810:shadowcolor=black@0.9:shadowx=3:shadowy=3" \
  -frames:v 1 "$POSTER"

ffmpeg -y -loop 1 -framerate 30 -i "$POSTER" -i "$AUDIO" \
  -filter_complex "\
    [1:a]aformat=channel_layouts=stereo,showwaves=s=900x160:mode=cline:rate=30:colors=0xFFD54A|0xFFFFFF,format=rgba,colorchannelmixer=aa=0.72[wave];\
    [0:v][wave]overlay=(W-w)/2:1510:shortest=1,format=yuv420p[v]" \
  -map "[v]" -map 1:a:0 -c:v libx264 -preset veryfast -crf 22 -profile:v high \
  -c:a aac -b:a 256k -ar 48000 -movflags +faststart -shortest "$OUTPUT"

echo "$OUTPUT"
