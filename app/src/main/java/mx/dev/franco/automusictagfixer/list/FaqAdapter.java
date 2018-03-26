package mx.dev.franco.automusictagfixer.list;

import android.animation.LayoutTransition;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import mx.dev.franco.automusictagfixer.R;

/**
 * Created by franco on 21/03/18.
 */

public class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.QuestionItemHolder> {
    private List<QuestionItem> mList;
    private OnItemClick mListener;

    public interface OnItemClick{
        void onItemClick(int position, View view);
    }

    public FaqAdapter(List list, OnItemClick listener){
        mList = list;
        mListener = listener;
    }

    @NonNull
    @Override
    public QuestionItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View v = layoutInflater.inflate(R.layout.question_item, parent, false);
        return new QuestionItemHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionItemHolder holder, int position) {
        QuestionItem questionItem = mList.get(position);
        holder.mQuestion.setText(questionItem.getQuestion());
        holder.mAnswer.setText(questionItem.getAnswer());
    }

    @Override
    public int getItemCount() {
        if(mList != null)
            return mList.size();
        return 0;
    }

    public class QuestionItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView mQuestion;
        public TextView mAnswer;
        public LinearLayout mContainer;
        public OnItemClick mListener;

        QuestionItemHolder(View root, OnItemClick listener){
            super(root);
            mQuestion = root.findViewById(R.id.question);
            mAnswer = root.findViewById(R.id.answer);
            mContainer = root.findViewById(R.id.container_questiom);
            mListener = listener;
            root.setOnClickListener(this);
            //Enable animations on this layout
            mContainer.getLayoutTransition()
                    .enableTransitionType(LayoutTransition.CHANGING);
        }

        @Override
        public void onClick(View v) {
            if(mListener != null){
                mListener.onItemClick(getAdapterPosition(), v);
            }
        }
    }

}
