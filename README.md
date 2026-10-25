# VannStore Remote Admin — consent-based starter

Includes a Flask API + website dashboard + Android Kotlin starter. This project is designed for devices you own or devices whose users have explicitly agreed to share data.

## Components
- `backend/app.py`: Flask API, SQLite storage, bearer-token authentication.
- `backend/requirements.txt`
- `website/index.html`, `website/style.css`, `website/app.js`: dashboard for registered devices.
- `android/`: Android Studio Kotlin starter that requests runtime location permission and sends location only after the user taps the button.

## Run the backend
```bash
cd backend
python -m venv .venv
# Windows: .venv\Scripts\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
export VANSTORE_API_TOKEN="replace-with-a-long-random-secret"
python app.py
```
On Windows PowerShell:
```powershell
$env:VANSTORE_API_TOKEN="replace-with-a-long-random-secret"
python app.py
```
API listens on `http://127.0.0.1:5000`. For use across the internet, deploy behind HTTPS; do not expose the development server directly.

## Open the website
Serve the `website` directory using a static web server, then open `index.html`. Set the API base URL in the dashboard settings to your backend URL and enter the same bearer token. Do not use an unencrypted public connection.

## Android
Open the `android` folder in Android Studio. The starter uses Google Play Services Location. Set `API_BASE_URL` and `API_TOKEN` in `MainActivity.kt` for your test server. For production, use secure token provisioning and HTTPS; do not hard-code a shared secret in a distributed APK.

The Android starter deliberately does not provide covert camera activation or background surveillance. Camera capture should be initiated visibly by the device user; this starter uses Android's camera app via an explicit user action.
