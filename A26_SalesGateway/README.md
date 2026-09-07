# NamiWin A26 Sales Gateway v0.1

Android companion app for Galaxy A26.

Capabilities:
- Stores CRM server URL.
- Polls CRM for queued mobile dial commands.
- Places cellular calls through the SIM using TelecomManager.
- Can request the Android default Dialer role.
- Provides Hold/Resume when the current carrier call exposes CAPABILITY_HOLD.
- Shows handoff alerts and a local “Take over” control.
- Reports command results to CRM.

Important platform limitation:
A normal third-party Android app cannot freely capture/inject both sides of carrier-call audio. Full autonomous AI speech and reliable attended transfer therefore require a GSM/LTE gateway or SIP bridge. This app is the A26 cellular control/companion layer.
