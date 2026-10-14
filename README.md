# DocuView Android

A lightweight, offline-first Android document viewer and PDF utility app.

## Features in this starter release
- Native PDF reader using Android `PdfRenderer` with previous/next page controls.
- Local previews for DOCX, XLSX, PPTX, TXT, CSV and Markdown. Office previews extract text/cell values; they are not full-fidelity Office renderers.
- Convert one or more selected images into a PDF.
- Convert typed/pasted text into a PDF.
- Uses Android's system document picker; no broad storage permission and no upload of files to a server.

## Build
Requires Android SDK Platform 35 and Gradle 8.9 / JDK 17.

```bash
gradle :app:bundleRelease
```

The bundle is created at `app/build/outputs/bundle/release/app-release.aab`.

## Google Play upload signing
To make a Play Console upload-ready signed AAB, create an upload keystore and configure the following GitHub Actions repository secrets:

- `ANDROID_KEYSTORE_BASE64`: base64 encoding of your `.jks` upload keystore
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Then run **Actions → Build Android App Bundle**. Download the `DocuView-release-bundle` artifact. Keep the keystore and passwords private and backed up. Do not change/loss the upload key without following Play Console's upload-key reset process.

If no signing secrets are configured, the workflow may produce an unsigned release bundle, which is not ready for Play Console upload.

## Important limitations
- Legacy `.doc`, `.xls`, and `.ppt` are not parsed locally. Save these as `.docx`, `.xlsx`, or `.pptx` first.
- Office files are previewed as extracted text/data, not pixel-perfect pages.
- Conversion of Word/Excel/PowerPoint to PDF is not yet included; image-to-PDF and text-to-PDF are implemented. The UI labels this as a future/limited conversion path so it does not falsely claim conversion support.
- Before public release, add a production app icon, privacy policy URL, store listing, screenshots, and test on real devices. Review Play Console's current target API requirements before publishing.
