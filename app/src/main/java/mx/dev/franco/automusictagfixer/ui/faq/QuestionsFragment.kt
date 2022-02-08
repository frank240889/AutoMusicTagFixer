package mx.dev.franco.automusictagfixer.ui.faq

import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.ui.BaseFragment
import mx.dev.franco.automusictagfixer.ui.faq.FaqAdapter.OnItemClick
import mx.dev.franco.automusictagfixer.ui.main.MainActivity
import java.util.*

class QuestionsFragment : BaseFragment(), OnItemClick {
    private var mQuestionItems: MutableList<QuestionItem>? = null
    private var mFaqAdapter: FaqAdapter? = null
    private var mRecyclerView: RecyclerView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_questions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mQuestionItems = ArrayList()
        val questions = resources.getStringArray(R.array.questions)
        val answers = resources.getStringArray(R.array.answers)
        mRecyclerView = view.findViewById(R.id.questions_recyclerview)
        mRecyclerView!!.layoutManager = LinearLayoutManager(activity)
        mRecyclerView!!.setHasFixedSize(true)
        mRecyclerView!!.setItemViewCacheSize(10)
        mFaqAdapter = FaqAdapter(mQuestionItems, this)
        mRecyclerView!!.adapter = mFaqAdapter
        for (y in questions.indices) {
            val questionItem = QuestionItem()
            questionItem.question = questions[y]
            questionItem.answer = answers[y]
            mQuestionItems!!.add(questionItem)
            mFaqAdapter!!.notifyItemInserted(mQuestionItems!!.size - 1)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as MainActivity?)!!.actionBar!!.title = getString(R.string.faq)
    }

    override fun onItemClick(position: Int, view: View?) {
        mFaqAdapter!!.setActivePosition(position)
    }

    override fun onDestroy() {
        super.onDestroy()
        mQuestionItems!!.clear()
        mQuestionItems = null
        mFaqAdapter = null
        mRecyclerView = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        var animation = super.onCreateAnimation(transit, enter, nextAnim)
        if (animation == null && nextAnim != 0) {
            animation = AnimationUtils.loadAnimation(activity, nextAnim)
        }
        if (animation != null && view != null) requireView().setLayerType(View.LAYER_TYPE_HARDWARE, null)
        return animation
    }

    companion object {
        @JvmStatic
        fun newInstance(): QuestionsFragment {
            return QuestionsFragment()
        }
    }
}