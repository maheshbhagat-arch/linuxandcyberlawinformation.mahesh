package com.example.pdfsuite

import android.content.ContentValues
import android.content.Intent
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
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max

class ScannerActivity : AppCompatActivity() {

    private data class ScanNode(val name: String, val uri: Uri)

    private val nodes = mutableListOf<ScanNode>()
    private lateinit var listAdapter: ArrayAdapter<String>
    private var lastGeneratedPdf: Uri? = null
    private var qualityPercent: Int = 85

    private val pickImages = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEachIndexed { i, uri -> nodes += ScanNode(name = "Gallery ${nodes.size + i + 1}", uri = uri) }
        refreshList()
    }

    private val capturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            val uri = saveBitmapToMediaStore(bmp)
            if (uri != null) {
                nodes += ScanNode(name = "Camera ${nodes.size + 1}", uri = uri)
                refreshList()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        val modeSpinner = findViewById<Spinner>(R.id.spinnerMode)
        val sizeSpinner = findViewById<Spinner>(R.id.spinnerPageSize)
        val listView = findViewById<ListView>(R.id.imageList)
        val status = findViewById<TextView>(R.id.tvScanStatus)
        val watermark = findViewById<EditText>(R.id.etWatermark)
        val autoCrop = findViewById<CheckBox>(R.id.cbAutoCrop)

        val modes = listOf("B&W (Archival)", "Enhance (AI Clarity)", "Scan (Document Optimized)")
        modeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)

        val pageSizes = listOf("A4 (595x842)", "Letter (612x792)")
        sizeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, pageSizes)

        listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = listAdapter

        findViewById<SeekBar>(R.id.seekQuality).apply {
            max = 40
            progress = 25
            findViewById<TextView>(R.id.tvQuality).text = "Quality: $qualityPercent%"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    qualityPercent = 60 + progress
                    findViewById<TextView>(R.id.tvQuality).text = "Quality: $qualityPercent%"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }

        findViewById<Button>(R.id.btnPickImages).setOnClickListener { pickImages.launch(arrayOf("image/*")) }
        findViewById<Button>(R.id.btnCapture).setOnClickListener { capturePreview.launch(null) }

        findViewById<Button>(R.id.btnReorder).setOnClickListener {
            nodes.reverse()
            refreshList()
            status.text = "Order changed. Total pages: ${nodes.size}"
        }

        findViewById<Button>(R.id.btnGenerateFromImages).setOnClickListener {
            if (nodes.isEmpty()) {
                Toast.makeText(this, "Pick or capture images first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isA4 = sizeSpinner.selectedItemPosition == 0
            val result = createPdfFromImages(
                scanNodes = nodes,
                mode = modeSpinner.selectedItem.toString(),
                autoCrop = autoCrop.isChecked,
                watermark = watermark.text.toString().trim(),
                pageWidth = if (isA4) 595 else 612,
                pageHeight = if (isA4) 842 else 792
            )
            status.text = result
        }

        findViewById<Button>(R.id.btnOpenLastPdf).setOnClickListener {
            val uri = lastGeneratedPdf
            if (uri == null) {
                Toast.makeText(this, "Generate PDF first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, ReaderActivity::class.java).putExtra("pdf_uri", uri.toString())
            startActivity(intent)
        }
    }

    private fun refreshList() {
        listAdapter.clear()
        listAdapter.addAll(nodes.mapIndexed { index, node -> "Page ${index + 1}: ${node.name}" })
        listAdapter.notifyDataSetChanged()
    }

    private fun createPdfFromImages(
        scanNodes: List<ScanNode>,
        mode: String,
        autoCrop: Boolean,
        watermark: String,
        pageWidth: Int,
        pageHeight: Int
    ): String {
        val document = PdfDocument()

        try {
            scanNodes.forEachIndexed { index, node ->
                val bitmap = decodeSampledBitmap(node.uri, max(pageWidth, pageHeight) * 2) ?: return@forEachIndexed
                val processed = processBitmap(bitmap, mode, autoCrop)

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                val fit = Bitmap.createScaledBitmap(processed, pageWidth - 40, pageHeight - 80, true)
                canvas.drawBitmap(fit, 20f, 20f, null)

                if (watermark.isNotBlank()) {
                    val paint = Paint().apply {
                        alpha = 80
                        textSize = 22f
                    }
                    canvas.rotate(-25f, pageWidth / 2f, pageHeight / 2f)
                    canvas.drawText("Neural Signature: $watermark", 50f, pageHeight / 2f, paint)
                    canvas.rotate(25f, pageWidth / 2f, pageHeight / 2f)
                }

                document.finishPage(page)
                processed.recycle()
                fit.recycle()
            }

            val fileName = "mb_ai_pdf_${System.currentTimeMillis()}.pdf"
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return "Failed to create output file"

            contentResolver.openOutputStream(uri)?.use { out ->
                document.writeTo(out)
            }

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            lastGeneratedPdf = uri

            return "Created ${scanNodes.size} pages successfully. File: $fileName"
        } catch (e: Exception) {
            return "Error: ${e.message}"
        } finally {
            document.close()
        }
    }

    private fun decodeSampledBitmap(uri: Uri, reqMaxSide: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }

        var sample = 1
        while ((opts.outWidth / sample) > reqMaxSide || (opts.outHeight / sample) > reqMaxSide) {
            sample *= 2
        }

        val decode = BitmapFactory.Options().apply { inSampleSize = sample }
        contentResolver.openInputStream(uri)?.use {
            return BitmapFactory.decodeStream(it, null, decode)
        }
        return null
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap): Uri? {
        val fileName = "scan_cam_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null

        contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, qualityPercent, out)
        }

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        contentResolver.update(uri, values, null, null)
        return uri
    }

    private fun processBitmap(src: Bitmap, mode: String, autoCrop: Boolean): Bitmap {
        var output = src.copy(Bitmap.Config.ARGB_8888, true)

        if (autoCrop) {
            val left = (output.width * 0.03).toInt()
            val top = (output.height * 0.03).toInt()
            val width = (output.width * 0.94).toInt().coerceAtLeast(1)
            val height = (output.height * 0.94).toInt().coerceAtLeast(1)
            output = Bitmap.createBitmap(output, left, top, width, height)
        }

        val filtered = Bitmap.createBitmap(output.width, output.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(filtered)
        val paint = Paint()
        val matrix = ColorMatrix()

        when (mode) {
            "B&W (Archival)" -> matrix.setSaturation(0f)
            "Enhance (AI Clarity)" -> matrix.set(
                floatArrayOf(
                    1.2f, 0f, 0f, 0f, 12f,
                    0f, 1.2f, 0f, 0f, 12f,
                    0f, 0f, 1.2f, 0f, 12f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            else -> matrix.set(
                floatArrayOf(
                    1.1f, 0f, 0f, 0f, 6f,
                    0f, 1.1f, 0f, 0f, 6f,
                    0f, 0f, 1.1f, 0f, 6f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(output, 0f, 0f, paint)
        return filtered
    }
}
