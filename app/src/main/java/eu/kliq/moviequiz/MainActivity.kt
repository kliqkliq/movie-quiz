package eu.kliq.moviequiz

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout

import java.io.IOException
import java.net.URL
import java.util.ArrayList
import java.util.HashMap
import java.util.Random

import info.movito.themoviedbapi.TmdbApi
import info.movito.themoviedbapi.TmdbMovies
import info.movito.themoviedbapi.model.MovieDb
import info.movito.themoviedbapi.model.config.TmdbConfiguration
import info.movito.themoviedbapi.model.core.MovieResultsPage
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class MainActivity : AppCompatActivity(), View.OnClickListener {

    // TheMovieDB API objects
    private lateinit var mTmdbConfiguration: TmdbConfiguration
    private lateinit var mTmdbMovies: TmdbMovies

    private var isWaiting = true
    private var mCurrentPage = 0
    private var mCurrentQuestion = 0
    private var mCurrentMovieId = 0
    private var mCorrectAnswer = 0
    private lateinit var mCurrentImage: Bitmap
    private var mCurrentMovies: MutableMap<Int, Int> = HashMap()
    private lateinit var mBaseUrl: String
    private lateinit var mMovies: Map<Int, MovieDb>
    private var mRandom = Random()

    // UI
    private lateinit var mImageView: ImageView
    private lateinit var mButtons: MutableList<Button>
    private lateinit var mWaitingLayout: RelativeLayout

    private val TAG = "MainActivity"
    private val API_KEY = BuildConfig.TMDB_API_KEY
    private val BACKDROP_SIZE = "w1280"
    private val CORRECT_ANSWER_COLOR = 0xFF00FF00.toInt()
    private val WRONG_ANSWER_COLOR = 0xFFFF0000.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mWaitingLayout = findViewById(R.id.waiting_layout) as RelativeLayout
        mImageView = findViewById(R.id.movie_img) as ImageView
        mButtons = ArrayList()
        mButtons.add(findViewById(R.id.answer_button1) as Button)
        mButtons.add(findViewById(R.id.answer_button2) as Button)
        mButtons.add(findViewById(R.id.answer_button3) as Button)
        mButtons.add(findViewById(R.id.answer_button4) as Button)

        for (button in mButtons) {
            button.setOnClickListener(this)
        }

        val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            initialize()
        }
    }

    private fun initialize() {
        doAsync {
            mTmdbMovies = TmdbApi(API_KEY).movies
            mTmdbConfiguration = TmdbApi(API_KEY).configuration
            mBaseUrl = mTmdbConfiguration.baseUrl
            val resultsPage: MovieResultsPage = mTmdbMovies.getPopularMovieList("pl", mCurrentPage++)
            mMovies = resultsPage.results.associateBy({it.id}, {it})
            uiThread {
                generateQuestion()
            }
        }
    }

    private fun generateQuestion() {
        setWaitingState(true)
        mCurrentMovies.clear()
        mCurrentMovieId = randomMovieId
        getMovieImage(mCurrentMovieId)
        mCorrectAnswer = mRandom.nextInt(3)
        for (iteration in 0..3) {
            // If this iteration is the same as the correct answer, set the ID of the current movie
            val id = if (iteration == mCorrectAnswer) mCurrentMovieId else uniqueRandomMovieId
            mCurrentMovies.put(iteration, id)
        }
        mCurrentQuestion++
    }

    private val uniqueRandomMovieId: Int
        get() {
            var id: Int
            do {
                id = randomMovieId
            } while (mCurrentMovies.containsValue(id))
            return id
        }

    private val randomMovieId: Int
        get() {
            val randomEntry = mRandom.nextInt(mMovies.size - 1)
            return mMovies.keys.toTypedArray()[randomEntry]
        }

    private fun setUiForCurrentQuestion() {
        Log.d(TAG, "setUiForCurrentQuestion()")
        // Set image
        mImageView.setImageBitmap(mCurrentImage)
        // Set buttons
        for (iteration in 0..3) {
            val id = mCurrentMovies[iteration]
            val title = mMovies[id]?.title
            val button = mButtons[iteration]
            button.text = title
            button.background.colorFilter = null
        }
        setWaitingState(false)
    }

    private fun getMovieImage(id: Int) {
        Log.d(TAG, "getMovieImage() id: " + id)
        doAsync {
            val paths = mTmdbMovies.getImages(id, "").backdrops
                    .filter { it.language == null }
                    .mapTo(ArrayList<String>()) { it.filePath }
            val randomPathId = mRandom.nextInt(paths.size - 1)
            try {
                val url = URL(mBaseUrl + BACKDROP_SIZE + paths[randomPathId])
                mCurrentImage = BitmapFactory.decodeStream(url.openConnection().getInputStream())
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
            uiThread {
                setUiForCurrentQuestion()
            }
        }
    }

    override fun onClick(v: View) {
        if (!isWaiting) {
            when (v.id) {
                R.id.answer_button1 -> selectAnswer(0)
                R.id.answer_button2 -> selectAnswer(1)
                R.id.answer_button3 -> selectAnswer(2)
                R.id.answer_button4 -> selectAnswer(3)
            }
        }
    }

    private fun selectAnswer(answerId: Int) {
        Log.d(TAG, "selectAnswer() answerId: " + answerId)
        val color: Int
        if (answerId == mCorrectAnswer) {
            Log.d(TAG, "selectAnswer() true")
            color = CORRECT_ANSWER_COLOR
        } else {
            Log.d(TAG, "selectAnswer() false")
            color = WRONG_ANSWER_COLOR
        }
        mButtons[answerId].background.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        generateQuestion()
    }

    private fun setWaitingState(state: Boolean) {
        mWaitingLayout.visibility = if (state) View.VISIBLE else View.INVISIBLE
        isWaiting = state
    }
}
