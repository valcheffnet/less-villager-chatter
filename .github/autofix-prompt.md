You are updating the Fabric mod "Less Villager Chatter" to a new Minecraft release, unattended in GitHub Actions.

The version bump is already done in gradle.properties. The build or the client game test now fails; the output is in
build-log.txt in the repository root. Make it pass again.

What the mod does (keep exactly this behaviour):
- Villagers and wandering traders wait longer between idle "hmm" sounds (mixin on Mob.getAmbientSoundInterval,
  Mob.resetAmbientSoundTime and the Mob constructor), with random start and random variation.
- Server-side volume for villager / trader sounds (mixin on LivingEntity.getSoundVolume).
- Client-side "use my volume" override (mixin on the client SoundEngine volume calculation).
- A settings screen (ChatterConfigScreen) opened through Mod Menu.

Rules:
- Change only files under src/, build.gradle, settings.gradle, gradle.properties and the Gradle wrapper.
  Never touch .github/ or scripts/.
- Adapt to renamed or moved Minecraft classes and methods. Find the new names in the game jars:
  ~/.gradle/caches/fabric-loom/<minecraft version>/ (use unzip -l and javap). Minecraft 26.x ships unobfuscated.
- You may bump loom_version, the Gradle wrapper, fabric_api_version or modmenu_version if the build needs it.
- Keep the game test's checks. Adjust the test only where the game's own API changed, never to hide a failure.
- If a feature truly cannot work on this Minecraft version, disable just that feature, explain why in CHANGELOG.md
  under the newest version, and keep everything else working.

Verify with:
  scripts/gametest.sh
and stop once it passes. Do not commit or push; the workflow does that after re-running the test itself.
