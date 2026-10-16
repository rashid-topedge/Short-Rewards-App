# DramaFlix (Android)

Jetpack Compose app that browses the same Next.js API as the `web/` catalog.

## Requirements

- Android Studio Ladybug+ / JDK 17
- Emulator or device
- Next.js API running (`cd web && npm run dev`)

## API base URL

Default (emulator → host machine):

```
http://10.0.2.2:3000/
```

Override in `local.properties`:

```
api.base.url=http://192.168.x.x:3000/
```

## Run

1. Start the web API: `cd ../web && npm run dev`
2. Open `AndroidDramaApp` in Android Studio
3. Sync Gradle → Run on emulator/device

## Features

- Catalog search + cursor **Load more**
- Drama detail (cover, synopsis, tags, episodes)
- HLS playback via Media3 ExoPlayer using `episodes.playUrl` from the API
