package com.example.scrapedown

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.scrapedown.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun fetchHtml(url: String): String {

    fun ensureHttpPrefix(url: String, https: Boolean): String {
        return if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (https){
                "https://$url"
            } else {
                "http://$url"
            }
        } else {
            url
        }
    }

    val client = OkHttpClient()

    val request = Request.Builder()
        .url(ensureHttpPrefix(url, true))
        .build()

    return withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val request2 = Request.Builder()
                    .url(ensureHttpPrefix(url, false))
                    .build()

                withContext(Dispatchers.IO) {
                    client.newCall(request2).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        response.body.string()
                    }
                }
            }
            response.body.string()
        }
    }
}


class MainActivity : AppCompatActivity() {
    // Declare the binding variable at the class level
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityMainBinding.inflate(layoutInflater)

        // Setting the content view with the binding root
        setContentView(binding.root)



        binding.convertButton.setOnClickListener {
            val url = binding.editUrlText.text.toString()

            if (Patterns.WEB_URL.matcher(url).matches()){
                lifecycleScope.launch {
                    try {
                        val html = fetchHtml(url)
                        // Once the HTML is fetched, update UI on the main thread
                        withContext(Dispatchers.Main) {
                            binding.apply {
                                scrapedTextView.text = html
                                scrapedTextView.visibility = View.VISIBLE
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Handle exception
                    }
                }
            } else {
                Toast.makeText(this, "Invalid URL",
                    Toast.LENGTH_SHORT).show();
            }


        }

    }
}