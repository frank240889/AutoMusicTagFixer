package mx.dev.franco.automusictagfixer.ui.faq;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import mx.dev.franco.automusictagfixer.R;

public class QuestionsActivity extends AppCompatActivity implements FaqAdapter.OnItemClick{

    private List<QuestionItem> mQuestionItems;
    private FaqAdapter mFaqAdapter;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_questions);
        //Set an action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> QuestionsActivity.super.onBackPressed());

        //Get action bar from toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.faq));

        mQuestionItems = new ArrayList<>();
        String[] questions = getResources().getStringArray(R.array.questions);

        String[] answers = getResources().getStringArray(R.array.answers);

        mRecyclerView = findViewById(R.id.questions_recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(10);
        mFaqAdapter = new FaqAdapter(mQuestionItems, this);
        mRecyclerView.setAdapter(mFaqAdapter);

        for(int y = 0 ; y < questions.length ; y++){
            QuestionItem questionItem = new QuestionItem();
            questionItem.setQuestion(questions[y]);
            questionItem.setAnswer(answers[y]);
            mQuestionItems.add(questionItem);
            mFaqAdapter.notifyItemInserted(mQuestionItems.size()-1);
        }

    }

    @Override
    public void onItemClick(int position, View view) {
        mFaqAdapter.setActivePosition(position);
        /*View answer = view.findViewById(R.id.answer);
        if(answer.getVisibility() == View.GONE){
            answer.setVisibility(View.VISIBLE);
        }
        else {
            answer.setVisibility(View.GONE);
        }*/
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mQuestionItems.clear();
        mQuestionItems = null;
        mFaqAdapter = null;
        mRecyclerView = null;
    }


}



