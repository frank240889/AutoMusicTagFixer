package mx.dev.franco.musicallibraryorganizer.list;

/**
 * Created by franco on 7/11/17.
 */

public final class QuestionItem {
    int mId = -1;
    private String mQuestion = "";
    private String mAnswer = "";
    private boolean mExpanded = false;


    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public String getQuestion() {
        return mQuestion;
    }

    public void setQuestion(String question) {
        this.mQuestion = question;
    }

    public String getAnswer() {
        return mAnswer;
    }

    public void setAnswer(String answer) {
        this.mAnswer = answer;
    }

    public boolean isExpanded(){
        return this.mExpanded;
    }

    public void setExpanded(boolean expanded){
        this.mExpanded = expanded;
    }


}
