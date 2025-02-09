package com.lingomia.android.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lingomia.android.databinding.ItemSceneBinding
import com.lingomia.android.data.models.Scene

class ScenesAdapter(
    private val onSceneClick: (String) -> Unit
) : ListAdapter<Scene, ScenesAdapter.SceneViewHolder>(SceneDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SceneViewHolder {
        val binding = ItemSceneBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SceneViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SceneViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SceneViewHolder(
        private val binding: ItemSceneBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSceneClick(getItem(position).id)
                }
            }
        }

        fun bind(scene: Scene) {
            binding.sceneTitle.text = scene.title
            binding.sceneDescription.text = scene.description
        }
    }

    private class SceneDiffCallback : DiffUtil.ItemCallback<Scene>() {
        override fun areItemsTheSame(oldItem: Scene, newItem: Scene): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Scene, newItem: Scene): Boolean {
            return oldItem == newItem
        }
    }
}