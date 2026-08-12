# mc_bowser mc浏览器

> 
>
>![Minecraft](https://img.shields.io/badge/Minecraft-Java%20Edition-brightgreen)
![Forge](https://img.shields.io/badge/%20NeoForge%20-26.1.2.86%20|%2026.1.2-orange)
![License](https://img.shields.io/badge/License-BSD%20|%20CC%20BY--NC--SA%204.0-blue)
>
> 本项目由人工智能辅助开发，如发现bug请及时反馈

## 构建

- `mc_bowser-1.2.1.jar`: 普通版本：适用于前置mod Rinku 能正确下载其插件 JCEF 的玩家（需要Github能正确联通下载）。
- `mc_bowser-1.2.1-offline.jar`: 附带了JCEF插件的版本，你仍然需要下载前置mod Rinku，但是Rinku无需联网下载下载JCEF，适用于国内及网络不好的玩家。嵌入式压缩包在使用前会通过 SHA-256 校验和 Rinku 的完整清单进行验证。

## 生存模式配方

工作台中间一个黑色羊毛，外围一圈铁锭制作，也可以使用JEI搜索：显示查看配方。

## 使用方法

- 纵向以 2x2 至 16x9 放置且所有方块应为同一朝向。
- 右键点击可切换显示画面和声音的开启/关闭。
- Shift + 右键点击可打开网页操作的UI，可以进行网页操作，当有人使用该页面时他人无法使用。
- 关闭UI后即可返回游戏世界，方块上会持续渲染网页或视频。
- 对于会请求打开新浏览器标签页的链接，会自动重定向到当前显示器中打开，解决诸如 Bilibili 等网站进入视频页时的加载。
- 在公开的 Bilibili 视频页面中，可在控制器里点击 Compat play（兼容播放），将视频下载并转码为 WebM，以便原版 CEF 播放。该功能仅支持播放器有权访问的非 DRM 内容。
- 媒体桥接功能会读取游戏目录下的：config/mc_bowser/media-bridge.properties
- 其中，tools-directory 指向包含 yt-dlp.exe 和 FFmpeg 的文件夹；cache-directory 用于指定转码后媒体文件的缓存目录。
- 系统会缓存最近使用的 3 个兼容视频；当打开第 4 个视频时，会自动按时间顺序删除过往缓存。
- 在服务器环境中，服务器会同步显示器状态、当前 URL/媒体内容以及播放开始时间。每个客户端都会在本地自行渲染并播放内容。同一时间，一个显示器只能由一名玩家持有完整控制器权限。
-
- HTML5 视频说明
- MC Bowser 使用已安装的 Rinku JCEF 运行时所自带的编解码器。在处理 H.264/AAC 等无法直接播放的视频流时，兼容播放功能会在本地将公开视频转换为 WebM/VP9/Opus 格式。
