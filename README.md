This is a toy app which is used to investigate a bug on Android 5.0 whereby
signature verification fails when using a security provider other than the
one bundled with Android.

The specific bug was first identified in F-Droid, where we were using the
SpongyCastle library (a fork of BouncyCastle, but in a different namespace).
It was used for the purpose of signing Jar files, however in the process of
registering it as a security provider, it broke Jar verification for the
.jar files downloaded by F-Droid too (for managing the index of available
apps).