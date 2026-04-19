package com.example.pdfsuite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var pdfList: ListView
    private lateinit var status: TextView
    private val pdfUris = mutableListOf<Uri>()
    private lateinit var adapter: ArrayAdapter<String>

    private val openSinglePdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { openReader(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pdfList = findViewById(R.id.pdfList)
        status = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnScanner).setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }
        findViewById<Button>(R.id.btnReader).setOnClickListener {
            openSinglePdf.launch(arrayOf("application/pdf"))
        }
        findViewById<Button>(R.id.btnBrain).setOnClickListener {
            startActivity(Intent(this, BrainLinkActivity::class.java))
        }
        findViewById<Button>(R.id.btnVault).setOnClickListener {
            startActivity(Intent(this, VaultActivity::class.java))
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        pdfList.adapter = adapter
        pdfList.setOnItemClickListener { _, _, position, _ ->
            openReader(pdfUris[position])
        }

        ensurePermissionsAndLoad()
    }

    override fun onResume() {
        super.onResume()
        loadPdfIndex()
    }

    private fun ensurePermissionsAndLoad() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissions += Manifest.permission.READ_EXTERNAL_STORAGE
        } else {
            permissions += Manifest.permission.READ_MEDIA_IMAGES
            permissions += Manifest.permission.READ_MEDIA_VIDEO
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 77)
        } else {
            loadPdfIndex()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 77) {
            loadPdfIndex()
        }
    }

    private fun loadPdfIndex() {
        val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.TITLE)
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE}=?"
        val args = arrayOf("application/pdf")
        val sort = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        val names = mutableListOf<String>()
        pdfUris.clear()

        val collection = MediaStore.Files.getContentUri("external")
        contentResolver.query(collection, projection, selection, args, sort)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleCol) ?: "Untitled.pdf"
                val uri = Uri.withAppendedPath(collection, id.toString())
                pdfUris += uri
                names += title
            }
        }

        adapter.clear()
        adapter.addAll(names)
        adapter.notifyDataSetChanged()

        status.text = "Indexed PDFs: ${names.size}"
        if (names.isEmpty()) {
            Toast.makeText(this, "No PDFs indexed. Use Reader button to pick manually.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun openReader(uri: Uri) {
        val intent = Intent(this, ReaderActivity::class.java).putExtra("pdf_uri", uri.toString())
        startActivity(intent)
    }
}
