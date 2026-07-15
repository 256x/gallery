# Literal Gallery

See everything. Nothing to configure.

A minimalist local photo and video gallery for Android.

<p>
  <a href="https://github.com/256x/gallery/releases/latest"><img src="https://img.shields.io/github/v/release/256x/gallery?label=GitHub%20Release"></a>&nbsp;<img src="https://img.shields.io/badge/Android-8%2B-blue">&nbsp;<img src="https://img.shields.io/badge/license-MIT-lightgrey">
</p>

[User Guide](./docs/USER_GUIDE.md)

## Features

- Date-grouped thumbnail grid — month header + day label, Google Photos style
- Loads every photo and video on the device, from any app or folder, into one flat timeline — no folder picker, no settings
- Pinch in/out on the grid to change thumbnail size (2–6 columns)
- Tap a thumbnail for full-screen view; swipe left/right for the next/previous photo or video
- Pinch-to-zoom and pan on photos
- Swipe up in the viewer to reveal EXIF info (camera, exposure, ISO, f-number, resolution, size)
- Share directly from the viewer
- No search. No cloud. No tracking.

## Notes

**Everything, always.** Screenshots, camera shots, downloads, app-saved images — anything MediaStore has indexed shows up in its day's slot, regardless of source. There's no folder allow-list to maintain.

## Development

- Kotlin / Jetpack Compose / Media3 (ExoPlayer) / Coil
- Target: Android 8.0+
- Built on `MediaStore`, with a source abstraction that leaves room for a future remote/cloud source without touching the UI or repository layer

This app was built with substantial assistance from [Claude](https://claude.ai) (Anthropic).

## License

MIT
