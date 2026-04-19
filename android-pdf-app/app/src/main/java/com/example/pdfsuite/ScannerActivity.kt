package com.example.pdfsuite

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class ScannerActivity : AppCompatActivity() {

    private val imageUris = mutableListOf<Uri>()
    private lateinit var listAdapter: ArrayAdapter<String>

    private val pickImages = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        imageUris.clear()
        imageUris.addAll(uris)
        refreshList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        val modeSpinner = findViewById<Spinner>(R.id.spinnerMode)
        val listView = findViewById<ListView>(R.id.imageList)
        val status = findViewById<TextView>(R.id.tvScanStatus)
        val watermark = findViewById<EditText>(R.id.etWatermark)
        val autoCrop = findViewById<CheckBox>(R.id.cbAutoCrop)

        val modes = listOf("B&W (Archival)", "Enhance (AI Clarity)", "Scan (Document Optimized)")
        modeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)

        listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = listAdapter

        findViewById<Button>(R.id.btnPickImages).setOnClickListener {
            pickImages.launch(arrayOf("image/*"))
        }

        findViewById<Button>(R.id.btnReorder).setOnClickListener {
            imageUris.reverse()
            refreshList()
            status.text = "Sequential compiling order reversed (simulated reorder)."
        }

        findViewById<Button>(R.id.btnGenerateFromImages).setOnClickListener {
            if (imageUris.isEmpty()) {
                Toast.makeText(this, "Pick images first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = createPdfFromImages(
                imageUris,
                modeSpinner.selectedItem.toString(),
                autoCrop.isChecked,
                watermark.text.toString().trim()
            )
            status.text = result
        }
    }

    private fun refreshList() {
        listAdapter.clear()
        listAdapter.addAll(imageUris.mapIndexed { index, uri -> "Node ${index + 1}: $uri" })
        listAdapter.notifyDataSetChanged()
    }

    private fun createPdfFromImages(
        uris: List<Uri>,
        mode: String,
        autoCrop: Boolean,
        watermark: String
    ): String {
        val document = PdfDocument()

        try {
            uris.forEachIndexed { index, uri ->
                val bitmap = decodeBitmap(uri) ?: return@forEachIndexed
                val processed = processBitmap(bitmap, mode, autoCrop)

                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                val scaled = Bitmap.createScaledBitmap(processed, 555, 760, true)
                canvas.drawBitmap(scaled, 20f, 20f, null)

                if (watermark.isNotBlank()) {
                    val paint = Paint().apply {
                        alpha = 80
                        textSize = 24f
                    }
                    canvas.rotate(-25f, 300f, 420f)
                    canvas.drawText("Neural Signature: $watermark", 70f, 420f, paint)
                    canvas.rotate(25f, 300f, 420f)
                }

                document.finishPage(page)
            }

            val fileName = "neural_scan_${System.currentTimeMillis()}.pdf"
            val values = android.content.ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return "Failed to create output file"

            contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)

            return "Neural Scanner compiled ${uris.size} nodes to $fileName"
        } catch (e: Exception) {
            return "Error: ${e.message}"
        } finally {
            document.close()
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        return null
    }

    private fun processBitmap(src: Bitmap, mode: String, autoCrop: Boolean): Bitmap {
        var output = src.copy(Bitmap.Config.ARGB_8888, true)

        if (autoCrop) {
            val left = (output.width * 0.03).toInt()
            val top = (output.height * 0.03).toInt()
            val width = (output.width * 0.94).toInt()
            val height = (output.height * 0.94).toInt()
            output = Bitmap.createBitmap(output, left, top, width, height)
        }

        val canvas = Canvas(output)
        val paint = Paint()
        val matrix = ColorMatrix()

        when (mode) {
            "B&W (Archival)" -> matrix.setSaturation(0f)
            "Enhance (AI Clarity)" -> {
                matrix.set(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, 10f,
                        0f, 1.2f, 0f, 0f, 10f,
                        0f, 0f, 1.2f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            else -> {
                matrix.set(
                    floatArrayOf(
                        1.1f, 0f, 0f, 0f, 0f,
                        0f, 1.1f, 0f, 0f, 0f,
                        0f, 0f, 1.1f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
        }

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(output, 0f, 0f, paint)
        return output
    }
}
