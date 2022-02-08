package mx.dev.franco.automusictagfixer.ui.faq

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.ui.faq.FaqAdapter.QuestionItemHolder

/**
 * Created by franco on 21/03/18.
 */
class FaqAdapter(list: List<QuestionItem>?, listener: OnItemClick) :
    RecyclerView.Adapter<QuestionItemHolder>() {
    private val mList: List<QuestionItem>? = list
    private val mListener: OnItemClick = listener
    private var mCurrentExpandedPosition = -1

    interface OnItemClick {
        fun onItemClick(position: Int, view: View?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionItemHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val v = layoutInflater.inflate(R.layout.question_item, parent, false)
        return QuestionItemHolder(v, mListener)
    }

    override fun onBindViewHolder(holder: QuestionItemHolder, position: Int) {
        val questionItem = mList!![position]
        holder.question.text = questionItem.question
        holder.answer.text = questionItem.answer
        if (questionItem.isExpanded) {
            holder.answer.visibility = View.VISIBLE
        } else {
            holder.answer.visibility = View.GONE
        }
    }

    fun setActivePosition(position: Int) {
        val collapsedQuestionItem: QuestionItem
        val expandedQuestionItem: QuestionItem
        if (mCurrentExpandedPosition != position) {
            if (mCurrentExpandedPosition == -1) {
                expandedQuestionItem = mList!![position]
                expandedQuestionItem.isExpanded = true
                notifyItemChanged(position)
            } else {
                collapsedQuestionItem = mList!![mCurrentExpandedPosition]
                collapsedQuestionItem.isExpanded = false
                notifyItemChanged(mCurrentExpandedPosition)
                expandedQuestionItem = mList[position]
                expandedQuestionItem.isExpanded = true
                notifyItemChanged(position)
            }
            mCurrentExpandedPosition = position
        } else {
            collapsedQuestionItem = mList!![mCurrentExpandedPosition]
            collapsedQuestionItem.isExpanded = false
            notifyItemChanged(mCurrentExpandedPosition)
            mCurrentExpandedPosition = -1
        }
    }

    override fun getItemCount(): Int {
        return mList?.size ?: 0
    }

    class QuestionItemHolder internal constructor(root: View, var listener: OnItemClick?) :
        RecyclerView.ViewHolder(root), View.OnClickListener {
        var question: TextView = root.findViewById(R.id.question)
        var answer: TextView = root.findViewById(R.id.answer)
        var container: LinearLayout = root.findViewById(R.id.container_question)
        override fun onClick(v: View) {
            if (listener != null) {
                listener!!.onItemClick(adapterPosition, v)
            }
        }

        init {
            root.setOnClickListener(this)
        }
    }

}