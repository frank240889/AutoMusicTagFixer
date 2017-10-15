# AutomusicTagFixer
What does it do?
- From every file you have in your smartphone, it generates an audio fingerprint, then connects to a ACR server and donwloads the correct data for current song, later allows to correct the song with this downloaded information, in other words, it allows to make a Shazam-like recognition but not from audio recorded, over the audiofiles you have instead and correct them.

What does it offer?
- Automatic, semiautomatic and manual correction for your audio files (mp3 files only for now).
- It will be totally free(if license of ACR service allows me), open source and ad free. You can fork it and modified. Just give me the credits for the base code.

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
- Dsiplay of aditional data of song like: size, channels, file type (only mp3 for now), resolution, frequency and speed in Kbps.
- Search integrated to list.
- Fast jump to current played song.
- One-button select all tracks for correcting.
- First app use tutorial.
- Correction indicator (for knowing what songs are corrected, what song is missing of data and what songs are no corrected yet).
- Current correction progress indicator for every song.
- Automatic and manual update of files list, in case that songs are added or removed from your smartphone.
- The correction made, including the cover art, is made over the file itself, mean that changes are visible to every media player able to read ID3 tags(most actually).
- Background correction, no matter if app is closed.

How to compile?(instructions in progress)

1. Download zip and import project from Android Studio, or import directly from Android Studio using the URL repo.
2. Download MyID3 library(https://sites.google.com/site/eternalsandbox/myid3-for-android/MyID3_for_Android.jar?attredirects=0) and put the JAR file in "MusicalLibraryOrganizer\app\libs", if "libs".
3. Download GNDK from https://developer.gracenote.com/gnsdk, you need to create an account, then register another API Key here https://developer.gracenote.com/user/25835/apps/add.
4. Extract the GNDK files from zip  
 folder doesn't exist, create

This app use an ACR technology powered by Gracenote.


This app and source code is made for Android >= 5, so maybe doesn't work on Android <= 4.
