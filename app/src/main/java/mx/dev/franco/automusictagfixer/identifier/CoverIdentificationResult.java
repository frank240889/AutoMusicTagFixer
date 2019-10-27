package mx.dev.franco.automusictagfixer.identifier;

import com.gracenote.gnsdk.GnImageSize;

public class CoverIdentificationResult extends Identifier.IdentificationResults {
    private byte[] cover;
    private String size;
    private GnImageSize gnImageSize;

    public CoverIdentificationResult() { }

    public CoverIdentificationResult(byte[] cover, String size, GnImageSize gnImageSize) {
        this();
        this.cover = cover;
        this.size = size;
        this.gnImageSize = gnImageSize;
    }

    public byte[] getCover() {
        return cover;
    }

    public void setCover(byte[] cover) {
        this.cover = cover;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public GnImageSize getGnImageSize() {
        return gnImageSize;
    }

    public void setGnImageSize(GnImageSize gnImageSize) {
        this.gnImageSize = gnImageSize;
    }
}
