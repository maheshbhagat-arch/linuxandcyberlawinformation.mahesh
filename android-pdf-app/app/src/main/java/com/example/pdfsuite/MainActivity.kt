package com.example.pdfsuite

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btnPickPdf: Button
    private lateinit var btnGeneratePdf: Button
    private lateinit var pdfPreview: ImageView
    private lateinit var etTitle: EditText
    private lateinit var etBody: EditText
    private lateinit var tvStatus: TextView

    private val openPdfLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            setStatus("No file selected.")
            return@registerForActivityResult
        }
        renderFirstPage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setListeners()
    }

    private fun bindViews() {
        btnPickPdf = findViewById(R.id.btnPickPdf)
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf)
        pdfPreview = findViewById(R.id.pdfPreview)
        etTitle = findViewById(R.id.etTitle)
        etBody = findViewById(R.id.etBody)
        tvStatus = findViewById(R.id.tvStatus)
    }

    private fun setListeners() {
        btnPickPdf.setOnClickListener {
            openPdfLauncher.launch(arrayOf("application/pdf"))
        }

        btnGeneratePdf.setOnClickListener {
            val title = etTitle.text.toString().trim().ifBlank { "Untitled Document" }
            val body = etBody.text.toString().trim().ifBlank {
                "No content provided."
            }
            createPdf(title, body)
        }
    }

    private fun renderFirstPage(uri: Uri) {
        try {
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) {
                        setStatus("Selected PDF has no pages.")
                        return
                    }

                    renderer.openPage(0).use { page ->
                        val bitmap = Bitmap.createBitmap(
                            page.width,
                            page.height,
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pdfPreview.setImageBitmap(bitmap)
                        setStatus("Opened PDF. Showing page 1 of ${renderer.pageCount}.")
                    }
                }
            } ?: run {
                setStatus("Unable to read the selected file.")
            }
        } catch (e: Exception) {
            setStatus("Error opening PDF: ${e.message}")
        }
    }

    private fun createPdf(title: String, body: String) {
        val document = PdfDocument()

        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            drawPdfContent(canvas, title, body)
            document.finishPage(page)

            val outputFile = File(filesDir, "generated_${System.currentTimeMillis()}.pdf")
            outputFile.outputStream().use { out ->
                document.writeTo(out)
            }

            setStatus("PDF saved in app storage: ${outputFile.name}")
            Toast.makeText(this, "PDF generated successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            setStatus("Error generating PDF: ${e.message}")
        } finally {
            document.close()
        }
    }

    private fun drawPdfContent(canvas: Canvas, title: String, body: String) {
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 28f
            isFakeBoldText = true
        }

        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 16f
        }

        canvas.drawText(title, 40f, 80f, titlePaint)

        val maxWidth = 510f
        var y = 130f
        body.split("\n").forEach { paragraph ->
            wrapText(paragraph, bodyPaint, maxWidth).forEach { line ->
                canvas.drawText(line, 40f, y, bodyPaint)
                y += 24f
            }
            y += 12f
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        val current = StringBuilder()

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "${current} $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current.clear()
                current.append(candidate)
            } else {
                lines.add(current.toString())
                current.clear()
                current.append(word)
            }
        }

        if (current.isNotEmpty()) {
            lines.add(current.toString())
        }

        return lines
    }

    private fun setStatus(message: String) {
        tvStatus.text = "Status: $message"
    }
}
