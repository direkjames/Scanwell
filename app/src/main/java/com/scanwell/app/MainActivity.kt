package com.scanwell.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.scanwell.app.pdf.ExportQuality
import com.scanwell.app.pdf.Protection
import com.scanwell.app.ui.ScanwellApp
import com.scanwell.app.ui.theme.ScanwellTheme
import com.scanwell.app.util.Names
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * The app's only window. It shows the screens (see the ui folder) and talks to
 * the two system screens the app borrows: Google's document scanner and the
 * "choose where to save" file picker.
 */
class MainActivity : ComponentActivity() {

    // The view model is built here directly instead of by Android's automatic lookup.
    // That lookup finds the constructor by name, which breaks once the release build
    // shrinks and renames the code (the cause of the crash on launch in release APKs).
    private val viewModel: MainViewModel by viewModels {
        viewModelFactory { initializer { MainViewModel(application) } }
    }

    /** Opens Google's scanner and receives the scanned pages when it closes. */
    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                val uris = scan?.pages?.map { page -> page.imageUri } ?: emptyList()
                viewModel.onScanFinished(uris)
            }
        }

    /** Opens the system's "save as" screen and receives the chosen location. */
    private val savePdfLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            if (uri != null) viewModel.savePdfTo(uri) else viewModel.pendingSave = null
        }

    /** Lets the person pick a PDF from anywhere on the phone for the PDF tools. */
    private val pickPdfLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.pickPdf(uri)
        }

    /** "Save as" screen for the result of a PDF tool. */
    private val saveToolResultLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            if (uri != null) viewModel.saveToolResultTo(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Shortcuts.install(this)

        setContent {
            ScanwellTheme {
                ScanwellApp(
                    viewModel = viewModel,
                    onScan = { target -> startScan(target) },
                    onSavePdf = { documentId, quality, protection -> savePdf(documentId, quality, protection) },
                    onPickPdf = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                    onSaveToolResult = { name -> saveToolResultLauncher.launch(name) },
                )
            }
        }

        // Opened from the "Scan" shortcut: go straight to the camera.
        if (savedInstanceState == null && intent?.action == Shortcuts.ACTION_SCAN) {
            startScan(ScanTarget.NewDocument)
        }
    }

    private fun startScan(target: ScanTarget) {
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        if (target == ScanTarget.IdCard) options.setPageLimit(2) // front and back

        viewModel.pendingScan = target
        try {
            GmsDocumentScanning.getClient(options.build())
                .getStartScanIntent(this)
                .addOnSuccessListener { intentSender ->
                    scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener { error ->
                    viewModel.showMessage("The scanner could not start. ${error.message ?: ""}".trim())
                }
        } catch (error: Exception) {
            viewModel.showMessage("The scanner could not start. ${error.message ?: ""}".trim())
        }
    }

    private fun savePdf(documentId: String, quality: ExportQuality, protection: Protection?) {
        val document = viewModel.findDocument(documentId) ?: return
        viewModel.pendingSave = SaveRequest(documentId, quality, protection)
        savePdfLauncher.launch(Names.safeFileName(document.title) + ".pdf")
    }
}
