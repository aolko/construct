# Construct Classic `.cap` compatibility

## Confirmed

- The original Construct Classic 1.7 file header is `Construct1.7` plus a null byte.
- The Android native layer validates this header and preserves every original byte.
- Opening a `.cap` through Android SAF copies the complete file into app-private storage.
- The app does not silently change file names, project versions or raw `.cap` data.
- Unknown plugin and object payload bytes remain in the preserved `.cap` copy.

## Current limitation

The original Windows implementation stores projects through MFC `CArchive`. That format includes
type-class records, native 32-bit values and plugin-specific binary records. A portable CArchive
implementation is not equivalent to ZIP, JSON or XML. The Android editor therefore does not claim
full semantic editing of arbitrary original `.cap` files yet.

To preserve compatibility honestly:

1. `.cap` files are recognized and raw bytes are preserved.
2. Android-native project data is stored as JSON in `.ccproj` documents.
3. Imported `.cap` files are not overwritten.
4. Plugin payloads that lack Android metadata are shown as unsupported, never fabricated.
5. A future phase should implement the MFC CArchive stream parser and class serializers using the
   original `Serialize` methods under `IDE/` and `Runtime/`.

## Plugin handling

The app exposes plugin metadata in `android/plugins-meta/plugins.json`. Plugins absent from that
file are not considered supported for editing. Their raw data must remain preserved.
