package mx.dev.franco.automusictagfixer.utilities;

/**
 * A class that wraps a message or its id resource.
 */
public class Message {
  protected String message;
  protected int idResourceMessage;
  protected String details;

  public Message(){}

  public Message(String message) {
    this();
    this.message = message;
  }

  public Message(String message, String details) {
    this();
    this.message = message;
    this.details = details;
  }

  public Message(int idResourceMessage) {
    this();
    this.idResourceMessage = idResourceMessage;
  }

  public Message(int idResourceMessage, String details) {
    this();
    this.idResourceMessage = idResourceMessage;
    this.details = details;
  }

  public Message(String message, int idResourceMessage) {
    this();
    this.message = message;
    this.idResourceMessage = idResourceMessage;
  }

  public Message(String message, int idResourceMessage, String details) {
    this();
    this.message = message;
    this.idResourceMessage = idResourceMessage;
    this.details = details;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public int getIdResourceMessage() {
    return idResourceMessage;
  }

  public void setIdResourceMessage(int idResourceMessage) {
    this.idResourceMessage = idResourceMessage;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }
}
