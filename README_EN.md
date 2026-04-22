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

- [Documentation Index](docs/README.md)
- [Development Guide](docs/guides/development.md)
- [Repository Layout](docs/project/repository_layout.md)
- [Codebase Map](docs/project/codebase-map.md)
- [Feature Overview](docs/product/features.md)
- [Internal Browser Design](docs/architecture/browser.md)
- [Architecture Analysis](docs/architecture/project-overview.md)
- [Remote URL Playback Design](docs/architecture/remote-playback.md)

## Feedback

- Issues: <https://github.com/Kiyori-CN/kiyori-android/issues>
- Repository: <https://github.com/Kiyori-CN/kiyori-android>
- Organization: <https://github.com/Kiyori-CN>
