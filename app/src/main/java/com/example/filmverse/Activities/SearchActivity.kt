package com.example.filmverse.Activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filmverse.Adapters.FavouriteListAdapter
import com.example.filmverse.Adapters.SearchListAdapter
import com.example.filmverse.Domain.MovieFavourite
import com.example.filmverse.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URLEncoder

class SearchActivity : AppCompatActivity() {
    private lateinit var adapterFilms: SearchListAdapter
    private lateinit var recyclerViewFilms: RecyclerView
    private lateinit var loading: View
    private lateinit var noResultsText: TextView
    private lateinit var backBtn: ImageView
    private val series = mutableListOf<MovieFavourite>()
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        overridePendingTransition(0, 0)

        setContentView(R.layout.activity_search)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val movieName = intent.getStringExtra("SEARCH_QUERY") ?: ""
        val search = findViewById<EditText>(R.id.editTextText3)
        if (movieName != "") {
                search.setText(movieName)
        }
        val query = URLEncoder.encode(search.text.toString(), "UTF-8")
        loading = findViewById(R.id.progressBar5)
        recyclerViewFilms = findViewById(R.id.recyclerView2)
        recyclerViewFilms.layoutManager =  LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapterFilms = SearchListAdapter(this, series)
        recyclerViewFilms.adapter = adapterFilms
        recyclerViewFilms.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.bottom = 1
            }
        })
        backBtn = findViewById(R.id.backImage3)
        backBtn.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        searchFilm(query)
        noResultsText = findViewById(R.id.noResultsText)
        search.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                if (event.rawX >= search.right - search.compoundDrawables[drawableEnd].bounds.width()) {
                    val query = search.text.toString()
                    if (query.isNotEmpty()) {
                        series.clear()
                        adapterFilms.notifyDataSetChanged()
                        searchFilm(query)
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun searchFilm(query: String, page: Int = 1) {
        val movieUrl = "https://kinogo.bot/search/$query/page/$page/"
        Log.e("SearchActivityUrl", "$movieUrl") // Логирование URL
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = Jsoup.connect(movieUrl).get()
                val noResultsMessage = document.select("div.box.berrors").text()
                if (noResultsMessage.contains("Новостей по данному запросу не найдено", ignoreCase = true)) {
                    Log.e("SearchFilterActivity", "No results found message displayed.")
                    withContext(Dispatchers.Main) {
                        noResultsText.visibility = View.VISIBLE // Показываем сообщение
                        loading.visibility = View.GONE
                    }
                    return@launch
                }
                // Извлечение всех элементов фильмов
                val movieElements = document.select("div.kino-card") // Селектор для всех карточек фильмов

                // Обработка каждого элемента
                for (element in movieElements) {
                    val titleElement = element.select("h2.kino-card-title").first()
                    val yearElement = element.select("span.card-title-year").first()
                    val posterElement = element.select("img.lazyloaded").first()
                    val ratingKpElement = element.select("span.kino-poster-kp-rting").first()
                    val ratingImdbElement = element.select("span.kino-poster-imdb-rting").first()
                    val descriptionElement = element.select("div.kino-info-text").first()

                    // Проверка на наличие элементов
                    val cleanedTitle = titleElement?.text()?.trim() ?: "Unknown Title"
                    val year = yearElement?.text()?.trim()?.removeSurrounding("(", ")") ?: "Unknown Year"
                    val posterPath = element.select("img").attr("data-src").takeIf { it.isNotEmpty() }
                        ?: element.select("img").attr("src")
                    val posterUrl = "https://kinogo.bot$posterPath"
                    val ratingKp = ratingKpElement?.attr("data-title")?.replace("KP: ", "") ?: "N/A"
                    val ratingImdb = ratingImdbElement?.attr("data-title")?.replace("IMDb: ", "") ?: "N/A"
                    val description = descriptionElement?.text()?.trim() ?: "No Description Available"
                    val id = element.select("a").attr("href").substringAfterLast("/").substringBefore(".html")
                    // Создание объекта MovieFavourite
                    val seriesItem = MovieFavourite(
                        title = cleanedTitle,
                        posterUrl = posterUrl,
                        id = id,
                        year = year,
                        ratingImdb = ratingImdb,
                        ratingKp = ratingKp,
                        description = description
                    )

                    // Добавление фильма в список
                    series.add(seriesItem)
                }

                // Проверка на наличие следующей страницы
                val nextPageElement = document.select("a.next-page").first() // Селектор для кнопки "Следующая"
                if (nextPageElement != null) {
                    val nextPageUrl = nextPageElement.attr("href")
                    val nextPageNumber = nextPageUrl.substringAfterLast("/").toIntOrNull() ?: (page + 1)
                    searchFilm(query, nextPageNumber) // Рекурсивный вызов для следующей страницы
                }

                withContext(Dispatchers.Main) {
                    adapterFilms.notifyDataSetChanged() // Уведомление адаптера о изменении данных
                    loading.visibility = View.GONE
                }
            } catch (e: IOException) {
                Log.e("SearchActivity", "Error fetching movie data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    loading.visibility = View.GONE
                }
            }
        }
    }
    override fun finish(){
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}