# QStudio macOS app

`.github/workflows/macos.yml` builds `QStudio.app` and a `.dmg` for Apple Silicon (arm64) and Intel (x64)
on GitHub's macOS runners. Each app contains:

- `qstudio.jar`, built from this repo with `ant jarr`
- its own Java 11 runtime, so it doesn't depend on whatever Java is installed on the Mac
- `prqlc`, the [PRQL](https://prql-lang.org) compiler, passed to qStudio with `-Dprqlc.path`

The apps are ad-hoc signed but not notarized, so the first time you open one macOS will warn about an
unidentified developer: open **System Settings → Privacy & Security** and choose **Open Anyway**.

The workflow runs on pushes and pull requests that touch `macos/` or the workflow, can be run by hand
from the Actions tab, and attaches the `.dmg` files to each published release.

## Credits

Based on [create-qstudio-macos-app](https://github.com/richb-hanover/create-qstudio-macos-app)
by Rich Brown, MIT licensed. `QStudio.icns` comes from that project.
