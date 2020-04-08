package mx.dev.franco.automusictagfixer.identifier;

public class CoverArt extends Identifier.IdentificationResults {
    private String size = "";
    private String url = "";
    public CoverArt() {
    }

    public CoverArt(String size, String url) {
        this();
        this.size = size;
        this.url = url;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
