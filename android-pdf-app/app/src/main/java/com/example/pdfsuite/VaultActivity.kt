package com.example.pdfsuite

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.pdfsuite.vault.CryptoManager
import java.io.File

class VaultActivity : AppCompatActivity() {

    private val files = mutableListOf<File>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var status: TextView

    private var pin = "1234"
    private var unlocked = false

    private val pickPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && unlocked) {
            encryptToVault(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vault)

        val etPin = findViewById<EditText>(R.id.etVaultPin)
        status = findViewById(R.id.tvVaultStatus)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        val list = findViewById<ListView>(R.id.vaultList)
        list.adapter = adapter

        findViewById<Button>(R.id.btnSetPin).setOnClickListener {
            val p = etPin.text.toString().trim()
            if (p.length >= 4) {
                pin = p
                status.text = "PIN updated"
            }
        }

        findViewById<Button>(R.id.btnUnlock).setOnClickListener {
            unlocked = etPin.text.toString() == pin
            status.text = if (unlocked) "Vault unlocked" else "Invalid PIN"
        }

        findViewById<Button>(R.id.btnImportVault).setOnClickListener {
            if (!unlocked) {
                status.text = "Unlock vault first"
                return@setOnClickListener
            }
            pickPdf.launch(arrayOf("application/pdf"))
        }

        list.setOnItemClickListener { _, _, position, _ ->
            if (!unlocked) {
                status.text = "Unlock vault first"
                return@setOnItemClickListener
            }

            val encFile = files[position]
            val out = File(cacheDir, "preview_${encFile.nameWithoutExtension}.pdf")
            CryptoManager.decryptFile(encFile, out, pin)
            status.text = "Decrypted to volatile cache: ${out.name}"
        }

        refresh()
    }

    private fun encryptToVault(uri: Uri) {
        val input = contentResolver.openInputStream(uri) ?: return
        val bytes = input.readBytes()
        val plain = File(cacheDir, "temp_plain.pdf")
        plain.writeBytes(bytes)

        val out = File(vaultDir(), "vault_${System.currentTimeMillis()}.enc")
        CryptoManager.encryptFile(plain, out, pin)
        plain.delete()

        status.text = "Encrypted with AES-256 (GCM) into Cyber-Vault"
        refresh()
    }

    private fun refresh() {
        files.clear()
        files += vaultDir().listFiles()?.toList().orEmpty()
        adapter.clear()
        adapter.addAll(files.map { it.name })
        adapter.notifyDataSetChanged()
    }

    private fun vaultDir(): File {
        val dir = File(filesDir, "cyber_vault")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
