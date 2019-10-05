package mx.dev.franco.automusictagfixer.utilities;

import mx.dev.franco.automusictagfixer.common.Action;

/**
 * A class that wraps a message with an action to take.
 */
public class ActionableMessage extends Message {
  private Action action;
  public ActionableMessage(Action action, String message) {
    super(message);
    this.action = action;
  }

  public ActionableMessage(Action action, int idResourceMessage) {
    super(idResourceMessage);
    this.action = action;
  }

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }
}
