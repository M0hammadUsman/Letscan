package com.qrscanner.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.qrscanner.R
import com.qrscanner.databinding.FragmentHistoryBinding
import com.qrscanner.db.QrScanDatabase
import com.qrscanner.entity.QrScan
import com.qrscanner.fragment.adapter.HistoryAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var database: QrScanDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup database
        database = QrScanDatabase.getDatabase(requireContext())

        // Setup RecyclerView
        historyAdapter = HistoryAdapter(
            emptyList(),
            onItemClick = { qrScan -> navigateToResult(qrScan) },
            onItemLongClick = { qrScan -> showDeleteDialog(qrScan) }
        )

        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }

        // Delete All Button
        binding.deleteAllToggle.setOnClickListener {
            showDeleteAllDialog()
        }

        // Fetch and display scan history
        viewLifecycleOwner.lifecycleScope.launch {
            database.qrScanDao().getAllScans().collect { scans ->
                historyAdapter.updateScans(scans)

                // Show/hide delete all button and empty view
                binding.deleteAllToggle.visibility = if (scans.isEmpty()) View.GONE else View.VISIBLE
                binding.emptyView.visibility = if (scans.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Setup toolbar menu
        setupToolbarMenu()
    }

    private fun setupToolbarMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.history_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete_all -> {
                        showDeleteAllDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun navigateToResult(qrScan: QrScan) {
        val action = HistoryFragmentDirections.actionHistoryToResult(qrScan.content)
        findNavController().navigate(action)
    }

    private fun showDeleteDialog(qrScan: QrScan) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Scan")
            .setMessage("Are you sure you want to delete this scan?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        database.qrScanDao().deleteScan(qrScan)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAllDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete All Scans")
            .setMessage("Are you sure you want to delete all scan history? This cannot be undone.")
            .setIcon(R.drawable.ic_delete_all)
            .setPositiveButton("Delete All") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        database.qrScanDao().deleteAllScans()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}