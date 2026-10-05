# surf-rift

**RiftRoyale** – ein Battle-Royale-Spielmodus auf Minestom. Jeder Server spielt genau eine Runde:
Wartelobby → Countdown → Boarding-Animation → Phantomflug → Fallschirm → Kampf in der schrumpfenden Zone → Sieg → Kick.

## Module

| Modul | Inhalt |
|---|---|
| `surf-rift-server` | Der Minestom-Server inkl. komplettem Spielmodus (kein Plugin) |
| `surf-rift-host` | Microservice, nimmt Rundenstatistiken per RabbitMQ-RPC entgegen und speichert sie in der Datenbank |
| `surf-rift-common` | Gemeinsame Modelle und der RPC-Vertrag `RiftStatsService` |
| `surf-rift-redis` | Gemeinsame Redis-Basis |
| `surf-rift-resourcepack` | Ressourcenpaket-Quellen (HUD, Effekte, Sounds, Shader) |
| `tools/resourcepack` | Python-Generatoren für Texturen, Fonts und Sounds des Ressourcenpakets |

## Umgebungsvariablen (Server)

Der Server hat keine Konfigurationsdateien. Alles Einstellbare kommt über Umgebungsvariablen.

| Variable | Standard | Beschreibung |
|---|---|---|
| `RIFT_AUTH_SECRET` | – | Velocity-Forwarding-Secret. Ohne Wert läuft der Server im Online-Mode |
| `RIFT_BIND_HOST` | `0.0.0.0` | Bind-Adresse |
| `RIFT_PORT` | `SERVER_PORT` bzw. `25565` | Port (Pterodactyl setzt `SERVER_PORT`) |
| `RIFT_MAX_PLAYERS` | `24` | Maximale Spielerzahl |
| `RIFT_MIN_PLAYERS` | `2` | Mindestspielerzahl für den Start |
| `RIFT_TEAM_SIZE` | `1` | Teamgröße, `1` = ohne Teams |
| `RIFT_MAP` | `test` | ID der Karte (siehe `RiftMaps`) |
| `RIFT_MAPS_DIRECTORY` | `maps` | Ordner mit den Anvil-Welten |
| `RIFT_LOBBY_WORLD` | `lobby` | Ordnername der Wartelobby-Welt |
| `RIFT_LOBBY_SPAWN` | `0.5,65,0.5,0,0` | Spawn der Wartelobby (`x,y,z[,yaw,pitch]`) |
| `RIFT_BOARDING_POSITION` | Lobby-Spawn | Position der Boarding-Animation |
| `RIFT_HOST` | – | UUID des Hosts. Ohne Wert wird der erste Spieler Host |
| `RIFT_GAME_ID` | zufällig | ID der Runde für die Statistiken |
| `RIFT_GAME_DURATION_SECONDS` | `900` | Standard-Spieldauer (vom Host änderbar) |
| `RIFT_START_COUNTDOWN_SECONDS` | `10` | Countdown von `/start` ohne Angabe |
| `RIFT_END_DELAY_SECONDS` | `10` | Zeit zwischen Sieg und Kick |
| `RIFT_SHUTDOWN_AFTER_GAME` | `true` | Server nach dem Kick beenden |
| `RIFT_STATS_ENABLED` | `true` | Statistiken an den Rift-Host senden |
| `RIFT_VIEW_DISTANCE` | `10` | Chunk-Sichtweite |
| `RIFT_TICK_THREADS` | `1` | Tick-Threads |
| `RIFT_PRELOAD_CHUNKS` | `true` | Alle Chunks innerhalb der Zone beim Start laden |

RabbitMQ wird wie gewohnt über `SURF_RABBITMQ_*` konfiguriert.

Fehlt der Kartenordner `maps/<worldFolder>`, wird für die Testkarte und die Lobby eine Welt generiert.
Nichts wird gespeichert: Welten werden nur gelesen, Modell- und RabbitMQ-Daten landen in temporären Ordnern.

## Spielablauf

- **Wartelobby**: Der Host startet mit `/start` (10 s) oder `/start <sekunden>`, bricht mit `/start cancel` ab und passt
  über `/settings` bzw. das Hotbar-Item Spieldauer, Loot-Menge, Fallschaden, Heilung bei Kill und Fluggeschwindigkeit an.
  Im Teammodus wählt jeder über `/team` bzw. das Banner ein Team (`T<id>` in Tab und Chat).
- **Boarding**: Schwarzblende, jeder sieht sich selbst in der Mitte und bis zu 5 andere Spieler (Teammates zuerst) um sich herum,
  die auf Phantome steigen und aufsteigen. Danach folgt eine zweite Schwarzblende.
- **Phantomflug**: Jeder fliegt auf seinem eigenen Phantom über die Karte und sieht nur sich selbst. `SHIFT` springt ab,
  am Ende der Route wird jeder abgeworfen. Abgesprungene Spieler sind für alle sichtbar.
- **Fallschirm**: Animierter Spieler (WSEE-Emote) und animierter Fallschirm. `WASD` steuert, Maus dreht, `SHIFT` sinkt schneller.
- **Kampf**: Vanilla-Fässer in vier Stufen (Vorrat, Selten, Episch, Legendär) mit einmaligem Loot. Die Zone schrumpft in Phasen
  bis zur Kartenmitte; in den letzten 20 % schließt sich zusätzlich die vertikale Zone.
- **Punkte**: Kill = 10, Assist = 5 (Schaden innerhalb der letzten 15 s). Teammates können sich nicht verletzen.
- **Zuschauer**: Eliminierte fliegen unsichtbar für Lebende herum, stehen ausgegraut unten im Tab und chatten nur untereinander.
- **Ende**: Sieg-Banner, nach 10 s Schwarzblende, Kick aller Spieler, Statistiken gehen an den Rift-Host.

## Ressourcenpaket

```
./gradlew :surf-rift-server:generateResourcePack
```

Erzeugt `surf-rift-server/build/resourcepack/rift-resourcepack.zip` samt SHA-1. Der Server verteilt das Paket nicht,
es wird lokal im Client angewendet (Ordner `build/resourcepack/pack` oder die ZIP in `resourcepacks/` legen). Der Task kopiert
`surf-rift-resourcepack/` und erzeugt die Modell-Assets der WSEE-Modelle (`src/main/resources/models/*.bbmodel`).

Die Texturen, Fonts und Sounds in `surf-rift-resourcepack/` stammen aus den Generatoren in `tools/resourcepack`
(Python 3 mit `pillow`, `numpy`, `scipy`, `soundfile`):

```
cd tools/resourcepack
python generate_all.py
```

Die Shader für den Spieler-Emote (`entity.vsh`/`entity.fsh`) und die Spielerkopf-Modelle stammen aus
[WorldSeedEntityEngine](https://github.com/AtlasEngineCa/WorldSeedEntityEngine) (Apache-2.0).
