package eu.kliq.moviequiz;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.model.Artwork;
import info.movito.themoviedbapi.model.MovieDb;
import info.movito.themoviedbapi.model.MovieImages;
import info.movito.themoviedbapi.model.config.TmdbConfiguration;
import info.movito.themoviedbapi.model.core.MovieResultsPage;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    TmdbConfiguration mTmdbConfiguration;
    TmdbMovies mTmdbMovies;
    MovieResultsPage mMovieResultsPage;
    int mCurrentPage = 0;
    int mCurrentQuestion = 0;
    int mCurrentMovieId;
    Bitmap mCurrentImage = null;
    Map<Integer, String> mCurrentTitles = new HashMap<>();
    int mCorrectAnswer;
    String mBaseUrl;
    Map<Integer, String> mMovies;
    Random mRandom = new Random();

    // UI
    ImageView mImageView;
    List<Button> mButtons;

    private static final String API_KEY = "885584a3af04b82774477558b5aba3fc";
    private static final String BACKDROP_SIZE = "w1280";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView)findViewById(R.id.movie_img);
        mButtons = new ArrayList<>();
        mButtons.add((Button)findViewById(R.id.answer_button1));
        mButtons.add((Button)findViewById(R.id.answer_button2));
        mButtons.add((Button)findViewById(R.id.answer_button3));
        mButtons.add((Button)findViewById(R.id.answer_button4));

        for (Button button : mButtons) {
            button.setOnClickListener(this);
        }

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            initialize();
        }
    }

    private void initialize() {
        new GetPopularMoviesTask().execute();
    }

    private void generateMoviesMap() {
        List<MovieDb> results = mMovieResultsPage.getResults();
        mMovies = new LinkedHashMap<>();
        for (MovieDb movie : results) {
            int id = movie.getId();
            String title = movie.getTitle();
            mMovies.put(id, title);
        }
        generateQuestion();
    }

    private void generateQuestion() {
        mCurrentMovieId = getRandomMovieId();
        getMovieImage(mCurrentMovieId);
        mCorrectAnswer = mRandom.nextInt(3);
        int id;
        mCurrentTitles.clear();
        for (int iteration = 0; iteration < 4; iteration++) {
            // If this iteration is the same as the correct anwser, set the ID of the current movie
            if (iteration == mCorrectAnswer) {
                id = mCurrentMovieId;
            } else {
                // Set the random movie ID
                do {
                    id = getRandomMovieId();
                } while (id == mCurrentMovieId);
            }
            mCurrentTitles.put(iteration, mMovies.get(id));
        }
        mCurrentQuestion++;
    }

    private int getRandomMovieId() {
        int randomEntry = mRandom.nextInt(mMovies.size() - 1);
        return (int) mMovies.keySet().toArray()[randomEntry];
    }

    private void setUiForCurrentQuestion() {
        Log.d(TAG, "setUiForCurrentQuestion()");
        // Set image
        mImageView.setImageBitmap(mCurrentImage);
        // Set buttons
        for (int iteration = 0; iteration < 4; iteration++) {
            mButtons.get(iteration).setText(mCurrentTitles.get(iteration));
        }
    }

    private void getMovieImage(int id) {
        Log.d(TAG, "getMovieImage() id: " + id);
        new GetMovieImageTask().execute(id);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.answer_button1:
                selectAnwser(0);
                break;
            case R.id.answer_button2:
                selectAnwser(1);
                break;
            case R.id.answer_button3:
                selectAnwser(2);
                break;
            case R.id.answer_button4:
                selectAnwser(3);
                break;
        }
    }

    private void selectAnwser(int anwserId) {
        Log.d(TAG, "selectAnwser() anwserId: " + anwserId);
        String toastText;
        if (anwserId == mCorrectAnswer) {
            Log.d(TAG, "selectAnwser() true");
            toastText = "Brawooo! :)";
        } else {
            Log.d(TAG, "selectAnwser() false");
            toastText = "Niestety nie :(";
        }
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
        generateQuestion();
    }

    private class GetPopularMoviesTask extends AsyncTask<Void, Void, MovieResultsPage> {
        @Override
        protected MovieResultsPage doInBackground(Void... params) {
            mTmdbMovies = new TmdbApi(API_KEY).getMovies();
            mTmdbConfiguration = new TmdbApi(API_KEY).getConfiguration();
            return mTmdbMovies.getPopularMovieList("pl", mCurrentPage++);
        }

        @Override
        protected void onPostExecute(MovieResultsPage results) {
            mMovieResultsPage = results;
            mBaseUrl = mTmdbConfiguration.getBaseUrl();
            generateMoviesMap();
        }
    }

    private class GetMovieImageTask extends AsyncTask<Integer, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Integer... params) {
            Bitmap image = null;
            List<String> paths = new ArrayList<>();
            MovieImages movieImages = mTmdbMovies.getImages(params[0], "");
            List<Artwork> backdrops = movieImages.getBackdrops();
            for (Artwork backdrop : backdrops) {
                if (backdrop.getLanguage() == null) {
                    paths.add(backdrop.getFilePath());
                }
            }
            int randomPathId = mRandom.nextInt(paths.size() - 1);
            try {
                URL url = new URL(mBaseUrl + BACKDROP_SIZE + paths.get(randomPathId));
                image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return image;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            mCurrentImage = result;
            setUiForCurrentQuestion();
        }
    }
}
