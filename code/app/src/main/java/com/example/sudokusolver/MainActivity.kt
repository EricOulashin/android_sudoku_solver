package com.example.sudokusolver

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var mainLayout: LinearLayout
    private lateinit var puzzleGrid: TableLayout
    private lateinit var cameraMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.preview_view)
        mainLayout = findViewById(R.id.linearLayout_main)
        puzzleGrid = findViewById(R.id.tableLayout_puzzle)
        cameraMessage = findViewById(R.id.camera_message)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // TODO: Improve random puzzle generation
        //mBtnRandomPuzzle = findViewById<Button>(R.id.btn_random_puzzle)
        mBtnLoad = findViewById<Button>(R.id.btn_load_puzzle)
        mBtnSolve = findViewById<Button>(R.id.btn_solve)
        mBtnClear = findViewById<Button>(R.id.btn_clear)
        mBtnScan = findViewById<Button>(R.id.btn_photo_scan)
        mBtnPrevSolution = findViewById<Button>(R.id.btn_prev_solution)
        mBtnNextSolution = findViewById<Button>(R.id.btn_next_solution)
        mSolutionNumInput = findViewById<EditText>(resources.getIdentifier("editText_solution_num", "id", getPackageName()))
        mStatusText = findViewById(R.id.textView_status)

        /*
        mBtnRandomPuzzle.setOnClickListener {
            mSolver.genRandomPuzzle()
            for (row in 1..9) {
                for (col in 1..9) {
                    setGridInputVal(row, col, mSolver.getVal(row-1, col-1))
                }
            }
        }
        */

        mBtnLoad.setOnClickListener {
            val intent = Intent()
                .setType("text/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent, "Select a file"), PUZZLE_FILE_CHOSEN_VAL)
        }

        mBtnSolve.setOnClickListener {
            // Make sure the puzzle on the screen isn't all zeroes before we try to solve it
            if (!puzzleAllZeros()) {
                mSolver.reset()
                for (row in 1..9) {
                    for (col in 1..9)
                        mSolver.setVal(row - 1, col - 1, getGridInputVal(row, col))
                }
                toggleUIInputsEnabled(false)
                setStatusText("Solving...")
                thread(start = true) {
                    // Returns a thread object if we'd need to do anything with it
                    mSolver.solve()
                    // Communicate with the UI thread when it's done solving
                    mHandler.obtainMessage(DONE_SOLVING_MSG_VAL).apply { sendToTarget() }
                }
            }
            else {
                var errorMsg = "Can't solve: The puzzle is all zeros"
                val toast = Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_SHORT)
                toast.show()
            }
        }

        mBtnClear.setOnClickListener {
            setStatusText("")
            for (row in 1..9) {
                for (col in 1..9) {
                    val inputName: String = "editText_row" + row.toString() + "_text" + col.toString()
                    val textInput: EditText = findViewById<EditText>(resources.getIdentifier(inputName, "id", getPackageName()))
                    textInput.setText("")
                }
            }
            mSolutionNumInput.setText("")
            toggleUIInputsEnabled(true)
        }

        mBtnScan.setOnClickListener {
            if (previewView.visibility == View.GONE) {
                mainLayout.visibility = View.GONE
                previewView.visibility = View.VISIBLE
                cameraMessage.visibility = View.VISIBLE
            } else {
                mainLayout.visibility = View.VISIBLE
                previewView.visibility = View.GONE
                cameraMessage.visibility = View.GONE
            }
        }

        mBtnPrevSolution.setOnClickListener {
            // Note: The number in the solution # input is 1-based, and the solution index is 0-based
            var solutionIdx: Int = mSolutionNumInput.text.toString().toInt() - 2
            if (setSolutionOnBoard(solutionIdx)) {
                mSolutionNumInput.setText((solutionIdx + 1).toString())
                mBtnNextSolution.isEnabled = true
                if (solutionIdx == 0)
                    mBtnPrevSolution.isEnabled = false
            }
            else
                mBtnPrevSolution.isEnabled = false
        }


        mBtnNextSolution.setOnClickListener {
            // Note: The number in the solution # input is 1-based, and the solution index is 0-based
            var solutionIdx: Int = mSolutionNumInput.text.toString().toInt()
            if (setSolutionOnBoard(solutionIdx)) {
                mSolutionNumInput.setText((solutionIdx + 1).toString())
                mBtnPrevSolution.isEnabled = true
                if (solutionIdx == mSolver.numSolutions() - 1)
                    mBtnNextSolution.isEnabled = false
            }
            else
                mBtnNextSolution.isEnabled = false
        }

        mSolutionNumInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                if (s.length > 0) {
                    val solutionIdx: Int = s.toString().toInt() - 1
                    if (!setSolutionOnBoard(solutionIdx)) {
                        var errorMsg = "Invalid solution number: " + (solutionIdx + 1).toString()
                        val toast = Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_SHORT)
                        toast.show()
                    }
                }
            }
        })

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Selected a file to load
        if ((requestCode == PUZZLE_FILE_CHOSEN_VAL) && (resultCode == RESULT_OK)) {
            var loadedSuccessfully: Boolean = true
            val selectedFilename = data?.data //The uri with the location of the file
            if (selectedFilename != null) {
                var rowNum: Int = 1
                contentResolver.openInputStream(selectedFilename)?.bufferedReader()?.forEachLine {
                    if (it.length >= 9) {
                        // Ignore lines starting with # as comment lines
                        if (it[0] != '#') {
                            for (charIdx in 0..it.length - 1) {
                                if (it[charIdx].isDigit()) {
                                    val valStr: String = it[charIdx].toString()
                                    if (!setGridInputVal(rowNum, charIdx + 1, valStr.toInt())) {
                                        var errorMsg = "Invalid value.  Row, col, value: " + rowNum.toString() + ", " + (charIdx + 1).toString() + ", " + valStr
                                        val toast = Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_SHORT)
                                        toast.show()
                                        loadedSuccessfully = false
                                        break
                                    }
                                }
                                else {
                                    val inputName: String = "editText_row" + rowNum.toString() + "_text" + (charIdx + 1).toString()
                                    val textInput: EditText = findViewById<EditText>(resources.getIdentifier(inputName, "id", getPackageName()))
                                    textInput.setText("")
                                }
                            }
                            ++rowNum
                        }
                    }
                }
            }
            else {
                val msg = "Null filename data received!"
                val toast = Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG)
                toast.show()
                loadedSuccessfully = false
            }

            if (loadedSuccessfully)
                setStatusText("Loaded successfully")
            else
                setStatusText("Error loading puzzle")
        }
    }

    fun getGridInputVal(pRow: Int, pCol: Int): Int {
        if ((pRow < 1) || (pRow > 9))
            return 0
        if ((pCol < 1) || (pCol > 9))
            return 0

        val inputName: String = "editText_row" + pRow.toString() + "_text" + pCol.toString()
        val textInput: EditText = findViewById<EditText>(resources.getIdentifier(inputName, "id", getPackageName()))
        val textValStr = textInput.text.toString()
        return if (textValStr.length == 0) 0 else textValStr.toInt()
    }

    fun setGridInputVal(pRow: Int, pCol: Int, pVal: Int): Boolean {
        if ((pRow < 1) || (pRow > 9))
            return false
        if ((pCol < 1) || (pCol > 9))
            return false
        if ((pVal < 0) || (pVal > 9)) // Allow 0 to clear the cell
            return false

        val inputName: String = "editText_row" + pRow.toString() + "_text" + pCol.toString()
        val textInput: EditText = findViewById<EditText>(resources.getIdentifier(inputName, "id", getPackageName()))
        textInput.setText(if (pVal == 0) "" else pVal.toString())
        return true
    }

    fun setStatusText(pStatus: String) {
        mStatusText.setText(pStatus)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, SudokuImageAnalyzer(this))
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    //private lateinit var mBtnRandomPuzzle: Button
    private lateinit var mBtnLoad: Button
    private lateinit var mBtnSolve: Button
    private lateinit var mBtnClear: Button
    private lateinit var mBtnScan : Button
    private lateinit var mBtnPrevSolution: Button
    private lateinit var mBtnNextSolution: Button
    private lateinit var mSolutionNumInput: EditText
    private lateinit var mStatusText: TextView
    private var mSolver: SudokuSolver = SudokuSolver()

    private val PUZZLE_FILE_CHOSEN_VAL: Int = 111
    private val DONE_SOLVING_MSG_VAL: Int = 112
    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    private val TAG = "SudokuSolver"

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        /*
         * handleMessage() defines the operations to perform when
         * the Handler receives a new Message to process.
         */
        override fun handleMessage(inputMessage: Message) {
            // Gets the image task from the incoming Message object.
            //val photoTask = inputMessage.obj as PhotoTask
            if (inputMessage.what == DONE_SOLVING_MSG_VAL) {
                toggleUIInputsEnabled(true)
                var statusText = "Done solving.  "
                if (mSolver.numSolutions() == 0)
                    statusText += "No solutions were found."
                else if (mSolver.numSolutions() == 1)
                    statusText += "1 solution was found."
                else
                    statusText += "There are " + mSolver.numSolutions().toString() + " solutions."
                setStatusText(statusText)
                mSolutionNumInput.filters = arrayOf(InputFilterMinMax(1, mSolver.numSolutions()))
                if (mSolver.numSolutions() > 0) {
                    setSolutionOnBoard(0)
                    mSolutionNumInput.setText("1")
                    mBtnPrevSolution.isEnabled = false
                    if (mSolver.numSolutions() == 1)
                        mBtnNextSolution.isEnabled = false
                }
                else {
                    mSolutionNumInput.setText("")
                    mBtnPrevSolution.isEnabled = false
                    mBtnNextSolution.isEnabled = false
                }
            }
        }
    }

    private fun setSolutionOnBoard(pSolutionIdx: Int): Boolean {
        var retVal: Boolean = true
        if ((pSolutionIdx >= 0) && (pSolutionIdx < mSolver.numSolutions())) {
            try {
                val solution: SudokuSolution = mSolver.getSolution(pSolutionIdx)
                for (row in 1..9) {
                    for (col in 1..9)
                        setGridInputVal(row, col, solution.getVal(row - 1, col - 1))
                }
            }
            catch (e: IndexOutOfBoundsException) {
                retVal = false
            }
        }
        else
            retVal = false

        return retVal
    }

    private fun toggleUIInputsEnabled(pEnabled: Boolean) {
        mBtnLoad.isEnabled = pEnabled
        mBtnSolve.isEnabled = pEnabled
        mBtnClear.isEnabled = pEnabled
        mBtnScan.isEnabled = pEnabled
        mBtnPrevSolution.isEnabled = pEnabled
        mBtnNextSolution.isEnabled = pEnabled
        mSolutionNumInput.isEnabled = pEnabled
    }

    private fun puzzleAllZeros(): Boolean {
        var retVal: Boolean = true
        for (row in 1..9) {
            for (col in 1..9) {
                if (getGridInputVal(row, col) != 0) {
                    retVal = false
                    break
                }
            }
        }
        return retVal
    }

    private class SudokuImageAnalyzer(private val activity: MainActivity) : ImageAnalysis.Analyzer {

        private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        activity.runOnUiThread {
                            val puzzleGrid = activity.puzzleGrid
                            val gridWidth = puzzleGrid.width
                            val gridHeight = puzzleGrid.height
                            val cellWidth = gridWidth / 9
                            val cellHeight = gridHeight / 9

                            // Create a 9x9 grid to store the average digit per cell
                            val cellDigits = Array(9) { Array(9) { mutableListOf<Int>() } }

                            for (block in visionText.textBlocks) {
                                for (line in block.lines) {
                                    for (element in line.elements) {
                                        if (element.text.length == 1 && element.text[0].isDigit()) {
                                            val digit = element.text.toInt()
                                            val boundingBox = element.boundingBox
                                            if (boundingBox != null) {
                                                val centerX = boundingBox.centerX()
                                                val centerY = boundingBox.centerY()

                                                val col = (centerX / cellWidth).toInt()
                                                val row = (centerY / cellHeight).toInt()

                                                if (row in 0..8 && col in 0..8) {
                                                    cellDigits[row][col].add(digit)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Now, find the most likely digit for each cell
                            for (row in 0..8) {
                                for (col in 0..8) {
                                    if (cellDigits[row][col].isNotEmpty()) {
                                        val mostCommonDigit = cellDigits[row][col].groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
                                        if (mostCommonDigit != null) {
                                            activity.setGridInputVal(row + 1, col + 1, mostCommonDigit)
                                        }
                                    }
                                }
                            }
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener {
                        imageProxy.close()
                    }
            }
        }
    }
}
