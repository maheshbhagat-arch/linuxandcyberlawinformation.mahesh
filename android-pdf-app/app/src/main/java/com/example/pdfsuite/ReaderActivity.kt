package com.example.pdfsuite

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class ReaderActivity : AppCompatActivity() {

    private lateinit var image: ImageView
    private lateinit var indicator: TextView
    private lateinit var loading: ProgressBar

    private var currentUri: Uri? = null
    private var currentPage = 0
    private var pageCount = 0

    private val pickPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            currentUri = it
            currentPage = 0
            renderPage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        image = findViewById(R.id.ivReader)
        indicator = findViewById(R.id.tvPageIndicator)
        loading = findViewById(R.id.loadingBar)

        currentUri = intent.getStringExtra("pdf_uri")?.let(Uri::parse)

        findViewById<Button>(R.id.btnPickPdfReader).setOnClickListener {
            pickPdf.launch(arrayOf("application/pdf"))
        }

        findViewById<Button>(R.id.btnPrev).setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                renderPage()
            }
        }

        findViewById<Button>(R.id.btnNext).setOnClickListener {
            if (currentPage < pageCount - 1) {
                currentPage++
                renderPage()
            }
        }

        findViewById<SeekBar>(R.id.seekZoom).apply {
            max = 150
            progress = 50
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val scale = (50 + progress) / 100f
                    image.scaleX = scale
                    image.scaleY = scale
                    findViewById<TextView>(R.id.tvZoom).text = "Zoom: ${(scale * 100).toInt()}%"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }

        renderPage()
    }

    private fun renderPage() {
        val uri = currentUri ?: return
        loading.visibility = View.VISIBLE

        contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) return
                pageCount = renderer.pageCount
                if (currentPage > pageCount - 1) currentPage = pageCount - 1

                renderer.openPage(currentPage).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    image.setImageBitmap(bitmap)
                    indicator.text = "Neural View ${currentPage + 1}/$pageCount"
                }
            }
        }

        loading.visibility = View.GONE
    }
}
