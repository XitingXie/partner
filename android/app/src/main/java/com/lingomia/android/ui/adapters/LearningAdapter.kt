package com.lingomia.android.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lingomia.android.databinding.ItemLearningPointBinding
import com.lingomia.android.data.models.LearningPoint

class LearningAdapter : ListAdapter<LearningPoint, LearningAdapter.LearningPointViewHolder>(LearningDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LearningPointViewHolder {
        val binding = ItemLearningPointBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LearningPointViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LearningPointViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LearningPointViewHolder(
        private val binding: ItemLearningPointBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(point: LearningPoint) {
            binding.typeText.text = point.type
            binding.originalText.text = point.original
            binding.explanationText.text = point.explanation
        }
    }

    private class LearningDiffCallback : DiffUtil.ItemCallback<LearningPoint>() {
        override fun areItemsTheSame(oldItem: LearningPoint, newItem: LearningPoint): Boolean {
            return oldItem.original == newItem.original
        }

        override fun areContentsTheSame(oldItem: LearningPoint, newItem: LearningPoint): Boolean {
            return oldItem == newItem
        }
    }
} 