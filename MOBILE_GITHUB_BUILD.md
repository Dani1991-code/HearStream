# HearStream APK direkt am Handy bauen lassen

Du brauchst dafür nur die GitHub-App oder github.com im Browser.

## Variante A: Neues GitHub-Repo am Handy

1. GitHub öffnen.
2. Neues Repository erstellen, z. B. `HearStream`.
3. Diese Projekt-ZIP entpacken oder die Dateien hochladen.
4. Wichtig: Die Datei `.github/workflows/build-apk.yml` muss mit hochgeladen werden.
5. In GitHub auf **Actions** gehen.
6. Workflow **Build HearStream APK** öffnen.
7. **Run workflow** drücken.
8. Nach dem grünen Haken den Build öffnen.
9. Unten bei **Artifacts** die Datei **HearStream-APK** herunterladen.
10. ZIP entpacken. Darin liegt `HearStream-debug.apk`.
11. APK auf deinem Android-Handy installieren.

## Hinweise

- Beim Installieren musst du eventuell **Unbekannte Apps installieren** erlauben.
- Android zeigt während laufendem Mikrofonstreaming eine dauerhafte Benachrichtigung an. Das ist notwendig.
- Die Hörgeräte müssen vorher als Bluetooth-/Medienausgabe mit dem Handy verbunden sein.
