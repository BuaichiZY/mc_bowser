# mc_bowser mc浏览器

> [🇨🇳 简体中文](README.md) | [🇺🇸 English](README_EN.md)
>
>![Minecraft](https://img.shields.io/badge/Minecraft-Java%20Edition-brightgreen)
![Forge](https://img.shields.io/badge/%20NeoForge%20-26.1.2.86%20|%2026.1.2-orange)
![License](https://img.shields.io/badge/License-BSD%20|%20CC%20BY--NC--SA%204.0-blue)
>
> 本项目由人工智能辅助开发，如发现bug请及时反馈
> 适用于mc java 26.1.2版本，Neoforge版本至少应为26.1.2.86及以上

## 构建

- `mc_bowser-1.2.1.jar`: 普通版本：适用于前置mod Rinku 能正确下载其插件 JCEF 的玩家（需要Github能正确联通下载）。
- `mc_bowser-1.2.1-offline.jar`: 附带了JCEF插件的版本，你仍然需要下载前置mod Rinku，但是Rinku无需联网下载下载JCEF，适用于国内及网络不好的玩家。嵌入式压缩包在使用前会通过 SHA-256 校验和 Rinku 的完整清单进行验证。

## 生存模式配方

工作台中间一个黑色羊毛，外围一圈铁锭制作，也可以使用JEI查看配方。
<img width="1404" height="811" alt="image" src="https://github.com/user-attachments/assets/dc1d99fd-9b3a-45b6-8cc2-632d302ca54a" />


## 使用方法

- 纵向以 2x2 至 16x9 放置且所有方块应为同一朝向。
- 右键点击可切换显示画面和声音的开启/关闭。
- Shift + 右键点击可打开网页操作的UI，可以进行网页操作，当有人使用该页面时他人无法使用。
- 关闭UI后即可返回游戏世界，方块上会持续渲染网页或视频。
- 对于会请求打开新浏览器标签页的链接，会自动重定向到当前显示器中打开，解决诸如 Bilibili 等网站进入视频页时的加载。
- 在 Bilibili 视频详情页中，可在控制器里点击兼容播放,视频会自动转码为 WebM，以便原版 CEF 播放。该功能仅支持非 DRM 内容，番剧、综艺等受限及其他特殊付费内容无法观看。
- 在服务器环境中，服务器会同步显示器状态、当前 URL/媒体内容以及播放开始时间。每个客户端都会在本地自行渲染并播放内容。同一时间，一个显示器只能由一名玩家能打开UI。

<img width="1404" height="813" alt="image" src="https://github.com/user-attachments/assets/644a627b-a6aa-4843-a18a-bf03ba0449ef" />
<img width="1404" height="811" alt="image" src="https://github.com/user-attachments/assets/09852e50-0331-43bc-9e52-aa28d2717e9e" />
