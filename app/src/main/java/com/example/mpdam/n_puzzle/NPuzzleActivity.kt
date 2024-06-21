package com.example.mpdam.n_puzzle

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewTreeObserver
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.*
import java.util.Collections.swap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class NPuzzleActivity : AppCompatActivity() {

    companion object {

        private const val NUM_COLUMNS = 3

        private const val NUM_TILES = NUM_COLUMNS * NUM_COLUMNS

        private const val BORDER_OFFSET = 6

        private const val BLANK_TILE_MARKER = NUM_TILES - 1

        private const val DEFAULT_FEWEST_MOVES = Long.MAX_VALUE

        private const val DEFAULT_FASTEST_TIME = Long.MAX_VALUE
    }

    private lateinit var clRoot: ConstraintLayout

    private lateinit var gvgPuzzle: GridViewGesture

    private lateinit var btnUpload: Button

    private lateinit var btnShuffle: Button

    private lateinit var pbShuffle: ProgressBar

    private lateinit var tvMoveNumber: TextView

    private lateinit var tvFewestMoves: TextView

    private lateinit var tvTimeTaken: TextView

    private lateinit var tvFastestTime: TextView

    private lateinit var spnPuzzle: Spinner

    private lateinit var tvTitle: TextView

    private lateinit var tvSuccess: TextView

    private lateinit var tvTrivia: TextView

    private lateinit var sp: SharedPreferences

    private var tileDimen: Int = 0

    private var puzzleDimen: Int = 0

    private lateinit var goalPuzzleState: ArrayList<Int>

    private lateinit var puzzleState: ArrayList<Int>

    private var blankTilePos: Int = BLANK_TILE_MARKER

    private var isPuzzleGridFrozen: Boolean = false

    private var isGameInSession: Boolean = false

    private lateinit var puzzleImage: Bitmap

    private lateinit var imageChunks: ArrayList<Bitmap>

    private lateinit var blankImageChunks: ArrayList<Bitmap>

    private lateinit var tileImages: ArrayList<ImageButton>

    private lateinit var puzzleImageChoices: Array<PuzzleImage>

    private var puzzleImageIndex: Int = 0

    private var indexOfCustom: Int = 0

    private var isGalleryImageChosen: Boolean = false

    private lateinit var shuffleRunnable: ShuffleRunnable

    private lateinit var shuffleScheduler: ScheduledExecutorService

    private lateinit var shuffleHandler: Handler

    private lateinit var timerHandler: Handler

    private var isTimerRunning: Boolean = false

    private var numMoves: Long = 0

    private var fewestMoves: Long = DEFAULT_FEWEST_MOVES

    private var timeTaken: Long = 0

    private var fastestTime: Long = DEFAULT_FASTEST_TIME

    private var puzzleSolution: Stack<StatePair>? = null

    private var numMovesSolution: Int = 0

    private lateinit var solveHandler: Handler

    private lateinit var solveDisplayHandler: Handler

    private var isSolutionDisplay: Boolean = false

    private var isSolutionPlay: Boolean = false

    private var isSolutionSkip: Boolean = false

    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_n_puzzle)

        /* Initialize all the necessary components, properties, etc. */
        initComponents()
        initSharedPreferences()
        initHandlers()
        initStateAndTileImages()
        initPuzzle()
        initGalleryLauncher()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, clRoot).let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onResume() {
        super.onResume()

        if (isGalleryImageChosen) {
            isGalleryImageChosen = false
        } else {
            spnPuzzle.setSelection(puzzleImageIndex)
        }
    }

    private fun initComponents() {

        clRoot = findViewById(R.id.cl_root)
        gvgPuzzle = findViewById(R.id.gvg_puzzle)

        btnShuffle = findViewById(R.id.btn_shuffle)
        setBtnShuffleAction()

        btnUpload = findViewById(R.id.btn_upload)
        setBtnUploadAction()

        pbShuffle = findViewById(R.id.pb_shuffle)

        tvMoveNumber = findViewById(R.id.tv_move_number)
        tvFewestMoves = findViewById(R.id.tv_fewest_moves)
        tvTimeTaken = findViewById(R.id.tv_time_taken)
        tvFastestTime = findViewById(R.id.tv_fastest_time)

        tvTitle = findViewById(R.id.tv_title)
        tvSuccess = findViewById(R.id.tv_success)
        tvSuccess.setOnClickListener {
            tvSuccess.visibility = View.GONE
        }

        tvTrivia = findViewById(R.id.tv_trivia)

        spnPuzzle = findViewById(R.id.spn_puzzle)
        spnPuzzle.adapter = SpinnerAdapter(
            this,
            R.layout.spn_puzzle_item,
            resources.getStringArray(R.array.puzzle_images)
        )

        puzzleImageChoices = PuzzleImage.values()
        indexOfCustom = puzzleImageChoices.lastIndex
    }

    private fun initSharedPreferences() {
        sp = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        fewestMoves =
            sp.getString(Key.KEY_FEWEST_MOVES.name, DEFAULT_FEWEST_MOVES.toString())?.toLong()
                ?: DEFAULT_FEWEST_MOVES
        fastestTime =
            sp.getString(Key.KEY_FASTEST_TIME.name, DEFAULT_FASTEST_TIME.toString())?.toLong()
                ?: DEFAULT_FASTEST_TIME

        displayStats()
    }

    private fun initHandlers() {

        shuffleScheduler = Executors.newScheduledThreadPool(NUM_TILES)
        shuffleHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                super.handleMessage(message)

                showTileAt(message.data.getInt(Key.KEY_TILE_POSITION.name))
                pbShuffle.progress = message.data.getInt(Key.KEY_PROGRESS.name)
                updateComponents()
            }
        }


        timerHandler = Handler(Looper.getMainLooper())


        solveHandler = Handler(Looper.getMainLooper())
        solveDisplayHandler = Handler(Looper.getMainLooper())
    }

    private fun initStateAndTileImages() {
        goalPuzzleState = ArrayList(NUM_TILES)
        puzzleState = ArrayList(NUM_TILES)
        tileImages = ArrayList(NUM_TILES)

        for (tile in 0 until NUM_TILES) {
            goalPuzzleState.add(tile)
            puzzleState.add(tile)

            tileImages.add(ImageButton(this))
        }
    }

    private fun resetState() {
        puzzleState = goalPuzzleState.toMutableList() as ArrayList<Int>
        blankTilePos = BLANK_TILE_MARKER
    }


    private fun initPuzzle() {
        setTouchSlopThreshold()
        setOnFlingListener()
        setDimensions()
    }


    private fun initGalleryLauncher() {
        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    loadPuzzle(result.data?.data)
                }
            }
    }

    private fun setBtnShuffleAction() {
        btnShuffle.setOnClickListener {
            if (isSolutionDisplay) {
                controlSolutionDisplay()
            } else if (!isGameInSession) {
                shuffle()
            } else {
                solve()
            }
        }
    }

    private fun setBtnUploadAction() {
        btnUpload.setOnClickListener {
            if (isSolutionDisplay) {
                skipSolution()
            } else {
                if (spnPuzzle.selectedItemPosition != indexOfCustom) {
                    spnPuzzle.setSelection(indexOfCustom)
                } else {
                    uploadPuzzleImage()
                }
            }
        }
    }

    private fun setSpnPuzzleAction() {
        spnPuzzle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position != indexOfCustom) {
                    loadPuzzle(position)
                } else {
                    uploadPuzzleImage()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun displayStats() {
        displayFewestMoves()
        displayFastestTime()
    }

    private fun displayFewestMoves() {
        tvFewestMoves.text = if (fewestMoves == DEFAULT_FEWEST_MOVES) {
            getString(R.string.default_move_count)
        } else {
            fewestMoves.toString()
        }
    }

    private fun displayFastestTime() {
        tvFastestTime.text = if (fastestTime == DEFAULT_FASTEST_TIME) {
            getString(R.string.default_timer)
        } else {
            TimeUtil.displayTime(fastestTime)
        }
    }

    private fun blankDisplayedStats() {
        /* Remove the statistics for the number of moves, and display them. */
        numMoves = 0
        tvMoveNumber.text = getString(R.string.default_move_count)

        /* Remove the statistics for the time taken, and display them. */
        timeTaken = 0
        tvTimeTaken.text = getString(R.string.default_timer)
    }

    private fun resetDisplayedStats() {
        /* Reset the statistics for the number of moves, and display them. */
        numMoves = 0
        tvMoveNumber.text = numMoves.toString()

        /* Reset the statistics for the time taken, and display them. */
        timeTaken = 0
        tvTimeTaken.text = TimeUtil.displayTime(timeTaken)
    }

    private fun setTouchSlopThreshold() {
        gvgPuzzle.setTouchSlopThreshold(ViewConfiguration.get(this).scaledTouchSlop)
    }

    private fun setOnFlingListener() {
        gvgPuzzle.setFlingListener(object : OnFlingListener {
            override fun onFling(direction: FlingDirection, position: Int) {
                moveTile(direction, position)
            }
        })
    }

    private fun setDimensions() {
        gvgPuzzle.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                gvgPuzzle.viewTreeObserver.removeOnGlobalLayoutListener(this)

                puzzleDimen = gvgPuzzle.measuredWidth
                tileDimen = puzzleDimen / NUM_COLUMNS

                setSpnPuzzleAction()

                initPuzzleImage()
                initChunks()
                displayPuzzle()
            }
        })
    }


    private fun initPuzzleImage() {
        /* Retrieve the most recently displayed puzzle image. */
        puzzleImageIndex = sp.getInt(Key.KEY_PUZZLE_IMAGE.name, 0)
        spnPuzzle.setSelection(puzzleImageIndex)

        puzzleImage = ImageUtil.resizeToSquareBitmap(
            ImageUtil.drawableToBitmap(
                this@NPuzzleActivity,
                puzzleImageChoices[puzzleImageIndex].drawableId
            ),
            puzzleDimen,
            puzzleDimen
        )
    }

    private fun initChunks() {
        imageChunks =
            ImageUtil.splitBitmap(
                puzzleImage,
                tileDimen - BORDER_OFFSET,
                NUM_TILES,
                NUM_COLUMNS
            ).first
        blankImageChunks =
            ImageUtil.splitBitmap(
                puzzleImage,
                tileDimen - BORDER_OFFSET,
                NUM_TILES,
                NUM_COLUMNS
            ).second
    }

    private fun displayPuzzle() {

        for ((position, tile) in puzzleState.withIndex()) {
            /* Properly reflect the blank tile depending on the puzzle state. */
            if (position == blankTilePos) {
                tileImages[blankTilePos].setImageBitmap(blankImageChunks[blankTilePos])
            } else {
                tileImages[position].setImageBitmap(imageChunks[tile])
            }
        }

        gvgPuzzle.adapter = TileAdapter(tileImages, tileDimen, tileDimen)
    }

    private fun displayBlankPuzzle() {

        for ((position, tile) in puzzleState.withIndex()) {
            if (position == blankTilePos) {
                tileImages[blankTilePos].setImageBitmap(blankImageChunks[blankTilePos])
            } else {
                tileImages[position].setImageBitmap(blankImageChunks[tile])
            }
        }

        gvgPuzzle.adapter = TileAdapter(tileImages, tileDimen, tileDimen)
    }

    private fun loadPuzzle(position: Int) {
        tvSuccess.visibility = View.GONE
        resetState()

        updatePuzzleImage(position)
        initChunks()
        displayPuzzle()
    }
    private fun updatePuzzleImage(position: Int) {
        puzzleImageIndex = position

        puzzleImage = ImageUtil.resizeToSquareBitmap(
            ImageUtil.drawableToBitmap(
                this@NPuzzleActivity, puzzleImageChoices[puzzleImageIndex].drawableId
            ),
            puzzleDimen,
            puzzleDimen
        )

        with(sp.edit()) {
            putInt(Key.KEY_PUZZLE_IMAGE.name, puzzleImageIndex)
            commit()
        }
    }

    private fun moveTile(direction: FlingDirection, position: Int) {
        var flag = false


        if (!isPuzzleGridFrozen) {
            if (MoveUtil.canMoveTile(direction, position, blankTilePos, NUM_COLUMNS)) {

                swap(puzzleState, position, blankTilePos)
                blankTilePos = position

                displayPuzzle()
                flag = updateGameStatus()

                if (numMoves == 1L) {
                    launchTimer()
                }
            }

            if (!flag) {
                tvSuccess.visibility = View.GONE
            }
        }
    }


    private fun updateGameStatus(): Boolean {
        if (isGameInSession) {
            trackMove()


            if (SolveUtil.isSolved(puzzleState, goalPuzzleState)) {

                timeTaken--

                if (numMoves < fewestMoves && timeTaken < fastestTime) {
                    endGame(SolveStatus.FEWEST_AND_FASTEST)
                } else if (numMoves < fewestMoves) {
                    endGame(SolveStatus.FEWEST_MOVES)
                } else if (timeTaken < fastestTime) {
                    endGame(SolveStatus.FASTEST_TIME)
                } else {
                    endGame(SolveStatus.USER_SOLVED)
                }

                return true
            }
        }

        return false
    }

    private fun trackMove() {
        numMoves++
        tvMoveNumber.text = numMoves.toString()
    }

    private fun launchTimer() {
        isTimerRunning = true

        timerHandler.post(object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    tvTimeTaken.text = TimeUtil.displayTime(timeTaken++)
                    timerHandler.postDelayed(this, TimeUtil.SECONDS_TO_MILLISECONDS.toLong())
                } else {
                    timerHandler.removeCallbacks(this)
                }
            }
        })
    }


    private fun shuffle() {
        pbShuffle.visibility = View.VISIBLE
        pbShuffle.progress = 0
        btnShuffle.text = getString(R.string.randomizing)


        btnUpload.visibility = View.INVISIBLE
        tvTrivia.visibility = View.VISIBLE
        tvTrivia.text = getString(R.string.trivia)

        tvSuccess.visibility = View.GONE
        disableClickables()
        resetDisplayedStats()
        getValidShuffledState()
        displayBlankPuzzle()
        startShowingTiles()
    }

    private fun getValidShuffledState() {
        val shuffledState: StatePair =
            ShuffleUtil.getValidShuffledState(puzzleState, goalPuzzleState, BLANK_TILE_MARKER)

        puzzleState = shuffledState.puzzleState
        blankTilePos = shuffledState.blankTilePos
    }

    private fun updateComponents() {
        when (pbShuffle.progress) {
            (NUM_TILES - 1) / 2 -> halfwayShuffling()
            (NUM_TILES - 1) -> finishShuffling()
        }
    }

    private fun halfwayShuffling() {
        btnShuffle.text = getString(R.string.inversions)
    }

    private fun finishShuffling() {
        isGameInSession = true

        tvTitle.setTextColor(
            ContextCompat.getColor(
                applicationContext,
                R.color.btn_first_variant
            )
        )

        btnUpload.setBackgroundColor(
            ContextCompat.getColor(
                applicationContext,
                R.color.btn_second_variant
            )
        )

        btnShuffle.setBackgroundColor(
            ContextCompat.getColor(
                applicationContext,
                R.color.btn_first_variant
            )
        )

        btnShuffle.text = getString(R.string.randomized)

        pbShuffle.visibility = View.GONE
        tvTrivia.text = getString(R.string.trivia_a_star)
        enableClickables()
    }

    private fun disableClickables() {
        isPuzzleGridFrozen = true
        btnShuffle.isEnabled = false
        spnPuzzle.isEnabled = false
    }

    private fun enableClickables() {
        isPuzzleGridFrozen = false
        btnShuffle.isEnabled = true
    }

    private fun showTileAt(position: Int) {
        tileImages[position].setImageBitmap(imageChunks[puzzleState[position]])

        /* Set (or reset) the adapter of the grid view. */
        gvgPuzzle.adapter = TileAdapter(tileImages, tileDimen, tileDimen)
    }

    private fun startShowingTiles() {
        for (position in 0 until tileImages.size) {
            if (position != blankTilePos) {

                val delay: Long =
                    ((0..AnimationUtil.SHUFFLING_ANIMATION_UPPER_BOUND).random()
                            + AnimationUtil.SHUFFLING_ANIMATION_OFFSET).toLong()

                shuffleRunnable = ShuffleRunnable(shuffleHandler, position, NUM_TILES)
                shuffleScheduler.schedule(shuffleRunnable, delay, TimeUnit.MILLISECONDS)
            }
        }
    }


    private fun solve() {
        puzzleSolution = SolveUtil.solve(
            StatePair(puzzleState, blankTilePos),
            goalPuzzleState,
            NUM_COLUMNS,
            BLANK_TILE_MARKER
        )
        blankDisplayedStats()

        displaySolution()

        endGame(SolveStatus.COMPUTER_SOLVED)
    }

    private fun displaySolution() {
        startSolution()

        puzzleSolution?.pop()!!
        numMovesSolution = puzzleSolution?.size!!

        animateSolution()
    }

    private fun controlSolutionDisplay() {
        if (isSolutionPlay) {
            pauseSolution()
        } else {
            resumeSolution()
        }
    }

    private fun startSolution() {
        isSolutionDisplay = true
        isSolutionPlay = true
        isPuzzleGridFrozen = true
    }

    private fun animateSolution() {
        Handler(Looper.getMainLooper()).postDelayed({
            solveDisplayHandler.post(object : Runnable {
                override fun run() {
                    if (puzzleSolution?.isNotEmpty()!! && isSolutionDisplay && isSolutionPlay) {
                        if (!isSolutionSkip) {
                            solveDisplayHandler.postDelayed(
                                this,
                                AnimationUtil.MOVE_SOLUTION_DELAY.toLong()
                            )
                        } else {
                            solveDisplayHandler.postDelayed(this, 0)
                        }

                        val puzzleStatePair: StatePair = puzzleSolution?.pop()!!
                        puzzleState = puzzleStatePair.puzzleState
                        blankTilePos = puzzleStatePair.blankTilePos

                        displayPuzzle()

                        if (puzzleSolution?.empty()!!) {
                            solveDisplayHandler.removeCallbacks(this)

                            endSolution()
                            displaySuccessMessage(SolveStatus.COMPUTER_SOLVED)
                            prepareForNewGame()
                        }
                    }
                }
            })
        }, AnimationUtil.FIRST_MOVE_SOLUTION_DELAY.toLong())
    }

    private fun endSolution() {
        isSolutionDisplay = false
        isSolutionPlay = false
        isPuzzleGridFrozen = false
        isSolutionSkip = false
    }

    private fun pauseSolution() {
        isSolutionPlay = false
        btnShuffle.text = getString(R.string.resume)
    }

    private fun resumeSolution() {
        isSolutionPlay = true
        btnShuffle.text = getString(R.string.pause)

        animateSolution()
    }

    private fun skipSolution() {
        isSolutionSkip = true
        resumeSolution()
    }

    private fun endGame(solveStatus: SolveStatus) {
        isGameInSession = false
        isTimerRunning = false
        if (solveStatus != SolveStatus.COMPUTER_SOLVED) {
            saveStats(solveStatus)
            displaySuccessMessage(solveStatus)
            prepareForNewGame()
        } else {
            prepareForSolution()
        }
    }


    private fun prepareForNewGame() {

        tvTitle.setTextColor(ContextCompat.getColor(applicationContext, R.color.btn_first))

        btnUpload.setBackgroundColor(
            ContextCompat.getColor(
                applicationContext,
                R.color.btn_second
            )
        )
        btnShuffle.setBackgroundColor(
            ContextCompat.getColor(
                applicationContext,
                R.color.btn_first
            )
        )
        btnShuffle.text = getString(R.string.new_game)
        btnUpload.visibility = View.VISIBLE
        btnUpload.text = getString(R.string.upload_picture)
        tvTrivia.visibility = View.GONE

        spnPuzzle.isEnabled = true
    }

    private fun prepareForSolution() {
        btnShuffle.text = getString(R.string.pause)

        btnUpload.visibility = View.VISIBLE
        btnUpload.text = getString(R.string.skip)
        tvTrivia.visibility = View.GONE
    }

    private fun saveStats(solveStatus: SolveStatus) {
        when (solveStatus) {
            SolveStatus.FEWEST_MOVES -> saveFewestMoves()
            SolveStatus.FASTEST_TIME -> saveFastestTime()
            SolveStatus.FEWEST_AND_FASTEST -> saveFewestAndFastest()
            else -> Unit
        }
    }

    private fun saveFewestAndFastest() {
        fewestMoves = numMoves
        tvFewestMoves.text = fewestMoves.toString()

        fastestTime = timeTaken
        tvFastestTime.text = TimeUtil.displayTime(fastestTime)


        with(sp.edit()) {
            putString(Key.KEY_FEWEST_MOVES.name, fewestMoves.toString())
            putString(Key.KEY_FASTEST_TIME.name, fastestTime.toString())
            apply()
        }
    }

    private fun saveFewestMoves() {
        fewestMoves = numMoves
        tvFewestMoves.text = fewestMoves.toString()

        with(sp.edit()) {
            putString(Key.KEY_FEWEST_MOVES.name, fewestMoves.toString())
            apply()
        }
    }

    private fun saveFastestTime() {
        fastestTime = timeTaken
        tvFastestTime.text = TimeUtil.displayTime(fastestTime)


        with(sp.edit()) {
            putString(Key.KEY_FASTEST_TIME.name, fastestTime.toString())
            apply()
        }
    }


    private fun displaySuccessMessage(solveStatus: SolveStatus) {
        tvSuccess.visibility = View.VISIBLE
        tvSuccess.text = getString(solveStatus.successMessageId)

        if (solveStatus == SolveStatus.COMPUTER_SOLVED) {
            var message = "$numMovesSolution ${tvSuccess.text}"

            if (numMovesSolution == 1) {
                message = message.substring(0, message.length - 1)
            }

            tvSuccess.text = message
        }

        /* Hide the success message after a set number of seconds. */
        Handler(Looper.getMainLooper()).postDelayed({
            tvSuccess.visibility = View.GONE
        }, AnimationUtil.SUCCESS_DISPLAY.toLong())
    }

    private fun uploadPuzzleImage() {
        UploadUtil.chooseFromGallery(this, galleryLauncher)
    }

    private fun loadPuzzle(imagePath: Uri?) {
        isGalleryImageChosen = true
        resetState()

        tvSuccess.visibility = View.GONE

        updatePuzzleImage(imagePath)
        initChunks()
        displayPuzzle()
    }

    private fun updatePuzzleImage(imagePath: Uri?) {
        puzzleImage = ImageUtil.resizeToSquareBitmap(
            BitmapFactory.decodeStream(
                contentResolver.openInputStream(imagePath!!)
            ),
            puzzleDimen,
            puzzleDimen
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsResult(grantResults)
    }
    private fun permissionsResult(grantResults: IntArray) {
        UploadUtil.permissionsResultGallery(grantResults, this@NPuzzleActivity, galleryLauncher)
    }
}