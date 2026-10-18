![Less Villager Chatter](art/banner.png)

# Less Villager Chatter

A trading hall with twenty villagers is loud. Each one says "hmm" about every 6 seconds, so a busy village never goes quiet. This mod makes villagers and the wandering trader talk less often, and quieter if you want.

It runs on the server. Players join with a normal vanilla client and install nothing.

## What it changes

In vanilla, a villager waits at least 4 seconds after each idle "hmm", then the chance of the next one grows every tick. That averages out to one sound every 6 seconds. The mod makes the wait longer:

| | Default | Vanilla |
|---|---|---|
| Villagers | one "hmm" every ~22 s | ~6 s |
| Wandering trader | one "hmm" every ~10 s | ~6 s |

Longer silences have a catch. Villagers that load at the same moment stay in step and then all "hmm" together. Two settings break that up:

- Random start puts each villager at a random point of its cycle when it loads.
- Variation makes every silence randomly up to 25% shorter or longer. The average does not change.

You can also lower the volume of all villager sounds and all wandering trader sounds, each with its own slider.

Other mobs are left alone. Work sounds and the yes/no sounds while trading still play, at the volume you pick.

## Singleplayer

On NeoForge, open Mods, select Less Villager Chatter and press Config. On Fabric, install [Mod Menu](https://modrinth.com/mod/modmenu) first, then do the same through its Mods screen. Changes apply right away. Without a settings screen the mod works the same and reads its config file.

![Settings screen](art/config-screen.png)

## Servers

Drop the jar into the server's `mods` folder. The settings are in `config/lessvillagerchatter.properties`, which the mod creates on first start. Restart the server after editing it.

```properties
# Ticks of silence after each "hmm" (20 ticks = 1 second)
villagerInterval=400
traderInterval=160
randomStart=true
# Percent, 0 to 90
variation=25
# Percent, 0 to 100
villagerVolume=100
traderVolume=100
# Client only, see below
useOwnVolume=false
```

The average time between sounds is about (interval + 40) / 20 seconds. 160 gives ~10 s, 400 gives ~22 s and 600 gives ~32 s.

The server's volume reaches vanilla clients through the sound packets. The hurt sound is the one exception: the client plays it on its own, so only players who have the mod hear it quieter.

## Your own volume on any server

With the mod on your client, turn on "Use my volume on every server". From then on you hear villagers and traders at your own volume on every server, whether it runs the mod or not.

## Requirements

- Minecraft 26.3
- Fabric: Fabric Loader 0.19 or newer. Fabric API is not needed. Mod Menu is optional, only for the settings screen.
- NeoForge: NeoForge 26.3.0.90 or newer.
- Java 25

## Building from source

```
./gradlew build
./gradlew -p neoforge build
```

The Fabric jar ends up in `build/libs`, the NeoForge jar in `neoforge/build/libs`. Both are built from the same sources.
`./gradlew runClientGameTest` starts a real Fabric client, creates a singleplayer world and checks the mod in it.
`./gradlew -p neoforge runSmokeTest` starts a NeoForge dedicated server, checks the mod and exits.

## License

All rights reserved, with permission to use the mod in modpacks. See [LICENSE.md](LICENSE.md).
