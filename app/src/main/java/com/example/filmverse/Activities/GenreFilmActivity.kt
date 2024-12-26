package com.example.filmverse.Activities

import Movie
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filmverse.Adapters.FilmListAdapter
import com.example.filmverse.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GenreFilmActivity : AppCompatActivity() {
    private lateinit var loading3: View
    private lateinit var loading4: View
    private lateinit var recyclerViewGenre: RecyclerView
    private lateinit var genre: String
    private lateinit var adapterGenre: FilmListAdapter
    private lateinit var backBtn: ImageView
    private var currentPage = 1
    private var isLoading = false
    private val movies = mutableListOf<Movie>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_genre_film)
        overridePendingTransition(0, 0)
        loading3 = findViewById(R.id.loading3)
        loading4 = findViewById(R.id.progressBar4)
        recyclerViewGenre = findViewById(R.id.recyclerViewGenre)

        // Получаем жанр из Intent
        genre = intent.getStringExtra("GENRE") ?: "default_genre"

        // Устанавливаем заголовок
        val titlePage = findViewById<TextView>(R.id.textGenre)
        titlePage.text = genre

        // Загружаем начальные фильмы
        sendRequestGenreFilms(currentPage)
        backBtn = findViewById(R.id.backImage2)
        backBtn.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        // Устанавливаем слушатель прокрутки
        recyclerViewGenre.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!isLoading && isLastItemDisplaying(recyclerView)) {
                    currentPage++
                    sendRequestGenreFilms(currentPage)
                }
            }
        })
    }


    private fun isLastItemDisplaying(recyclerView: RecyclerView): Boolean {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        return lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == itemCount - 1
    }

    private fun sendRequestGenreFilms(page: Int) {
        if (page == 1) {
            loading3.visibility = View.VISIBLE
            loading4.visibility = View.GONE
        }
        else {
            loading4.visibility = View.VISIBLE
        }

        isLoading = true // Устанавливаем состояние загрузки
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var url: String
                if (page == 1) {
                    url = "https://kinogo.bot/f/genres=$genre/s.trailers=0/sort=date;desc/"
                } else {
                    url = "https://kinogo.bot/f/genres=$genre/s.trailers=0/sort=date;desc/page/$page/"
                }
                Log.d("FilmLActivity", "Fetching data from: $url")
                val document = Jsoup.connect(url).get()

                // Выбор элементов фильмов
                val movieElements = document.select("div.kino-card")
                Log.d("FilmLActivity", "Number of movie elements found on page $page: ${movieElements.size}")

                // Обработка каждого элемента фильма
                for (element in movieElements) {
                    val title = element.select("h2.kino-card-title").text().trim()
                    val posterPath = element.select("img").attr("data-src").takeIf { it.isNotEmpty() }
                        ?: element.select("img").attr("src")
                    val posterUrl = "https://kinogo.bot$posterPath"
                    val id = element.select("a").attr("href").substringAfterLast("/").substringBefore(".html")

                    Log.d("FilmLActivity", "Extracted movie: Title: $title, Poster URL: $posterUrl, ID: $id")

                    val movie = Movie(title = title, posterUrl = posterUrl, id = id)
                    movies.add(movie)
                }

                Log.d("MainActivity", "Total movies parsed: ${movies.size}")

                // Обновление UI на главном потоке
                withContext(Dispatchers.Main) {
                    if (page == 1) {
                        loading3.visibility = View.GONE // Скрываем ProgressBar после первой загрузки
                        adapterGenre = FilmListAdapter(movies)
                        recyclerViewGenre.adapter = adapterGenre
                    } else {
                        loading4.visibility = View.GONE // Скрываем ProgressBar после первой загрузки
                        adapterGenre.notifyDataSetChanged() // Обновляем адаптер для новых данных
                    }
                    isLoading = false // Сбрасываем состояние загрузки
                }

            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    if (page == 1) {
                        loading3.visibility = View.GONE
                    }
                    isLoading = false
                    Log.e("MainActivity", "Error fetching data: ${e.message}", e)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (page == 1) {
                        loading3.visibility = View.GONE
                    }
                    isLoading = false
                    Log.e("MainActivity", "Unexpected error: ${e.message}", e)
                }
            }
        }
    }
    override fun finish(){
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}