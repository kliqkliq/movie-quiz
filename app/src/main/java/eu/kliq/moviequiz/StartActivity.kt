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
        button_start.setOnClickListener({startActivity(Intent(this, MainActivity::class.java))})
        button_about.setOnClickListener({startActivity(Intent(this, AboutActivity::class.java))})
    }
}