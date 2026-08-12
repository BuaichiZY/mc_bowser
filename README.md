# mc_bowser mc浏览器

> 
>
>![Minecraft](https://img.shields.io/badge/Minecraft-Java%20Edition-brightgreen)
![Forge](https://img.shields.io/badge/%20NeoForge%20-26.1.2.86%20|%2026.1.2-orange)
![License](https://img.shields.io/badge/License-BSD%20|%20CC%20BY--NC--SA%204.0-blue)
>
> This build was developed with the assistance of AI. If you encounter any issues, please feel free to report them. I will do my best to fix them.

## Builds

- `mc_bowser-1.2.1.jar`: small normal build; Rinku downloads its own JCEF runtime.
- `mc_bowser-1.2.1-offline-windows-x64.jar`: Windows x64 offline build containing the exact
  Rinku 3.0.1 JCEF release. Rinku 3.0.1 is still required as a separate mod, but no JCEF network
  download is needed. The embedded archive is verified by SHA-256 and Rinku's full manifest before use.

## Survival recipe

Craft one Browser Display Panel with black wool in the center and eight iron ingots around it.

## In-world browser display

- Build a rectangular 2x2 to 16x9 panel. Every panel must face the same direction.
- Right-click toggles the display and sound on/off without capturing the camera.
- Shift + right-click requests the exclusive address bar and full keyboard/mouse controller.
- Close the controller to return to the world; the same browser continues rendering and playing on the blocks.
- Right-click the front surface to click the corresponding point on the web page without opening another UI.
- Links that request a new browser tab are redirected into the current display, which is required by sites such as Bilibili.
- On a public Bilibili video page, press **Compat play** in the controller to download and transcode it to WebM for stock CEF. This supports non-DRM content the player is allowed to access.

The media bridge reads `config/mc_bowser/media-bridge.properties` from the game directory. `tools-directory`
points at the folder containing `yt-dlp.exe` and FFmpeg; `cache-directory` controls converted media.
The three most recently used compatible videos are cached; opening a fourth evicts the oldest one.

On servers, the server synchronizes the display state, current URL/media and playback start time. Each client
renders and plays the content locally. Only one player can hold the full controller for a display at a time.

### HTML5 video note

MC Bowser uses the codecs shipped by the installed Rinku JCEF runtime. The official runtime does not decode
Bilibili's H.264/AAC streams, so compatibility playback converts public videos to WebM/VP9/Opus locally.
