# SAKChat
When I Play Minecraft I find the chat stuck in 2011: extremely basic compared to the complexity of the game, so I decided to develop this mod to add the features that I feel are missing in Minecraft Vanilla Chat,
SAKChat is a modular, lightweight client-side Fabric mod built to filter, personalize and automate chat.
It's fully configurable using the in-game GUI (using ModMenu or `/sakchat gui`) or by editing the JSON files directly. _(Just a heads-up: if you break the JSON syntax, the mod will safely reset the file to default, so be careful!)_.
Hope you like it! :D
## Installation

1. Install Fabric Loader
2. Download Fabric API and Cloth Config API and drop them into your mods folder
3. Add the SAKChat .jar in there too
4. Launch the game!

Since it's 100% client-side, it works everywhere: singleplayer, multiplayer and Realms. Servers won't even know you're running it.

#### Supported Minecraft versions
| Minecraft version | SAKChat version |
|-------------------|-----------------|
| 26.1.x            | 1.0.0           |

## Modules
### Triggers
Reactions to specific chat keywords or regular expressions.

* Supports regex/text, case-sensitive/insensitive
* Actions: Sound cues, word highlighting, toast notifications and hotbar HUD popups
* Cooldowns to prevent chat spam
* Optional auto-run shortcuts on match

### Blacklist
The Blacklist replaces text or patterns with custom strings (default: ****).

* Regex support, case-sensitive/insensitive (e.g., `(?i)badword|anotherone`)

### Shortcuts
Sends commands or text instantly.

* Arguments positions are defined via % (extra arguments append to the end)
* Supports keybinding

### Placeholders
Gets data from the game in real time and injects it in messages or shortcuts.

* Player Data: Health, hunger, armor, exact coordinates and facing direction
* Technical Info: Server IP, FPS, OS and RAM usage
* Shortcuts integration

### Privacy & QoL Modules

* Anti-Doxxing & Anti-Leak: Blocks emails, IPs, phone numbers and coordinates in incoming/outgoing text
* Streamer Mode: Local client-side username masking
* Chat Recorder: Logs raw, unfiltered chat history by world/server into .minecraft/sakchat/records/
* Mute Chat: Toggles chat visibility (commands remain available)

## Configuration Files
Everything lives inside .minecraft/config/sakchat/:

* `triggers.json`
* `blacklist.json`
* `shortcuts.json`
* `status.json`

## How Messages are Processed
**Incoming Text:**

`Chat Packet -> Record Engine -> Streamer Mode -> Anti-Doxxing -> Blacklist -> Trigger Engine -> Actions -> Mute Chat -> Chat`

**Outgoing Text:**

`Chat -> Mute Chat* -> Anti-Leak -> Placeholder Engine -> Shortcut -> Chat Packet`

*Note: Mute chat prevents sending messages if enabled but allows sending commands.

> [!NOTE]
> All regex patterns compile asynchronously when the game loads, so you won't experience any gameplay stutters.

## On the Roadmap
Im currently working on implementing a **streamer integration** (Live chat bridges for Twitch & YouTube with configurable mutes and chat type, e.g. "Only Subscribers"), an **Auto-Translator** (auto translation of chat), a **Player Mute** (to completely silence annoying players, similar as Mute Chat does) and **Accessibility & Fonts** (use Unicode char families to render chat and send messages, monochrome chat, and colorblind filters).


## Essential Commands

* `/sakchat help` — Shows the list of commands
* `/sakchat gui` — Opens the configuration settings directly
* `/sakchat save` — Forces the current settings to save to your disk
* `/sakchat reload` — Forces the mod to re-import files from your disk (only available in dev mode)
* `/sc <id> [args...]` — Runs a custom shortcut instantly

## Examples

* Quick Coordinates:
  ``` json
  shortcut
    {
    "id": "coords",
    "shortcut": "I'm at {x} {y} {z}",
    "isCommand": false,
    "placeholding": true,
    "placeholder": "xyz",
    "keyBinding": false,
    "key": "key.keyboard.unknown"
    }
  ```
* Auto-Responder:
  ``` json
  shortcut
    {
    "id": "love-mc",
    "shortcut": "Minecraft is awesome!",
    "isCommand": false,
    "placeholding": false,
    "placeholder": "",
    "keyBinding": false,
    "key": "key.keyboard.unknown"
    }
  trigger
    {
    "id": "love-mc",
    "enabled": true,
    "pattern": "Minecraft",
    "regex": false,
    "caseSensitive": false,
    "cooldownMs": 3000,
    "actions": {
      "sound": false,
      "hudPopup": false,
      "highlight": false,
      "toast": false,
      "shortcut": true
       },
    "appearance": {
      "highlightColor": "#129A9A",
      "backgroundColor": "#00000000",
      "bold": true
       },
    "sound": {
      "id": "minecraft:block.anvil.land",
      "volume": 1.0,
      "pitch": 2.0
       },
    "shortcut": {
      "id": "love-mc"
       }
    },
  ```
* Integrate a shortcut in a message:
  ``` json
  shortcut
    {
    "id": "welcome_message",
    "shortcut": "Hello, welcome to the server!",
    "isCommand": false,
    "placeholding": true,
    "placeholder": "wm",
    "keyBinding": false,
    "key": "key.keyboard.unknown"
    }
  ```
  `{wm} I hope you enjoy!` -> Hello, welcome to the server! I hope you enjoy!
* Use arguments in your shortcut:
  ``` json
  shortcut:
    {
    "id": "welcome_message2",
    "shortcut": "Hello %, welcome to the server!",
    "isCommand": false,
    "placeholding": true,
    "placeholder": "wm2",
    "keyBinding": false,
    "key": "key.keyboard.unknown"
    }
  ```
  `/sc welcome_message2 Steve "Hope you enjoy!"` -> Hello Steve, welcome to the server! Hope you enjoy!

  `/sc welcome_message2 "Steve and Alex" enjoy!` -> Hello Steve and Alex, welcome to the server! enjoy!
* Ping: Set up a trigger with your username to receive an alert whenever someone's talking about you
  ``` json
  trigger:
    {
    "id": "ping",
    "enabled": true,
    "pattern": "Steve",
    "regex": false,
    "caseSensitive": false,
    "cooldownMs": 1000,
    "actions": {
      "sound": true,
      "hudPopup": true,
      "highlight": true,
      "toast": true,
      "shortcut": false
       },
    "appearance": {
      "highlightColor": "#129A9A",
      "backgroundColor": "#00000000",
      "bold": true
       },
    "sound": {
      "id": "minecraft:block.anvil.land",
      "volume": 1.0,
      "pitch": 2.0
       },
    "shortcut": {
      "id": ""
       }
    },
  ```
* SMP Logs: Keep the chat recorder active on survival servers to easily look back at old trade deals, coordinates people gave you, or fun conversations.
`/sakchat modules toggle record-chat true`
* Experiment and mix multiple modules together to personalize your chat even more

## Contributing & Issues
If you've found a bug, or have a cool idea for a module, feel free to open an Issue or open a PR (I might not be able to review them fast though)! :)

[License](LICENSE): GPLv3