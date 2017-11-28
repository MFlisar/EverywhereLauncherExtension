# EverywhereLauncherExtensions
This is an extension app for [Everywhere Launcher](https://play.google.com/store/apps/details?id=com.appindustry.everywherelauncher)

![Icon](https://lh3.googleusercontent.com/FcsZLjxsQpCQxXB9jGqcxpQzNXiglVY5MmTNSA2tZncv5nHdlWdAx5dHxuyPE2kaAw=w300)

## How to install it?
1. Enable *Unknown Sources* in the android apps settings
2. Download the extension app: ...
3. Install it by simply executing it

## Why is this app not in the play store?
This app is the result of google's change for using the **ACCESSIBILITY SERVICE**. In the past google did not restrict the usage to any use case (not even in their descriptions), but now they do. They only allow the usage for apps that help disabled people to use the phone. Google does supsend all apps that use this service if they do not use it for their new defined purpose.

## Why is this extension app necessary?
1. Everywhere Launcher allows to execute phone button clicks (i.e. the recent apps button, back button) and uses the **ACCESSIBILITY SERVICE** for this functionality.
2. Everywhere Launcher allows to disable itself in certain apps (the blacklisted apps) automatically. Therefore the app needs to get notified whenever the app in the foreground changes. Again the **ACCESSIBILITY SERVICE** is used for this.

## Why open source?
Because the **ACCESSIBILITY SERVICE** does allow dangerous things (that's why google is suspending it's usage for any other usage than their defined one). By open sourcing the app everyone can check that this extension app is 100% safe and does not misuse the service. Additionally anyone not trusting my compiled apk can check out the source code, compile the app and install the own compiled app (total control about what is compiled and get's installed on your phone). It can't be safer.
