# Kiyori

**[中文版本](README.md) | [English Version](README_EN.md)**

> [!IMPORTANT]
> Primary repository: <https://github.com/Kiyori-CN/kiyori-android>

Kiyori is an Android browser-first app with built-in media tools.

The current direction prioritizes the in-app browser, page navigation, search, bookmarks, browsing history, video sniffing, remote playback, WebDAV access, and Bilibili-related capabilities. The project still keeps its `libmpv` playback layer to handle local files, sniffed media links, and remote streams through a unified playback entry.

## Positioning

Kiyori is no longer documented as a standalone local video player. It is now positioned as a hybrid Android app with a browser core and optional media capabilities:

- Built-in browser as the main entry point
- Web video sniffing and remote media playback
- WebDAV access and remote file opening
- Local media browsing and playback history
- Bilibili login, parsing, download, danmaku, and related integrations
- Anime4K and other playback enhancements kept as media-side features

## Documentation

- [Development Guide](docs/development_guide.md)
- [Repository Layout](docs/repository_layout.md)
- [Feature Overview](docs/features.md)
- [Internal Browser Design](docs/internal_browser_design.md)
- [Architecture Analysis](docs/project_architecture_analysis.md)
- [Remote URL Playback Design](docs/remote_url_playback_design.md)

## Feedback

- Issues: <https://github.com/Kiyori-CN/kiyori-android/issues>
- Repository: <https://github.com/Kiyori-CN/kiyori-android>
- Organization: <https://github.com/Kiyori-CN>
