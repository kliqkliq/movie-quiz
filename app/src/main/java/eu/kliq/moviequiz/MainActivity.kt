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

import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private var isWaiting = true
    private lateinit var mButtons: MutableList<Button>
    private val CORRECT_ANSWER_COLOR = 0xFF00FF00.toInt()
    private val WRONG_ANSWER_COLOR = 0xFFFF0000.toInt()
    private val mQuestionManager: QuestionManager = QuestionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addButtons()
        checkConnectionAndInit()
    }

    private fun addButtons() {
        mButtons = ArrayList()
        for (iteration in 0 until QuestionManager.ANSWERS) {
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
        for (iteration in 0 until QuestionManager.ANSWERS) {
            val button = mButtons[iteration]
            button.text = mQuestionManager.getCurrentQuestionMovieTitle(iteration)
            button.background.colorFilter = null
        }
        setWaitingState(false)
    }

    private fun selectAnswer(answerId: Int) {
        Log.d(TAG, "selectAnswer($answerId)")
        val color: Int
        if (answerId == mQuestionManager.correctAnswer) {
            Log.d(TAG, "correct answer")
            color = CORRECT_ANSWER_COLOR
        } else {
            Log.d(TAG, "wrong answer")
            color = WRONG_ANSWER_COLOR
        }
        mButtons[answerId].background.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        generateQuestion()
    }

    private fun generateQuestion() {
        setWaitingState(true)
        mQuestionManager.generateQuestion { bitmap -> setUiForCurrentQuestion(bitmap) }
    }

    private fun setWaitingState(state: Boolean) {
        waiting_layout.visibility = if (state) View.VISIBLE else View.INVISIBLE
        isWaiting = state
    }
}
