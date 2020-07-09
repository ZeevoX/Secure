# Secure
*An encrypted local password manager for Android*

## FAQ

### Why use a password manager?
  > A password manager stores all of your passwords in a single account. The master password to your safe is the only password you’ll ever need to remember.

### Is it secure?
  > As the name would suggest, I have done my best to make sure your passwords are encrypted well. We use `MD5 PKCS #5` encryption at the moment, but we plan on including support for AES 256-bit encryption soon.
  
### How do I know that you aren't sending off my passwords to your server?
  > This application does its best at staying transparent and clear as to what is happening to your passwords. If you do decide to use the Google Drive™ backup feature of this app, your passwords will be backed up in encrypted form to Google Drive only.
  > 
  > And they won't be backed up because the code automagically decided it was time. Your passwords will only ever be backed up if you press the backup button. That way, your passwords stay on your device and are only backed up when you want them to be.
  
### What about my other questions?
  > Please feel free to [contact me](mailto:zeevox.dev@gmail.com), [create a new Github issue](https://github.com/ZeevoX/Secure/issues/new), or leave feedback using the *send feedback* button in the app.
  
## Installation

#### For most users
You can download a pre-compiled APK of the app from [the releases page](https://github.com/ZeevoX/Secure/releases/latest).

#### For experimenters
You can sign up to [my Telegram channel](https://t.me/ZeevoX_CI) where the latest experimental releases will be published.

#### For adventurers

This project uses [Android Studio 4.0](https://developer.android.com/studio), where, once installed, you can use the **Import from VCS** feature. Android Studio will clone the repository and attempt to build it. The build will fail.

To fix this, we must create a new JKS keystore and key in the root directory of the project. We will use this to sign your build of the app. This is necessary for the Google Drive backup feature to work, which verifies the APK's signature.

Next, you will need to create a `keystore.properties` file in the root directory of the project and replace the placeholders with the passwords to the keystore and key you just created.

```
storeFile ../<keystore filename>.jks
storePassword <keystore password>
keyAlias = <key name>
keyPassword <key password>
```

Now you can follow Google's guide on [configuring a Google API Console project](https://developers.google.com/identity/sign-in/android/start-integrating#configure_a_project)

Once you have integrated your `credentials.json` file, try building again, and it should build successfully. If it does not, feel free to take a look in the [issue tracker](https://github.com/ZeevoX/Secure/issues) and perhaps create a new issue.

## Acknowledgements

* [**eypher**](https://github.com/eypher) for the password encryption/decryption algorithms

## License

```
 Copyright (C) 2019 Timothy "ZeevoX" Langer
 
 Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 except in compliance with the License. You may obtain a copy of the License at
 
        http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied. See the License for the specific language governing
 permissions and limitations under the License.
 ```
