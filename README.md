# AutoMusicTagFixer
********************************************************************************************************************************
-VERY IMPORTANT:
1. This app is released in PlayStore, you can download it from next link: https://play.google.com/store/apps/details?id=mx.dev.franco.automusictagfixer
2. This app uses the awesome jAudioTagger library, visit http://www.jthink.net/jaudiotagger/ for more information.
********************************************************************************************************************************
- NOTICE!: Now the app is published under beta state, so you can directly download by join it to beta program, click in below link:

https://play.google.com/apps/testing/mx.dev.franco.automusictagfixer

What does it do?
- For every audio file you have in your smartphone (all supported audio types by Android and jAudioTagger library), it will generate an audio fingerprint, then will connect to an ACR server and will download the correct data for every song, later will allow you to correct the song with this downloaded information; in other words, it will make a Shazam-like recognition but not from audio recorded, but over audio files you have and will correct their meta tags, including filename.

For what?
- The objective of this app is to correct meta tags from your audio files to help you maintain your musical library more organized.

What does it offer?
- Automatic, semiautomatic and manual correction for your audio files.
- It will be totally free(if license of ACR service allows me), open source and ad free.

What corrects?
- Automatic, semi automatic and manual mode allows to correct title, artist, album, genre, track number, publication track year, cover art and filename (it will apply the same name of title, if is ON on Settings app).
- Download cover mode only corrects the cover art.


Features:
- Tags correction for your audio files. It currently fully supports Mp3, Mp4 (Mp4 audio, M4a and M4p audio) Ogg Vorbis, Flac and Wma, there is limited support for Wav and Real formats.(http://www.jthink.net/jaudiotagger/index.jsp).
- Full support for files both non removable external storage(better known as internal or shared storage) and removable storage(SD Card).
- Material Design UI with dark theme for battery save on AMOLED displays.
- Automatic mode: Allows select one or more songs from list and correct them without user intervention.
- Semi automatic mode: Downloads track information, but requires confirmation from user to apply found tags.
- Manual mode: Allows to edit tags manually, including cover art.
- Download cover mode: Downloads only the cover art and allows save it as embbed cover art or indepedent image file.
- Extraction and saved of cover art from audio file.
- Basic media player integrated(no forward nor backward, only playing funcionality).
- Additional display of song data like: file size, channels, file type, resolution, frequency and speed in Kbps.
- Search widget integrated in list.
- One-button selection for selecting all tracks, for use in automatic mode.
- First app use tutorial (after splash screen).
- Different sort types for list in main activity.
- Correction indicator for knowing what songs are already corrected, what songs are missing data, what songs are no corrected yet and what songs can not correct with this app(are not supported).
- Current correction progress indicator for every song when automatic and semi automatic modes are used.
- Automatic and manual updating of list, in case songs are added or removed from your smartphone.
- The correction made, including cover art, is made over the file itself, means that changes are visible to every media player able to read ID3 tags(most nowadays).
- For MP3 files, in any mode of correction, ID3 tags version 1 will be automatically converted to ID3 version 2, this conversion will allow to write covers over the file itself(and other tags) to mp3 files, because ID3 version 1 has not support for some tags.
- Background correction (with notification in status bar indicating the progress of task an ability to stop it from here) no matter if you close the app.
- Crashlytics to report back to developer issues and crashes.
