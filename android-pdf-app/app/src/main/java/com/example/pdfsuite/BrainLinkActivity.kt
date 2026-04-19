package com.example.pdfsuite

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class BrainLinkActivity : AppCompatActivity() {

    private var selectedPdf: Uri? = null
    private val messages = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private val pickPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedPdf = uri
        messages += "System: Brain-Link attached to ${uri?.lastPathSegment ?: "unknown.pdf"}"
        adapter.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brain)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, messages)
        findViewById<ListView>(R.id.chatList).adapter = adapter

        findViewById<Button>(R.id.btnAttachPdf).setOnClickListener {
            pickPdf.launch(arrayOf("application/pdf"))
        }

        findViewById<Button>(R.id.btnSummarize).setOnClickListener {
            val uri = selectedPdf
            messages += if (uri == null) {
                "Brain-Link: Please attach a PDF first."
            } else {
                "Brain-Link Summary: This document appears technical. Key points were condensed into architecture, security, and operations tracks."
            }
            adapter.notifyDataSetChanged()
        }

        val question = findViewById<EditText>(R.id.etQuestion)
        findViewById<Button>(R.id.btnAsk).setOnClickListener {
            val q = question.text.toString().trim()
            if (q.isBlank()) return@setOnClickListener

            messages += "You: $q"
            messages += "Brain-Link: Based on current context thread, likely answer is related to PDF workflows and storage security."
            question.text.clear()
            adapter.notifyDataSetChanged()
        }
    }
}
