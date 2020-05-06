package mx.dev.franco.automusictagfixer.utilities;

import android.content.Context;
import androidx.annotation.StringRes;
import com.google.android.material.snackbar.Snackbar;
import java.lang.ref.WeakReference;
import mx.dev.franco.automusictagfixer.common.Action;

/**
 * A class that wraps a message or its id resource.
 */
public class SnackbarMessage<D> {
  private final String title;
  private final String body;
  private final String mainActionText;
  private final Action mainAction;
  private final D data;
  private final int duration;
  private final boolean dismissible;

  public SnackbarMessage(Builder<D> builder) {
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

  public D getData() {
    return data;
  }

  public static class Builder<D> {
    private String title;
    private String body;
    private String mainActionText;
    private Action mainAction = Action.NONE;
    private D data;
    private int duration = Snackbar.LENGTH_SHORT;
    private boolean dismissible = true;
    private WeakReference<Context> contextRef;
    public Builder(Context context) {
      contextRef = new WeakReference<>(context);
    }

    public Builder<D> title(@StringRes int title) {
      this.title = contextRef.get().getString(title);
      return this;
    }

    public Builder<D> title(String title) {
      this.title = title;
      return this;
    }

    public Builder<D> body(@StringRes int body) {
      this.body = contextRef.get().getString(body);
      return this;
    }

    public Builder<D> body(String body) {
      this.body = body;
      return this;
    }

    public Builder<D> mainActionText(@StringRes int mainActionText) {
      this.mainActionText = contextRef.get().getString(mainActionText);
      return this;
    }

    public Builder<D> mainActionText(String mainActionText) {
      this.mainActionText = mainActionText;
      return this;
    }

    public Builder<D> data(D data) {
      this.data = data;
      return this;
    }

    public Builder<D> action(Action action) {
      mainAction = action;
      return this;
    }

    public Builder<D> duration(int duration) {
      this.duration = duration;
      return this;
    }

    public Builder<D> dismissible(boolean dismissible) {
      this.dismissible = dismissible;
      return this;
    }

    public SnackbarMessage<D> build() {
      SnackbarMessage snackbarMessage = new SnackbarMessage<>(this);
      contextRef.clear();
      contextRef = null;
      return snackbarMessage;
    }

  }
}
