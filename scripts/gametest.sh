#!/usr/bin/env bash
# Build the mod and run the client game test on a headless Linux machine.
# The virtual screen needs 24-bit colour: xvfb-run's default (8-bit) has no GLX visual that OpenGL accepts.
# The timeout stops a client that fails to start from hanging the job forever.
set -o pipefail
cd "$(dirname "$0")/.."
timeout --kill-after=30s 20m \
  xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" \
  ./gradlew build runClientGameTest --no-daemon --console=plain
