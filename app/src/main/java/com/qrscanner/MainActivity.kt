package com.qrscanner

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.qrscanner.databinding.ActivityMainBinding
import com.qrscanner.fragment.ScanFragmentDirections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var navController: NavController
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val WRITE_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private const val READ_MEDIA_IMAGES_PERMISSION = Manifest.permission.READ_MEDIA_IMAGES
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            startCamera()
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        // Add navigation listener to manage camera state
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.scanFragment -> {
                    // Restart camera when returning to scan fragment
                    startCamera()
                    binding.previewView.visibility = View.VISIBLE

                    // Show flash toggle button
                    binding.flashToggle.visibility = View.VISIBLE
                }
                R.id.resultFragment -> {
                    // Stop camera when navigating to result fragment
                    stopCamera()
                    binding.previewView.visibility = View.GONE
                    // Hide flash toggle button
                    binding.flashToggle.visibility = View.GONE
                }
                R.id.historyFragment -> {
                    // Hide camera-related views
                    stopCamera()
                    binding.previewView.visibility = View.GONE
                    // Hide flash toggle button, show/hide delete all based on history
                    binding.flashToggle.visibility = View.GONE
                }
            }
        }

        binding.flashToggle.setOnClickListener {
            toggleFlash()
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(arrayOf(CAMERA_PERMISSION, READ_MEDIA_IMAGES_PERMISSION))
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(CAMERA_PERMISSION, WRITE_EXTERNAL_STORAGE_PERMISSION),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun allPermissionsGranted() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(baseContext, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(baseContext, READ_MEDIA_IMAGES_PERMISSION) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(baseContext, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(baseContext, WRITE_EXTERNAL_STORAGE_PERMISSION) == PackageManager.PERMISSION_GRANTED
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrResult ->
                        runOnUiThread {
                            handleQRCodeResult(qrResult)
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera and store the camera instance
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                this.cameraProvider = cameraProvider
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun toggleFlash() {
        camera?.let { cam ->
            cam.cameraInfo.hasFlashUnit().let { hasFlash ->
                if (hasFlash) {
                    val isFlashOn = cam.cameraInfo.torchState.value == TorchState.ON
                    cam.cameraControl.enableTorch(!isFlashOn)

                    // Update button icon
                    binding.flashToggle.setImageResource(
                        if (!isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
                    )
                }
            }
        }
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        camera = null
    }

    private fun handleQRCodeResult(result: String) {
        // Add logging to understand the current state
        Log.d("Navigation", "Current destination: ${navController.currentDestination?.label}")

        try {
            // Check if we're on the correct starting fragment
            if (navController.currentDestination?.id == R.id.scanFragment) {
                // Stop camera before navigating
                stopCamera()
                binding.previewView.visibility = View.GONE

                val action = ScanFragmentDirections.actionScanToResult(result)
                navController.navigate(action)
            } else {
                // Optionally, you might want to wait or skip navigation
                Log.w("Navigation", "Cannot navigate from current destination")
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Navigation error", e)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

class QRCodeAnalyzer(
    private val onQrCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    // Use an atomic boolean to prevent multiple detections
    private val isProcessing = AtomicBoolean(false)

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        // Prevent concurrent processing
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            val scanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_DATA_MATRIX
                    )
                    .build()
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        // Additional validation checks
                        val isValidBarcode = isValidBarcodeDetection(barcode, imageProxy)

                        if (isValidBarcode) {
                            when (barcode.valueType) {
                                Barcode.TYPE_URL -> {
                                    barcode.url?.url?.let { onQrCodeDetected(it) }
                                }
                                Barcode.TYPE_TEXT -> {
                                    barcode.displayValue?.let { onQrCodeDetected(it) }
                                }
                            }
                            // Break after first valid detection
                            break
                        }
                    }
                }
                .addOnFailureListener {
                    // Error handling
                    Log.e("QRCodeAnalyzer", "Scanning failed", it)
                }
                .addOnCompleteListener {
                    isProcessing.set(false)
                    imageProxy.close()
                }
        } else {
            isProcessing.set(false)
            imageProxy.close()
        }
    }

    private fun isValidBarcodeDetection(barcode: Barcode, imageProxy: ImageProxy): Boolean {
        // Check if barcode content is not empty
        if (barcode.displayValue.isNullOrBlank()) return false

        // Check barcode corners to ensure it's within a reasonable portion of the image
        val imageWidth = imageProxy.width
        val imageHeight = imageProxy.height

        // Get barcode corner points
        val cornerPoints = barcode.cornerPoints ?: return false

        // Calculate the area of the detected barcode
        val barcodeArea = calculatePolygonArea(cornerPoints)
        val imageArea = imageWidth.toFloat() * imageHeight.toFloat()

        // Check if barcode occupies a reasonable portion of the image
        val barcodeRatio = barcodeArea / imageArea

        // Logging for debugging
        Log.d("QRCodeAnalyzer", "Barcode Area Ratio: $barcodeRatio")

        // Adjust these thresholds based on your specific use case
        return barcodeRatio in 0.05f..0.7f &&
                barcode.displayValue?.length?.let { it in 5..500 } ?: false
    }

    // Helper function to calculate polygon area
    private fun calculatePolygonArea(points: Array<Point>?): Float {
        if (points == null || points.size < 3) return 0f

        var area = 0f
        for (i in points.indices) {
            val j = (i + 1) % points.size
            area += points[i].x * points[j].y
            area -= points[j].x * points[i].y
        }
        return abs(area / 2f)
    }
}