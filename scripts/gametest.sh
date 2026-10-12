#!/usr/bin/env bash
# Build the mod and run the client game test on a headless Linux machine (GitHub Actions).
# Same setup as Fabric API's own CI: Loom provides the virtual display when CI is set, and
# mesa-vulkan-drivers (installed by the workflow) gives a software renderer.
# The timeout stops a client that fails to start from hanging the job forever.
set -o pipefail
cd "$(dirname "$0")/.."
export CI="${CI:-true}"
timeout --kill-after=30s 20m ./gradlew build runClientGameTest --no-daemon --console=plain --stacktrace
