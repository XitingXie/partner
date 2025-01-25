package com.example.app.ui.adapters

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.app.ui.fragments.ChatFragment
import com.example.app.ui.fragments.LearningFragment

class ConversationPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        Log.d("ConversationPagerAdapter", "Creating fragment at position $position")
        return when (position) {
            0 -> ChatFragment().apply {
                Log.d("ConversationPagerAdapter", "Created ChatFragment")
            }
            1 -> LearningFragment().apply {
                Log.d("ConversationPagerAdapter", "Created LearningFragment")
            }
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
} 