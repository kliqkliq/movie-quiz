package eu.kliq.moviequiz

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import info.movito.themoviedbapi.TmdbApi
import info.movito.themoviedbapi.TmdbMovies
import info.movito.themoviedbapi.model.MovieDb
import info.movito.themoviedbapi.model.config.TmdbConfiguration
import info.movito.themoviedbapi.model.core.MovieResultsPage
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.IOException
import java.net.URL
import java.util.*

object QuestionManager {
    private val TAG = "QuestionManager"
    private val API_KEY = BuildConfig.TMDB_API_KEY
    private lateinit var mTmdbConfiguration: TmdbConfiguration
    private lateinit var mTmdbMovies: TmdbMovies
    private lateinit var mBaseUrl: String
    private var mRandom = Random()
    private var mCurrentPage = 0
    private var mCurrentMovieId = 0
    private var mCurrentMovies: MutableMap<Int, Int> = HashMap()
    private var mMovies: MutableMap<Int, MovieDb> = HashMap()
    private val BACKDROP_SIZE = "w1280"
    private val SCORE_MULTIPLIER = 100
    val BUTTONS = 4
    var score = 0
    var correctAnswer = 0
    var currentQuestion = 0

    fun initialize(callback: () -> Unit) {
        doAsync {
            mTmdbMovies = TmdbApi(API_KEY).movies
            mTmdbConfiguration = TmdbApi(API_KEY).configuration
            mBaseUrl = mTmdbConfiguration.baseUrl
            fetchMoviesPage()
            uiThread {
                callback()
            }
        }
    }

    private fun fetchMoviesPage() {
        Log.d(TAG, "fetchMoviesPage()")
        val resultsPage: MovieResultsPage = mTmdbMovies.getPopularMovieList(Locale.getDefault().language, mCurrentPage++)
        mMovies.putAll(resultsPage.results.associateBy({ it.id }, { it }))
    }

    fun generateQuestion(callback: (Bitmap) -> Unit) {
        Log.d(TAG, "generateQuestion()")
        mCurrentMovies.clear()
        doAsync {
            if (currentQuestion % 3 == 0) {
                Log.d(TAG, "Add new movies")
                fetchMoviesPage()
            }
            mCurrentMovieId = randomMovieId
            correctAnswer = mRandom.nextInt(BUTTONS - 1)
            for (iteration in 0 until BUTTONS) {
                // If this iteration is the same as the correct answer, set the ID of the current movie
                val id = if (iteration == correctAnswer) mCurrentMovieId else uniqueRandomMovieId
                Log.d(TAG, "Add answer - button index: $iteration, movieId: $id, correct: " + (iteration == correctAnswer))
                mCurrentMovies.put(iteration, id)
            }
            currentQuestion++
            val currentImage = getMovieImage(mCurrentMovieId)
            uiThread {
                callback(currentImage)
            }
        }
    }

    private val uniqueRandomMovieId: Int
        get() {
            var id: Int
            do {
                id = randomMovieId
            } while (mCurrentMovieId == id || mCurrentMovies.containsValue(id))
            return id
        }

    private val randomMovieId: Int
        get() {
            val randomEntry = mRandom.nextInt(mMovies.size - 1)
            return mMovies.keys.toTypedArray()[randomEntry]
        }

    fun getCurrentQuestionMovieTitle(number: Int): String? {
        val id = mCurrentMovies[number]
        return mMovies[id]?.title
    }

    private fun getMovieImage(id: Int): Bitmap {
        Log.d(TAG, "getMovieImage($id)")
        val currentImage: Bitmap
        val paths = mTmdbMovies.getImages(id, "").backdrops
                .filter { it.language == null }
                .mapTo(ArrayList<String>()) { it.filePath }
        val randomPathId = mRandom.nextInt(paths.size - 1)
        try {
            val url = URL(mBaseUrl + BACKDROP_SIZE + paths[randomPathId])
            currentImage = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return currentImage
    }

    fun onAnswer(answerId: Int, time: Long): Boolean {
        Log.d(TAG, "onAnswer($answerId, $time)")
        if (answerId == correctAnswer) {
            val questionScore = ((1f - Math.min(1f, time/10000f)) * SCORE_MULTIPLIER).toInt()
            score += questionScore
            Log.d(TAG, "question score: $questionScore")
            return true
        } else {
            Log.d(TAG, "wrong answer")
            return false
        }
    }

    fun restartGame() {
        Log.d(TAG, "restartGame()")
        score = 0
        currentQuestion = 0
        mCurrentPage = 0
        mMovies.clear()
        mCurrentMovies.clear()
    }
}
