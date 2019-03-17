package mx.dev.franco.automusictagfixer.UI.faq;

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
    private int mCurrentExpandedPosition = -1;

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
        holder.question.setText(questionItem.getQuestion());
        holder.answer.setText(questionItem.getAnswer());
        if(questionItem.isExpanded()) {
            holder.answer.setVisibility(View.VISIBLE);
        }
        else {
            holder.answer.setVisibility(View.GONE);
        }
    }

    public void setActivePosition(int position) {
        QuestionItem collapsedQuestionItem;
        QuestionItem expandedQuestionItem;
        if(mCurrentExpandedPosition != position) {
            if(mCurrentExpandedPosition == -1) {
                expandedQuestionItem = mList.get(position);
                expandedQuestionItem.setExpanded(true);
                notifyItemChanged(position);
            }
            else {
                collapsedQuestionItem = mList.get(mCurrentExpandedPosition);
                collapsedQuestionItem.setExpanded(false);
                notifyItemChanged(mCurrentExpandedPosition);
                expandedQuestionItem = mList.get(position);
                expandedQuestionItem.setExpanded(true);
                notifyItemChanged(position);
            }
            mCurrentExpandedPosition = position;
        }
        else {
            collapsedQuestionItem = mList.get(mCurrentExpandedPosition);
            collapsedQuestionItem.setExpanded(false);
            notifyItemChanged(mCurrentExpandedPosition);
            mCurrentExpandedPosition = -1;
        }
    }

    @Override
    public int getItemCount() {
        if(mList != null)
            return mList.size();
        return 0;
    }

    public static class QuestionItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView question;
        public TextView answer;
        public LinearLayout container;
        public OnItemClick listener;

        QuestionItemHolder(View root, OnItemClick listener){
            super(root);
            question = root.findViewById(R.id.question);
            answer = root.findViewById(R.id.answer);
            container = root.findViewById(R.id.container_question);
            this.listener = listener;
            root.setOnClickListener(this);
            //Enable animations on this layout
            /*container.getLayoutTransition()
                    .enableTransitionType(LayoutTransition.CHANGING);*/
        }

        @Override
        public void onClick(View v) {
            if(listener != null){
                listener.onItemClick(getAdapterPosition(), v);
            }
        }
    }

}
