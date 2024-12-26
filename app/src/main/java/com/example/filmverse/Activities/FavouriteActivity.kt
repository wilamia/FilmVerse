package com.example.filmverse.Activities
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filmverse.Adapters.FavouriteListAdapter
import com.example.filmverse.Domain.MovieFavourite
import com.example.filmverse.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException

class FavouriteActivity() : AppCompatActivity(){
    private lateinit var loading4 : ProgressBar
    private lateinit var adapterFilms:  FavouriteListAdapter
    private lateinit var recyclerViewFilms: RecyclerView
    private val series = mutableListOf<MovieFavourite>()
    private lateinit var backBtn: ImageView
    private lateinit var noFavoritesTextView: TextView
    private var isDataLoaded = true
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favourite)
        val usernameFromIntent = getUserSession().toString()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        noFavoritesTextView=findViewById(R.id.noFavoritesTextView)
        backBtn = findViewById(R.id.backImage2)
        backBtn.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        loading4 = findViewById(R.id.progressBar2)
        recyclerViewFilms = findViewById(R.id.recyclerView)
        recyclerViewFilms.layoutManager =  LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapterFilms = FavouriteListAdapter(this, series)
        recyclerViewFilms.adapter = adapterFilms
        recyclerViewFilms.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.bottom = 1 // Установите нужный отступ между элементами
            }
        })
        loadFavoriteMovies(usernameFromIntent)
    }
    private fun getUserSession(): String? {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        return sharedPreferences.getString("username", null)
    }
    private fun loadFavoriteMovies(userId: String) {
        // Other database related codes...
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId).child("movies")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val favoriteMovies = mutableListOf<String>()
                for (movieSnapshot in dataSnapshot.children) {
                    val movieId = movieSnapshot.key
                    if (movieId != null) {
                        favoriteMovies.add(movieId)
                    }
                }
                if (favoriteMovies.isEmpty()) {
                    showNoFavoritesMessage() // Метод для отображения сообщения
                } else {
                    for (movieId in favoriteMovies) {
                        loadMovieData(movieId)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@FavouriteActivity, "Ошибка при загрузке избранных фильмов: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun showNoFavoritesMessage() {
        recyclerViewFilms.visibility = View.GONE
        loading4.visibility = View.GONE
        noFavoritesTextView.visibility = View.VISIBLE
    }

    private fun loadMovieData(movieId: String) {
        val movieUrl = "https://kinogo.bot/$movieId.html"
        Log.e("FavouriteActivityUrl", "$movieUrl")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = Jsoup.connect(movieUrl).get()
                val title = document.select("h1.main-title").text()
                val cleanedTitle = if (title.endsWith(")")) {
                    title.substringBeforeLast(" (") // Removes the year in parentheses
                } else {
                    title
                }
                val posterRelativeUrl = document.select("img.full-left-poster").attr("src")
                val posterUrl = "https://kinogo.bot$posterRelativeUrl"
                val year = document.select("div.right-info-element.year-spacer .datecreated").text()
                val id = movieId
                val ratings = document.select("div.services-ratings .rating-trigger")
                val ratingKp = ratings.getOrNull(0)?.text()?.replace("КП: ", "")
                val ratingImdb = ratings.getOrNull(1)?.text()?.replace("IMDb: ", "") ?: "N/A"
                val description = document.select("span#data-text").text()

                // Проверка на наличие рейтингов
                val seriesItem = if (!ratingKp.isNullOrEmpty() && !ratingImdb.isNullOrEmpty()) {
                    MovieFavourite(
                        title = cleanedTitle,
                        posterUrl = posterUrl,
                        id = id,
                        year = year,
                        ratingImdb = ratingImdb,
                        ratingKp = ratingKp,
                        description = description
                    )
                } else null // Если не удалось получить необходимые данные

                withContext(Dispatchers.Main) {
                    if(seriesItem != null) {
                        series.add(seriesItem)
                        adapterFilms.notifyItemInserted(series.size - 1)
                    } else {
                        Log.e("FavouriteActivity", "Movie data is incomplete.")
                    }
                    loading4.visibility = View.GONE
                }
            } catch (e: IOException) {
                Log.e("FavouriteActivity", "Error fetching movie data: ${e.message}", e)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (!isDataLoaded) {
            val usernameFromIntent = getUserSession().toString()
            loadFavoriteMovies(usernameFromIntent) // Загружаем любимые фильмы
            isDataLoaded = true
        }
        refreshUI()
    }
    private fun refreshUI() {

        series.clear() // Очистка старых данных, если это необходимо
        adapterFilms.notifyDataSetChanged() // Уведомление адаптера о том, что данные изменились
    }
    override fun onPause() {
        super.onPause()
        isDataLoaded = false // Сброс флага при уходе с активности
    }
    override fun finish(){
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

}

