# Mythic Maces

Twenty cinematic PvP maces for modern Paper survival, Skyblock and Oneblock servers.

Every mace is a real Mace item marked with persistent data. On hit it triggers a unique combat effect with particles, sound and an internal cooldown: freeze, lightning, void pull, fire, poison, knockback, gravity, sonic shockwave, healing, teleport, area damage and more.

## Give command

`/mace give <player> <mace>`

Examples: `/mace give Semm frostbind`, `/mace give Semm stasis`, `/mace give Semm celestial`.

Tab completion lists all 20 IDs. Requires `mythicmaces.admin` (OP by default).

## Build

Java 21 and modern Paper are required:

```bash
mvn clean package
```

Copy the generated jar to your server's `plugins/` directory and restart.