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
import android.widget.*
import java.io.File
import java.lang.IndexOutOfBoundsException
import kotlin.concurrent.thread

// TODO: Add Random Puzzle button
/*
mSolver.genRandomPuzzle()
for (row in 1..9) {
    for (col in 1..9) {
        setGridInputVal(row, col, mSolver.getVal(row-1, col-1))
    }
}
 */

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBtnLoad = findViewById<Button>(R.id.btn_load_puzzle)
        mBtnSolve = findViewById<Button>(R.id.btn_solve)
        mBtnClear = findViewById<Button>(R.id.btn_clear)
        mBtnPrevSolution = findViewById<Button>(R.id.btn_prev_solution)
        mBtnNextSolution = findViewById<Button>(R.id.btn_next_solution)
        mSolutionNumInput = findViewById<EditText>(resources.getIdentifier("editText_solution_num", "id", getPackageName()))
        mStatusText = findViewById(R.id.textView_status)

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
        if ((pVal < 1) || (pVal > 9))
            return false

        val inputName: String = "editText_row" + pRow.toString() + "_text" + pCol.toString()
        val textInput: EditText = findViewById<EditText>(resources.getIdentifier(inputName, "id", getPackageName()))
        textInput.setText(pVal.toString())
        return true
    }

    fun setStatusText(pStatus: String) {
        mStatusText.setText(pStatus)
    }

    private lateinit var mBtnLoad: Button
    private lateinit var mBtnSolve: Button
    private lateinit var mBtnClear: Button
    private lateinit var mBtnPrevSolution: Button
    private lateinit var mBtnNextSolution: Button
    private lateinit var mSolutionNumInput: EditText
    private lateinit var mStatusText: TextView
    private var mSolver: SudokuSolver = SudokuSolver()

    private val PUZZLE_FILE_CHOSEN_VAL: Int = 111
    private val DONE_SOLVING_MSG_VAL: Int = 112

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
}
