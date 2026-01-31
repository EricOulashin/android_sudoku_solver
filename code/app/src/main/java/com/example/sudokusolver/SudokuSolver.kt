package com.example.sudokusolver

import java.lang.IndexOutOfBoundsException
import kotlin.random.Random

// See this page for an example in Kotlin:
// https://rosettacode.org/wiki/Sudoku#Kotlin

class SudokuSolver {
    class SudokuSolver constructor() {
    }

    public fun setVal(pRow: Int, pCol: Int, pVal: Int) {
        mPuzzle[pRow][pCol] = pVal
    }

    public fun getVal(pRow: Int, pCol: Int): Int {
        return mPuzzle[pRow][pCol]
    }

    public fun solve() {
        solve(0, 0)
    }

    public fun numSolutions(): Int {
        return mSolutions.size
    }

    public fun getSolution(pSolutionIdx: Int): SudokuSolution {
        return mSolutions[pSolutionIdx]
        /*
        if ((pSolutionIdx >= 0) && (pSolutionIdx < mSolutions.size))
            return mSolutions[pSolutionIdx]
        else
            throw IndexOutOfBoundsException("Invalid solution index: " + pSolutionIdx.toString())
        */
    }

    public fun reset() {
        for (row in 0..8) {
            for (col in 0..8) {
                mPuzzle[row][col] = 0
            }
        }
        mSolutions.clear()
    }

    public fun genRandomPuzzle() {
        reset()
        // Set 10 random values on the puzzle
        var row: Int
        var col: Int
        var randomVal: Int
        for (valNum in 1..30) {
            row = Random.nextInt(0, 9)
            col = Random.nextInt(0, 9)
            randomVal = Random.nextInt(1, 10)
            while (!valSafe(row, col, randomVal))
                randomVal = Random.nextInt(1, 10)
            mPuzzle[row][col] = randomVal
        }
        solve(0, 0)
        if (mSolutions.size > 0) {
            for (row in 0..8) {
                for (col in 0..8)
                    mPuzzle[row][col] = mSolutions[0].getVal(row, col)
            }
        }
    }

    /////////////////////
    // Private stuff

    // Solves the puzzle using a brute force algorithm.
    //
    // Parameters:
    //  pRow: The row index (0-based)
    //  pCol: The column index (0-based)
    private fun solve(pRow: Int, pCol: Int) {
        if (pRow == 9) {
            // Copy the solution from mPuzzle to a collection of solutions, if there is one
            mSolutions.add(SudokuSolution())
            var solutionIdx = mSolutions.size - 1
            for (row in 0..8) {
                for (col in 0..8)
                    mSolutions[solutionIdx].setVal(row, col, mPuzzle[row][col])
            }
        }
        else {
            for (value in 1..9) {
                if (valSafe(pRow, pCol, value)) {
                    var t = mPuzzle[pRow].get(pCol)
                    mPuzzle[pRow][pCol] = value
                    if (pCol == 8)
                        solve(pRow + 1, 0)
                    else
                        solve(pRow, pCol + 1)

                    mPuzzle[pRow][pCol] = t
                }
            }
        }
    }

    // Returns whether or not it's safe to place a value at a given puzzle cell.
    //
    // Parameters:
    //  pRow: The row index (0-based)
    //  pCol: The column index (0-based)
    //  pVal: The value to test
    private fun valSafe(pRow: Int, pCol: Int, pVal: Int): Boolean {
        if (mPuzzle[pRow][pCol] == pVal) return true;
        if (mPuzzle[pRow][pCol] != 0) return false;
        for (col in 0..8) {
            if (mPuzzle[pRow][col] == pVal) return false
        }
        for (row in 0..8) {
            if (mPuzzle[row][pCol] == pVal) return false
        }
        val br: Int = pRow / 3
        val bc: Int = pCol / 3
        var upperRowLimit: Int = ((br+1) * 3) - 1
        for (row in (br*3)..upperRowLimit) {
            var upperColLimit = ((bc+1) * 3) - 1
            for (col in (bc*3)..upperColLimit) {
                if (mPuzzle[row][col] == pVal) return false
            }
        }

        return true
    }

    private fun puzzleIsSolved(): Boolean {
        var isSolved: Boolean = true
        for (row in 0..8) {
            for (col in 0..8) {
                if (mPuzzle[row][col] == 0) {
                    isSolved = false
                    break
                }
            }
        }
        return isSolved
    }

    // Member variables/objects

    private var mPuzzle = arrayOf(intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0))
    private var mSolutions = mutableListOf<SudokuSolution>()
}