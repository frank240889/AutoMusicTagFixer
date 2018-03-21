package mx.dev.franco.automusictagfixer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mikepenz.iconics.view.IconicsTextView;

import java.util.ArrayList;
import java.util.List;

import mx.dev.franco.automusictagfixer.list.QuestionItem;

public class QuestionsActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener {

    private Toolbar toolbar;
    private List<QuestionItem> mQuestionItems;
    private FaqAdapter mFaqAdapter;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_questions);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mQuestionItems = new ArrayList<>();
        String[] questions = getResources().getStringArray(R.array.questions);

        String[] answers = getResources().getStringArray(R.array.answers);

        for(int y = 0 ; y < questions.length ; y++){
            QuestionItem questionItem = new QuestionItem();
            questionItem.setQuestion(questions[y]);
            questionItem.setAnswer(answers[y]);
            mQuestionItems.add(questionItem);
        }

        mListView = (ListView) findViewById(R.id.questions_listview);
        mFaqAdapter = new FaqAdapter(mQuestionItems);
        mListView.setAdapter(mFaqAdapter);
        mFaqAdapter.notifyDataSetChanged();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                IconicsTextView answer = view.findViewById(R.id.answer);
                if(answer.getVisibility() == View.GONE){
                    answer.setVisibility(View.VISIBLE);
                }
                else {
                    answer.setVisibility(View.GONE);
                }
            }
        });
        Log.d("size adapter", mFaqAdapter.getCount()+"");
        Log.d("size list", mQuestionItems.size()+"");

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return true;
    }


    private class FaqAdapter extends ArrayAdapter {
        private List<QuestionItem> mList;

        public FaqAdapter(List list){
            super(QuestionsActivity.this, 0);
            mList = list;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            QuestionItem questionItem = mList.get(position);
            if(convertView == null){
                LayoutInflater layoutInflater = getLayoutInflater();
                convertView = layoutInflater.inflate(R.layout.question_item, null);
            }
            ((TextView)convertView.findViewById(R.id.question)).setText(questionItem.getQuestion());
            ((TextView)convertView.findViewById(R.id.answer)).setText(questionItem.getAnswer());

            return convertView;
        }

        @Override
        public int getCount(){
            if(mList != null)
                return mList.size();
            return 0;
        }

    }

}



