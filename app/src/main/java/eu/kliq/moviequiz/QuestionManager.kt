package eu.kliq.moviequiz

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import info.movito.themoviedbapi.TmdbApi
import info.movito.themoviedbapi.TmdbMovies
import info.movito.themoviedbapi.TmdbPeople
import info.movito.themoviedbapi.model.MovieDb
import info.movito.themoviedbapi.model.config.TmdbConfiguration
import info.movito.themoviedbapi.model.core.IdElement
import info.movito.themoviedbapi.model.core.MovieResultsPage
import info.movito.themoviedbapi.model.people.Person
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.IOException
import java.net.URL
import java.util.*
import kotlin.collections.HashMap

object QuestionManager {
    private val TAG = "QuestionManager"
    private val API_KEY = BuildConfig.TMDB_API_KEY
    private val BACKDROP_SIZE = "w1280"
    private val PEOPLE_IMAGE_SIZE = "h632"
    private val SCORE_MULTIPLIER = 100
    private val WOMAN_GENDER = 1
    private val MAN_GENDER = 2
    private lateinit var mTmdbConfiguration: TmdbConfiguration
    private lateinit var mTmdbMovies: TmdbMovies
    private lateinit var mTmdbPeople: TmdbPeople
    private lateinit var mBaseUrl: String
    private lateinit var type: QuestionType
    private var mRandom = Random()
    private var mCurrentPage = 0
    private var mCurrentItemId = 0
    private var mCurrentItems: MutableMap<Int, Int> = HashMap()
    private var mMovies: MutableMap<Int, MovieDb> = HashMap()
    private var mMan: MutableMap<Int, Person> = HashMap()
    private var mWoman: MutableMap<Int, Person> = HashMap()
    private var mCurrentGender = WOMAN_GENDER
    val BUTTONS = 4
    var score = 0
    var correctAnswer = 0
    var currentQuestion = 0

    enum class QuestionType {
        MOVIES, PEOPLE
    }

    fun initialize(callback: () -> Unit) {
        doAsync {
            mTmdbMovies = TmdbApi(API_KEY).movies
            mTmdbPeople = TmdbApi(API_KEY).people
            mTmdbConfiguration = TmdbApi(API_KEY).configuration
            mBaseUrl = mTmdbConfiguration.baseUrl
            uiThread {
                callback()
            }
        }
    }

    private fun fetchQuestion() {
        Log.d(TAG, "fetchQuestion()")
        if (type == QuestionType.MOVIES) {
            val resultsPage: MovieResultsPage = mTmdbMovies.getPopularMovies(Locale.getDefault().language, mCurrentPage++)
            mMovies.putAll(resultsPage.results.associateBy({ it.id }, { it }))
        } else {
            val resultsPage: TmdbPeople.PersonResultsPage = mTmdbPeople.getPersonPopular(mCurrentPage++)
            val items = resultsPage.results.associateBy({ it.id }, { it })
            val peopleByGender = items.map { it.value }.groupBy { mTmdbPeople.getPersonInfo(it.id).gender}
            mWoman.putAll(peopleByGender[WOMAN_GENDER]!!.associateBy({ it.id }, { it }))
            mMan.putAll(peopleByGender[MAN_GENDER]!!.associateBy({ it.id }, { it }))
        }
    }

    fun generateQuestion(questionType: QuestionType, callback: (Bitmap) -> Unit) {
        Log.d(TAG, "generateQuestion()")
        type = questionType
        mCurrentItems.clear()
        doAsync {
            if (currentQuestion % 3 == 0) {
                Log.d(TAG, "Add new question")
                fetchQuestion()
            }
            mCurrentGender = mRandom.nextInt(2) + 1
            mCurrentItemId = randomItemId
            correctAnswer = mRandom.nextInt(BUTTONS)
            for (iteration in 0 until BUTTONS) {
                // If this iteration is the same as the correct answer, set the ID of the current item
                val id = if (iteration == correctAnswer) mCurrentItemId else uniqueRandomItemId
                Log.d(TAG, "Add answer - button index: $iteration, itemId: $id, correct: " + (iteration == correctAnswer))
                mCurrentItems.put(iteration, id)
            }
            currentQuestion++
            val currentImage = getQuestionImage(mCurrentItemId)
            uiThread {
                callback(currentImage)
            }
        }
    }

    private val uniqueRandomItemId: Int
        get() {
            var id: Int
            do {
                id = randomItemId
            } while (mCurrentItemId == id || mCurrentItems.containsValue(id))
            return id
        }

    private val randomItemId: Int
        get() {
            val items: MutableMap<Int, IdElement> = when (type) {
                QuestionType.MOVIES -> mMovies as MutableMap<Int, IdElement>
                QuestionType.PEOPLE -> if (mCurrentGender == WOMAN_GENDER) mWoman as MutableMap<Int, IdElement> else mMan as MutableMap<Int, IdElement>
            }
            val randomEntry = mRandom.nextInt(items.size - 1)
            return items.keys.toTypedArray()[randomEntry]
        }

    fun getCurrentQuestionButtonString(number: Int): String? {
        val id = mCurrentItems[number]
        return when (type) {
            QuestionType.MOVIES -> mMovies[id]!!.title
            QuestionType.PEOPLE -> when (mCurrentGender) {
                WOMAN_GENDER -> mWoman[id]!!.name
                MAN_GENDER -> mMan[id]!!.name
                else -> ""
            }
        }
    }

    private fun getQuestionImage(id: Int): Bitmap {
        Log.d(TAG, "getQuestionImage($id)")
        val currentImage: Bitmap
        val paths = when (type) {
            QuestionType.MOVIES ->
                mTmdbMovies.getImages(id, "").backdrops
                        .filter { it.language == null }
                        .mapTo(ArrayList<String>()) { it.filePath }
            QuestionType.PEOPLE ->
                mTmdbPeople.getPersonImages(id)
                        .mapTo(ArrayList<String>()) { it.filePath }
        }
        val randomPathId = mRandom.nextInt(paths.size - 1)
        try {
            val imageSize = when (type) {
                QuestionType.MOVIES -> BACKDROP_SIZE
                QuestionType.PEOPLE -> PEOPLE_IMAGE_SIZE
            }
            val url = URL(mBaseUrl + imageSize + paths[randomPathId])
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
        mMan.clear()
        mWoman.clear()
        mCurrentItems.clear()
    }
}
