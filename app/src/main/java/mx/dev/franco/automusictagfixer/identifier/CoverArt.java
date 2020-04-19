package mx.dev.franco.automusictagfixer.identifier;

public class CoverArt extends Identifier.IdentificationResults {
    private String size = "";
    private String url = "";
    private String dimension = "";
    public CoverArt() {
    }

    public CoverArt(String size, String url) {
        this();
        this.size = size;
        this.url = url;
    }

    public CoverArt(String size, String url, String dimension) {
        this(size, url);
        this.dimension = dimension;
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

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }
}
