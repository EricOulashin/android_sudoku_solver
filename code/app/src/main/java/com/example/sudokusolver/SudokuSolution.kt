package com.example.sudokusolver

class SudokuSolution {
    class SudokuSolution constructor() {

    }

    public fun setVal(pRow: Int, pCol: Int, pVal: Int) {
        mPuzzle[pRow][pCol] = pVal
    }

    public fun getVal(pRow: Int, pCol: Int): Int {
        return mPuzzle[pRow][pCol]
    }

    private var mPuzzle = arrayOf(intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0))
}