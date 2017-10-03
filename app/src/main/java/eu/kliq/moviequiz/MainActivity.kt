package eu.kliq.moviequiz

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private var isWaiting = true
    private lateinit var mButtons: MutableList<Button>
    private val CORRECT_ANSWER_COLOR = 0xFF00FF00.toInt()
    private val WRONG_ANSWER_COLOR = 0xFFFF0000.toInt()
    private val mQuestionManager: QuestionManager = QuestionManager
    private var startTime: Long = 0
    private var mHiScore = 0
    private val HI_SCORE_KEY = "hi_score_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addButtons()
        refreshScoreText()
        getHiScore()
        overlay_layout.setOnClickListener({restartGame()})
        checkConnectionAndInit()
    }

    private fun addButtons() {
        mButtons = ArrayList()
        for (iteration in 0 until QuestionManager.BUTTONS) {
            val button = Button(this)
            button.setOnClickListener({
                if (!isWaiting) selectAnswer(iteration)
            })
            button_layout.addView(button)
            mButtons.add(button)
        }
    }

    private fun checkConnectionAndInit() {
        val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            mQuestionManager.initialize {generateQuestion()}
        }
    }

    private fun setUiForCurrentQuestion(bitmap: Bitmap) {
        Log.d(TAG, "setUiForCurrentQuestion()")
        // Set image
        movie_img.setImageBitmap(bitmap)
        // Set buttons
        for (iteration in 0 until QuestionManager.BUTTONS) {
            val button = mButtons[iteration]
            button.text = mQuestionManager.getCurrentQuestionMovieTitle(iteration)
            button.background.colorFilter = null
        }
        startTime =  System.currentTimeMillis()
        refreshRoundText()
        setWaitingState(false)
    }

    private fun selectAnswer(answerId: Int) {
        Log.d(TAG, "selectAnswer($answerId)")
        val time = System.currentTimeMillis() - startTime
        val isCorrect = mQuestionManager.onAnswer(answerId, time)
        val color = if (isCorrect) CORRECT_ANSWER_COLOR else WRONG_ANSWER_COLOR
        mButtons[answerId].background.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        refreshScoreText()
        if (isCorrect) generateQuestion() else finishGame()
    }

    private fun finishGame() {
        overlay_layout.visibility = View.VISIBLE
        game_over_layout.visibility = View.VISIBLE
        saveHiScore()
    }

    private fun restartGame() {
        overlay_layout.visibility = View.GONE
        game_over_layout.visibility = View.GONE
        mQuestionManager.restartGame()
        refreshScoreText()
        generateQuestion()
    }

    private fun refreshScoreText() {
        score_text.text = getString(R.string.score, mQuestionManager.score)
    }

    private fun refreshRoundText() {
        round_text.text = getString(R.string.round, mQuestionManager.currentQuestion)
    }

    private fun refreshHiScoreText() {
        hi_score_text.text = getString(R.string.hi_score, mHiScore)
    }

    private fun generateQuestion() {
        setWaitingState(true)
        mQuestionManager.generateQuestion { bitmap -> setUiForCurrentQuestion(bitmap) }
    }

    private fun setWaitingState(state: Boolean) {
        overlay_layout.visibility = if (state) View.VISIBLE else View.GONE
        waiting_progressbar.visibility = if (state) View.VISIBLE else View.GONE
        isWaiting = state
    }

    private fun getHiScore() {
        mHiScore = getPreferences(Context.MODE_PRIVATE).getInt(HI_SCORE_KEY, 0)
        refreshHiScoreText()
    }

    private fun saveHiScore() {
        if (mQuestionManager.score > mHiScore) {
            mHiScore = mQuestionManager.score
            val sharedPref = getPreferences(Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putInt(HI_SCORE_KEY, mHiScore)
            editor.apply()
            refreshHiScoreText()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mQuestionManager.restartGame()
    }
}
