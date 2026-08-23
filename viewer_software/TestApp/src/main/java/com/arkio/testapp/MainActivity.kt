package com.arkio.testapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arkio.officeengine.ArkioOfficeEngine
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val engine by lazy { ArkioOfficeEngine(this) }
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var pickButton: Button
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        
        layout.addView(TextView(this).apply { text = "ArkioOfficeEngine Test"; textSize = 24f; setPadding(0, 0, 0, 32) })
        pickButton = Button(this).apply { text = "Pick Office File" }
        progressBar = ProgressBar(this).apply { visibility = View.GONE }
        statusText = TextView(this).apply { text = "Pick a .docx, .xlsx, or .pptx file to test"; textSize = 14f; setPadding(0, 16, 0, 16) }
        resultText = TextView(this).apply { text = ""; textSize = 12f }
        
        layout.addView(pickButton); layout.addView(progressBar); layout.addView(statusText); layout.addView(resultText)
        setContentView(layout)
        
        pickButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/msword", "application/vnd.ms-excel", "application/vnd.ms-powerpoint"
                ))
            }
            startActivityForResult(intent, 1001)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) data?.data?.let { processFile(it) }
    }

    private fun processFile(uri: Uri) {
        pickButton.isEnabled = false; progressBar.visibility = View.VISIBLE; statusText.text = "Converting..."; resultText.text = ""
        lifecycleScope.launch {
            try {
                val fileName = getFileName(uri) ?: "temp_file.docx"
                val tempFile = File(cacheDir, fileName)
                contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(tempFile).use { output -> input.copyTo(output) } }
                statusText.text = "Processing: $fileName"
                val startTime = System.currentTimeMillis()
                val result = engine.convertToPdf(tempFile.absolutePath)
                val elapsed = System.currentTimeMillis() - startTime
                progressBar.visibility = View.GONE; pickButton.isEnabled = true
                if (result.success) {
                    val pdfFile = File(result.outputPdfPath!!)
                    statusText.text = "✅ SUCCESS"
                    resultText.text = "File: $fileName\nPages: ${result.pageCount}\nFormat: ${result.originalFormat}\nTime: ${elapsed}ms\nSize: ${pdfFile.length() / 1024}KB\nPath: ${result.outputPdfPath}"
                    openPdf(result.outputPdfPath!!)
                } else {
                    statusText.text = "❌ FAILED"
                    resultText.text = "Error: ${result.error?.code}\nMessage: ${result.error?.message}"
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE; pickButton.isEnabled = true; statusText.text = "❌ Exception"; resultText.text = e.message
            }
        }
    }

    private fun openPdf(pdfPath: String) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", File(pdfPath))
            startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Open PDF"))
        } catch (e: Exception) { Toast.makeText(this, "PDF created but no viewer installed", Toast.LENGTH_LONG).show() }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { if (it.moveToFirst()) { val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (idx >= 0) name = it.getString(idx) } }
        return name ?: uri.lastPathSegment
    }
}
