# VIRA

VIRA is a Meta Quest prototype with a fixed screen, an offline anime catalog, a movable folding Anime Card, and a local Library. The current app includes hand and controller interaction, local status and rating, and playback of a bundled demo clip.

## Build

Open [app/](app/) in Android Studio. The project uses JDK 17, Android SDK Platform 34, Build Tools 35.0.0, and Meta Spatial Editor. Run these commands from `app/` on Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The debug APK is written to `app/app/build/outputs/apk/debug/app-debug.apk`. For interaction controls and the demo playback flow, see the [app guide](app/README.md).

## Repository layout

- [app/](app/) contains VIRA source, scene files, assets, database schemas, and tests.
- [assets/](assets/) contains the editable room blockout and asset register.
- [docs/](docs/) contains product, design, testing, and technical notes.
- [StarterSample/](StarterSample/) is the original coursework baseline from Meta Spatial SDK Samples at `f233e2327b95f9871b75bdba867d6fdd726f07cc`.

The active app uses Meta Spatial SDK 0.15.0 and StarterSample revision `822f4d1ec967d4975ae8db386502b60cda5218c0`. See [upstream versions](config/upstream.lock.json) and [licenses and notices](NOTICE.md).
