# mc_bowser - Minecraft Browser

> [🇨🇳 简体中文](README.md) | [🇺🇸 English](README_EN.md)
>
>![Minecraft](https://img.shields.io/badge/Minecraft-Java%20Edition-brightgreen)
![Forge](https://img.shields.io/badge/%20NeoForge%20-26.1.2.86%20|%2026.1.2-orange)
![License](https://img.shields.io/badge/License-BSD%20|%20CC%20BY--NC--SA%204.0-blue)

> This project was developed with the assistance of AI. If you encounter any bugs, please report them.
> This mod is compatible with Minecraft Java Edition 26.1.2 and requires NeoForge version 26.1.2.86 or above.

## Builds

- `mc_bowser-1.2.1.jar`: Standard version. Suitable for players whose dependency mod **Rinku** can correctly download its JCEF runtime (requires a stable connection to GitHub for downloads).

- `mc_bowser-1.2.1-offline.jar`: Includes the JCEF runtime package. You still need to install the dependency mod **Rinku**, but Rinku no longer needs to download JCEF online. Recommended for players with limited or unstable network access. The bundled archive will be verified using SHA-256 checksums and Rinku's complete file manifest before use.

## Survival Recipe

- Crafted with one black wool in the center and iron ingots surrounding it in a crafting table.

- You can also search for the recipe using JEI.

<img width="1404" height="811" alt="image" src="https://github.com/user-attachments/assets/5de44a13-9199-434e-b7ac-583dd9fa40f1" />


## Usage

- Place the blocks vertically in a size ranging from **2×2 to 16×9**. All blocks must face the same direction.
- Right-click to toggle the display and sound on/off.
- Shift + right-click opens the web interaction UI, allowing you to control the webpage. Only one player can use the controller for a display at the same time.
- Closing the UI returns you to the world. The webpage or video will continue rendering on the blocks.
- Links that request a new browser tab will be automatically redirected to the current display, fixing loading issues on websites such as Bilibili when opening video pages.
- On public Bilibili video pages, press **Compat Play** in the controller to download and transcode videos into WebM format for playback with the default CEF. This feature only supports non-DRM content that the player is allowed to access.
- The media bridge reads:
from the game directory.

- `tools-directory` specifies the folder containing `yt-dlp.exe` and FFmpeg.
- `cache-directory` specifies the directory used to store converted media files.
- The system caches the three most recently used compatible videos. When a fourth video is opened, the oldest cached file will be removed automatically.
- On servers, the server synchronizes the display state, current URL/media content, and playback start time. Each client renders and plays the content locally. Only one player can hold full controller access for a display at the same time.

<img width="1404" height="811" alt="image" src="https://github.com/user-attachments/assets/7e1d374c-a852-47e8-8c4d-8cc6133e9b95" />
<img width="1404" height="815" alt="image" src="https://github.com/user-attachments/assets/0968593e-a409-48ed-a4d0-d00b3489a883" />


## HTML5 Video Notes

- MC Bowser uses the codecs included with the installed **Rinku JCEF runtime**.

- For video streams that cannot be played directly, such as H.264/AAC streams, the compatibility playback feature locally converts public videos into **WebM/VP9/Opus** format.
