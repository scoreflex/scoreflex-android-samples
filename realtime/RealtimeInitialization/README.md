RealtimeInitialization sample
===============================

To use this sample you have to:

* Create an account on the [Scoreflex platform](http://developer.scoreflex.com/
  "Scoreflex developer site") (if you don't have one already).
* Create a game -or modify an existing one- (check the **Web** option in the
  list of available platforms).
* Download the [Scoreflex Android
  SDK](https://github.com/scoreflex/scoreflex-android-sdk "Scoreflex Android SDK
  on GitHub") (sample tested with the realtime branch)
* Checkout this sample and setup it to use the Scoreflex Android SDK
* Edit the *MainActivity.java* file and update the `APP_CLIENT_ID` and
  `APP_CLIENT_SECRET` variables with your game's identifiers.

------

The RealtimeInitialization sample focuses on the following Scoreflex SDK features:

* Manage lifecycle of the realtime session:
  - Manage Scoreflex SDK initialization/deinitialization
  - Handle player login/logout events
  - Manage realtime session initialization/deinitialization

Typical use-cases covered by this sample:

* Use Restart/Stop buttons to test initialization/deinitialization of the
  realtime session.
* Use login/logout from the user's profile WebView to test the player changes.
