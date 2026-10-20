package com.nytech.sgs

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var ivDisplay: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var tvFirstAid: TextView
    private lateinit var progressBar: ProgressBar
    private val processor = SGSImageProcessor()
    private var currentProcessedBitmap: Bitmap? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> processUri(uri) }
        }
    }

    private val captureCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let { analyzeBitmap(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildDynamicUi())
    }

    private fun processUri(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { stream ->
            val b = BitmapFactory.decodeStream(stream)
            analyzeBitmap(b)
        }
    }

    private fun analyzeBitmap(rawBitmap: Bitmap) {
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "SGS SCANNING IN PROGRESS..."

        CoroutineScope(Dispatchers.Default).launch {
            val result = processor.processImage(rawBitmap)
            currentProcessedBitmap = result.annotatedBitmap
            val aidInstructions = FirstAidEngine.getProtocol(result.detections)

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                ivDisplay.setImageBitmap(result.annotatedBitmap)
                tvStatus.text = "SCAN COMPLETE // ${result.summary}"
                tvFirstAid.text = aidInstructions
            }
        }
    }

    private fun saveAnalyzedImage() {
        val bmp = currentProcessedBitmap ?: run {
            Toast.makeText(this, "No analyzed image to save!", Toast.LENGTH_SHORT).show()
            return
        }
        val filename = "SGS_FORENSIC_${System.currentTimeMillis()}.jpg"
        val values = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SGS_Forensics")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { stream: OutputStream ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            Toast.makeText(this, "Saved to Gallery / SGS_Forensics!", Toast.LENGTH_LONG).show()
        }
    }

    private fun buildDynamicUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#070A11"))
            setPadding(24, 36, 24, 24)
        }

        val tvHeader = TextView(this).apply {
            text = "SGS 2.0 // TACTICAL OPTICS"
            setTextColor(android.graphics.Color.parseColor("#00F0FF"))
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        root.addView(tvHeader)

        val tvSub = TextView(this).apply {
            text = "AI-Assisted Vehicle, Palm & Trauma Identification"
            setTextColor(android.graphics.Color.parseColor("#64748B"))
            textSize = 12f
        }
        root.addView(tvSub)

        ivDisplay = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 850
            ).apply { setMargins(0, 20, 0, 16) }
            setBackgroundColor(android.graphics.Color.parseColor("#0D1424"))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        root.addView(ivDisplay)

        progressBar = ProgressBar(this).apply { visibility = View.GONE }
        root.addView(progressBar)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setMargins(0, 0, 0, 16)
        }

        fun createSciFiBtn(label: String, colorHex: String, action: () -> Unit): Button {
            return Button(this).apply {
                text = label
                setBackgroundColor(android.graphics.Color.parseColor(colorHex))
                setTextColor(android.graphics.Color.BLACK)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setOnClickListener { action() }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(6, 0, 6, 0)
                }
            }
        }

        btnRow.addView(createSciFiBtn("📷 CAMERA", "#00F0FF") {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            captureCameraLauncher.launch(takePictureIntent)
        })
        btnRow.addView(createSciFiBtn("🖼 GALLERY", "#00E676") {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        })
        btnRow.addView(createSciFiBtn("💾 SAVE", "#FFB300") { saveAnalyzedImage() })
        root.addView(btnRow)

        tvStatus = TextView(this).apply {
            text = "READY // AWAITING SENSORY INPUT"
            setTextColor(android.graphics.Color.parseColor("#00F0FF"))
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        root.addView(tvStatus)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        tvFirstAid = TextView(this).apply {
            text = "FIRST-AID PROTOCOL:\nSystem standing by for image trauma triage..."
            setTextColor(android.graphics.Color.parseColor("#F1F5F9"))
            setBackgroundColor(android.graphics.Color.parseColor("#141D33"))
            setPadding(20, 20, 20, 20)
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        scroll.addView(tvFirstAid)
        root.addView(scroll)

        return root
    }

    private fun LinearLayout.setMargins(l: Int, t: Int, r: Int, b: Int) {
        val params = layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(l, t, r, b)
        layoutParams = params
    }
}
