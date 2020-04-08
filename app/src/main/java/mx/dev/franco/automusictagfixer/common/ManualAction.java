package mx.dev.franco.automusictagfixer.common;

public class ManualAction {
    private String mActionMessage = "";
    private String mActionText = null;
    private Action mIdAction =  Action.NONE;

    public ManualAction() {
    }

    public ManualAction(String actionMessage, String actionText, Action idAction) {
        this.mActionMessage = actionMessage;
        this.mActionText = actionText;
        this.mIdAction = idAction;
    }

    public String getActionMessage() {
        return mActionMessage;
    }

    public void setActionMessage(String actionMessage) {
        this.mActionMessage = actionMessage;
    }

    public String getActionText() {
        return mActionText;
    }

    public void setActionText(String actionText) {
        this.mActionText = actionText;
    }

    public Action getIdAction() {
        return mIdAction;
    }

    public void setIdAction(Action idAction) {
        this.mIdAction = idAction;
    }
}
