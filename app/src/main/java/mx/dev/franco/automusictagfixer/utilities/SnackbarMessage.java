package mx.dev.franco.automusictagfixer.utilities;

import android.content.Context;

import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;

import mx.dev.franco.automusictagfixer.common.Action;

/**
 * A class that wraps a message or its id resource.
 */
public class SnackbarMessage {
  private final String title;
  private final String body;
  private final String mainActionText;
  private final Action mainAction;
  private final Object data;
  private final int duration;
  private final boolean dismissible;

  public SnackbarMessage(Builder builder) {
    this.title = builder.title;
    this.body = builder.body;
    this.mainActionText = builder.mainActionText;
    this.data = builder.data;
    this.mainAction = builder.mainAction;
    this.duration = builder.duration;
    this.dismissible = builder.dismissible;
  }

  public String getTitle() {
    return title;
  }

  public String getBody() {
    return body;
  }

  public String getMainActionText() {
    return mainActionText;
  }

  public Action getMainAction() {
    return mainAction;
  }

  public boolean isDismissible() {
    return dismissible;
  }

  public int getDuration() {
    return duration;
  }

  public Object getData() {
    return data;
  }

  public static class Builder {
    private String title;
    private String body;
    private String mainActionText;
    private Action mainAction = Action.NONE;
    private Object data;
    private int duration = Snackbar.LENGTH_SHORT;
    private boolean dismissible = true;
    private WeakReference<Context> contextRef;
    public Builder(Context context) {
      contextRef = new WeakReference<>(context);
    }

    public Builder title(@StringRes int title) {
      this.title = contextRef.get().getString(title);
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder body(@StringRes int body) {
      this.body = contextRef.get().getString(body);
      return this;
    }

    public Builder body(String body) {
      this.body = body;
      return this;
    }

    public Builder mainActionText(@StringRes int mainActionText) {
      this.mainActionText = contextRef.get().getString(mainActionText);
      return this;
    }

    public Builder mainActionText(String mainActionText) {
      this.mainActionText = mainActionText;
      return this;
    }

    public Builder data(Object data) {
      this.data = data;
      return this;
    }

    public Builder action(Action action) {
      mainAction = action;
      return this;
    }

    public Builder duration(int duration) {
      this.duration = duration;
      return this;
    }

    public Builder dismissible(boolean dismissible) {
      this.dismissible = dismissible;
      return this;
    }

    public SnackbarMessage build() {
      SnackbarMessage snackbarMessage = new SnackbarMessage(this);
      contextRef.clear();
      contextRef = null;
      return snackbarMessage;
    }

  }
}
