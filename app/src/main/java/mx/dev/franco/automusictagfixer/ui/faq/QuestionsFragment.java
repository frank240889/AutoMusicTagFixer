package mx.dev.franco.automusictagfixer.ui.faq;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.ui.BaseFragment;
import mx.dev.franco.automusictagfixer.ui.MainActivity;

public class QuestionsFragment extends BaseFragment implements FaqAdapter.OnItemClick{

    private List<QuestionItem> mQuestionItems;
    private FaqAdapter mFaqAdapter;
    private RecyclerView mRecyclerView;

    public QuestionsFragment(){}

    public static QuestionsFragment newInstance(){
        return new QuestionsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_questions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mQuestionItems = new ArrayList<>();
        String[] questions = getResources().getStringArray(R.array.questions);

        String[] answers = getResources().getStringArray(R.array.answers);

        mRecyclerView = view.findViewById(R.id.questions_recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
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

        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //((MainActivity)getActivity()).toggle.syncState();
        ((MainActivity)getActivity()).mActionBar.setTitle(getString(R.string.faq));
        ((MainActivity)getActivity()).mStartTaskFab.hide();
        //mToolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());

        //Get action bar from toolbar
        //((MainActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public void onItemClick(int position, View view) {
        mFaqAdapter.setActivePosition(position);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mQuestionItems.clear();
        mQuestionItems = null;
        mFaqAdapter = null;
        mRecyclerView = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }


    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation animation = super.onCreateAnimation(transit, enter, nextAnim);

        if (animation == null && nextAnim != 0) {
            animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
        }

        if (animation != null && getView() != null)
            getView().setLayerType(View.LAYER_TYPE_HARDWARE, null);

        return animation;
    }
}



