package com.lingomia.android.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lingomia.android.R

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    private val messages = mutableListOf<Message>()
    private var showFeedback = false

    data class Message(
        val content: String,
        val isUser: Boolean,
        var feedback: String? = null
    )

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
        val feedbackText: TextView? = view.findViewById(R.id.feedbackText)
    }

    fun getLastUserMessage(): Message? {
        return messages.findLast { it.isUser }
    }

    fun updateMessageFeedback(message: Message, feedback: String?) {
        val index = messages.indexOf(message)
        if (index != -1) {
            messages[index] = message.copy(feedback = feedback)
            notifyItemChanged(index)
        }
    }

    fun setShowFeedback(show: Boolean) {
        showFeedback = show
        notifyDataSetChanged()
    }

    fun addMessage(message: String, isUser: Boolean, feedback: String? = null) {
        messages.add(Message(message, isUser, feedback))
        notifyItemInserted(messages.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == 0) R.layout.item_message_user else R.layout.item_message_bot
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.content
        
        holder.feedbackText?.apply {
            if (showFeedback && message.isUser && message.feedback != null) {
                visibility = View.VISIBLE
                text = message.feedback
                Log.d("ChatAdapter", "Showing feedback: ${message.feedback}")
            } else {
                visibility = View.GONE
                Log.d("ChatAdapter", "Hiding feedback. showFeedback: $showFeedback, isUser: ${message.isUser}, feedback: ${message.feedback}")
            }
        }
    }

    override fun getItemCount() = messages.size

    override fun getItemViewType(position: Int) = if (messages[position].isUser) 0 else 1
} 