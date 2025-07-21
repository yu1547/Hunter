import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ntou01157.hunter.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val backendUrl = "https://hunter-backend-sbz8.onrender.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 發送 GET 請求
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder().url(backendUrl).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()

                Log.d("API_RESPONSE", "Response from backend: $body")
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error calling backend", e)
            }
        }
    }
}