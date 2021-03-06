package eu.kliq.moviequiz

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_start.*

class StartActivity : AppCompatActivity() {

    private val TAG = "StartActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        button_movies.setOnClickListener {startMainActivity(QuestionType.MOVIES)}
        button_people.setOnClickListener {startMainActivity(QuestionType.PEOPLE)}
        button_tv.setOnClickListener {startMainActivity(QuestionType.TV)}
        button_about.setOnClickListener {startActivity(Intent(this, AboutActivity::class.java))}
    }

    private fun startMainActivity(type: QuestionType) {
        startActivity(MainActivityIntent(type))
    }
}