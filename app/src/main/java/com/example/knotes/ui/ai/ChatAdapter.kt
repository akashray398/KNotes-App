package com.example.knotes.ui.ai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.knotes.R
import com.example.knotes.ui.notes.AiChatViewModel

class ChatAdapter : ListAdapter<AiChatViewModel.ChatMessage, ChatAdapter.ChatViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layout = if (viewType == VIEW_TYPE_USER) R.layout.item_chat_user else R.layout.item_chat_ai
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView: TextView = view.findViewById(R.id.textViewMessage)
        fun bind(message: AiChatViewModel.ChatMessage) {
            textView.text = message.text
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<AiChatViewModel.ChatMessage>() {
        override fun areItemsTheSame(
            oldItem: AiChatViewModel.ChatMessage,
            newItem: AiChatViewModel.ChatMessage
        ): Boolean = oldItem.timestamp == newItem.timestamp

        override fun areContentsTheSame(
            oldItem: AiChatViewModel.ChatMessage,
            newItem: AiChatViewModel.ChatMessage
        ): Boolean = oldItem == newItem
    }

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }
}
