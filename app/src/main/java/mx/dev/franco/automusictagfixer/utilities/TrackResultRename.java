package mx.dev.franco.automusictagfixer.utilities;

import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class TrackResultRename extends AudioTagger.ResultRename {
  private Track track;

  public TrackResultRename(AudioTagger.ResultRename resultRename) {
    setCode(resultRename.getCode());
    setError(resultRename.getError());
    setNewAbsolutePath(resultRename.getNewAbsolutePath());
  }

  public TrackResultRename() {
    super();
  }

  public TrackResultRename(int code, String newAbsolutePath) {
    super(code, newAbsolutePath);
  }

  public Track getTrack() {
    return track;
  }

  public void setTrack(Track track) {
    this.track = track;
  }
}
