# AutomusicTagFixer
What does it do?
- From every file you have in your smartphone, it generates an audio fingerprint, then connects to a ACR server and donwloads the correct data for current song, later allows to correct the song with this downloaded information, in other words, it allows to make a Shazam-like recognition but not from audio recorded, over the audiofiles you have instead and correct them.

What does it offer?
- Automatic, semiautomatic and manual correction for your audio files.
- It will be totally free(if license of ACR service allows me), open source and ad free.

What does it correct?
- Automatic, semi automatic and manual mode allows to correct title, artist, album, genre, track number, publication track year, cover art and filename (it will apply the same name of title, if is ON on Settings app).
- Download cover mode only corrects the cover art.

Features:

- Material Design UI with dark theme for battery save on AMOLED displays.
- Automatic mode: Allows select one or more songs from list and correct them without user intervention.
- Semi automatic mode: Downloads track information, but requires confirmation from user to apply the found tags.
- Manual mode: Allows to edit track for adding correct tags, including cover art.
- Download cover mode: Downloads only the cover art and allow to user save it as cover art or image file.
- Extraction and saved of cover art from audiofile.
- Basic media player integrated.
- Display of aditional data song like: size, channels, file type, resolution, frequency and speed in Kbps.
- Search integrated to list.
- Fast jump to current played song.
- One-button select all tracks for correcting.
- First app use tutorial.
- Different sort types.
- Correction indicator (for knowing what songs are corrected, what song is missing of data and what songs are no corrected yet).
- Current correction progress indicator for every song.
- Automatic and manual update of files list, in case that songs are added or removed from your smartphone.
- The correction made, including the cover art, is made over the file itself, mean that changes are visible to every media player able to read ID3 tags(most nowadays).
- Background correction, no matter if app is closed.

How to build it?

1. Download zip and import project from Android Studio, or import directly from Android Studio using the URL repo.
2. Enter here and register an account https://developer.gracenote.com/gnsdk.
3. Once you hace registered, go to MyAccount -> My Apps in top menu.
4. Create a new app, follow the instructions to create a new app and obtain the required info:
5. Once you have created the app, use the info provided from it and put the corresponding values to these fields:
          
          public static final String gnsdkLicenseString;
          public static final String gnsdkClientId;
          public static final String gnsdkClientTag;
          
6. Then, in Android Studio go to Build -> Build APK.
7. When apk is generated, copy it to your device and install it.
8. That's all!!!, I hope enjoy correcting your songs in a easier way, and specially FREE.

-VERY IMPORTANT:
1. This app is Beta version yet. 
2. This app use an ACR technology powered by Gracenote, visit https://developer.gracenote.com/gnsdk for more information.
3. This app uses the awesome jAudioTagger library, visit http://www.jthink.net/jaudiotagger/ for more information.
4. This app uses the amazing Glide library, visit https://github.com/bumptech/glide for more information.
5- This app and source code is made for Android >= 5, so maybe doesn't work on Android <= 4.
