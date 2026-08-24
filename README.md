# The Last Lantern

A premium-style **single-feature** progression plugin for Paper survival, Skyblock and Oneblock servers.

Every player owns one soul lantern. It remembers real survival play—mining, harvesting, hunting and fishing—then lets the player spend those memories on one temporary ritual at a time.

## Gameplay loop

- A personal lantern is granted automatically on first join.
- Right-click it or use `/lantern` to open its inventory GUI.
- Build four memory types through normal gameplay and level the lantern.
- Spend a selected memory to ignite a three-minute ritual.

| Ritual | Effect |
|---|---|
| Rootsong | Gently advances nearby crops. |
| Deepsight | Marks nearby ores with particles, with no permanent world changes. |
| Moonward | Keeps monsters away inside the ritual radius. |
| Tidecall | Gives swimming players Dolphin's Grace and water breathing. |

## Install

Requires **Java 21** and modern Paper. Run `mvn clean package`, copy the generated jar into `plugins/`, then restart. Configure goals, radius, duration and levels in `plugins/LastLantern/config.yml`.

## Commands

- `/lantern` — open the Lantern UI.
- `/lantern give [player]` — create a replacement lantern (OP only).
