package com.qrscanner.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.qrscanner.databinding.FragmentResultBinding
import com.qrscanner.db.QrScanDatabase
import com.qrscanner.entity.QrScan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL

class ResultFragment : Fragment() {
    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val args: ResultFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val qrContent = args.qrContent

        // Add back button functionality
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Determine if the content is a valid URL
        val isUrl = try {
            URL(qrContent)
            true
        } catch (e: Exception) {
            false
        }

        if (isUrl) {
            // URL handling
            binding.resultContainer.visibility = View.GONE
            binding.webViewContainer.visibility = View.VISIBLE
            binding.loadingBar.visibility = View.VISIBLE

            binding.webView.apply {
                webViewClient = WebViewClient()
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        if (newProgress == 100) {
                            // Hide loading bar when page has fully loaded
                            binding.loadingBar.visibility = View.GONE
                            binding.webView.visibility = View.VISIBLE
                        } else {
                            // Show loading bar while page is loading
                            binding.loadingBar.visibility = View.VISIBLE
                            binding.webView.visibility = View.GONE
                        }
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                }

                // Start loading the URL
                loadUrl(qrContent)
            }
        } else {
            // Non-URL content handling
            binding.resultContainer.visibility = View.VISIBLE
            binding.webViewContainer.visibility = View.GONE

            // Display the text content
            binding.resultText.text = qrContent

            // Copy button functionality
            binding.copyButton.setOnClickListener {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("QR Code Content", qrContent)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        // Save scan to database asynchronously
        val qrScan = QrScan(content = qrContent, isUrl = isUrl)
        CoroutineScope(Dispatchers.IO).launch {
            val database = QrScanDatabase.getDatabase(requireContext())
            database.qrScanDao().insertScan(qrScan)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

