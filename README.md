# Android Sudoku solver
This is a Sudoku puzzle solver for Android.  I originally created this in November-December
2019; I've updated the build settings to be more recent; i.e., it can build for and run on
a Google Pixel 8 Pro, for instance.

In this app, there's a Sudoku board that you can type numbers into directly; you can also
load puzzles from text files.  The text file format is simple: Each line of the text file
corresponds to the line from the puzzle board, with dots representing empty cells.  The
first line can also be a comment starting with the # character.  You can copy puzzle files
onto your Android device and then load them from this app.  There is a set of pre-defined
puzzle files in the puzzles directory.

To solve Sudoku puzzles, I used a brute-force algorithm that was published online. I don't
quite remember where I saw it, but it looks like there are similar algorithms
<a href='https://codereview.stackexchange.com/questions/46640/logical-sudoku-solver-in-java' target='_blank'>here</a>.

In the 'puzzles' directory, the 'nearWorstCase.txt' puzzle is challenging for the algorithm.

I haven't submitted this to the Google Play store, as there are already other Sudoku solver
apps there, and I didn't want to add yet another one.  I mainly wanted to create a Sudoku
solver app myself, which doesn't have any ads, etc..

I used Android Studio to make this application and used Kotlin as the programming language.
Currently, it is set up for the following:
<ul>
<li><b>Compile SDK version</b>: 34
<li><b>Minimum SDK version: </b>: 21
<li><b>Target SDK version 34</b>: 
</ul>

# Screenshots
The following are screenshots of the app, with a puzzle typed in and after the puzzle is solved:

<p align="center">
  <img src="screenshots/SudokuSolver_Android_NewPuzzle.png" alt="New puzzle" style="width: 50%; height: auto;">
  <img src="screenshots/SudokuSolver_Android_Solved.png" alt="Puzzle solved" style="width: 50%; height: auto;">
</p>
