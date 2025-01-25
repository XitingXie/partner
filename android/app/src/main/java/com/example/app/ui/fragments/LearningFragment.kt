package com.example.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.databinding.FragmentLearningBinding
import com.example.app.ui.adapters.LearningAdapter
import com.example.app.data.models.LearningPoint

class LearningFragment : Fragment() {
    private var _binding: FragmentLearningBinding? = null
    private val binding get() = _binding!!
    private lateinit var learningAdapter: LearningAdapter
    private val learningPoints = mutableListOf<LearningPoint>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        learningAdapter = LearningAdapter()
        binding.learningPointsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = learningAdapter
        }
    }

    fun addLearningPoints(points: List<LearningPoint>) {
        learningPoints.addAll(points)
        learningAdapter.submitList(learningPoints.toList())
    }
} 