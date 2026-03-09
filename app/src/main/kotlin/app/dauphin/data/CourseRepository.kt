package app.dauphin.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.dauphin.models.CourseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "course_prefs")

class CourseRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = OkHttpClient()

    private val Q_KEY = stringPreferencesKey("q_param")
    private val COURSE_DATA_KEY = stringPreferencesKey("course_data")
    private val STUDENT_ID_KEY = stringPreferencesKey("student_id")

    val qParamFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[Q_KEY]
    }

    val studentIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[STUDENT_ID_KEY]
    }

    suspend fun saveStudentId(studentId: String) {
        context.dataStore.edit { preferences ->
            preferences[STUDENT_ID_KEY] = studentId
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(STUDENT_ID_KEY)
            preferences.remove(Q_KEY)
            preferences.remove(COURSE_DATA_KEY)
        }
    }

    suspend fun saveQParam(q: String) {
        context.dataStore.edit { preferences ->
            preferences[Q_KEY] = q
        }
    }

    suspend fun getCourseData(q: String? = null): CourseResponse? = withContext(Dispatchers.IO) {
        val finalQ = q ?: qParamFlow.first() ?: return@withContext null
        
        if (q != null) {
            saveQParam(q)
        }

        // Use HttpUrl to ensure parameters like '+' are properly encoded
        val urlBuilder = "https://ilifeapi.az.tku.edu.tw/api/ilifeStuClassApi".toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("q", finalQ)
            ?.build()

        val url = urlBuilder?.toString() ?: return@withContext null

        Log.d("CourseRepository", "Fetching from URL: $url")

        return@withContext try {
            val remoteData = fetchFromRemote(url)
            if (remoteData != null) {
                Log.d("CourseRepository", "Successfully fetched ${remoteData.stuelelist.size} classes")
                saveToLocal(remoteData)
                remoteData
            } else {
                Log.w("CourseRepository", "Remote fetch returned null, falling back to local")
                fetchFromLocal()
            }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error fetching remote data", e)
            fetchFromLocal()
        }
    }

    private fun fetchFromRemote(url: String): CourseResponse? {
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("CourseRepository", "HTTP Error: ${response.code}")
                    return null
                }
                val body = response.body?.string() ?: return null
                Log.d("CourseRepository", "Response body: $body")
                json.decodeFromString<CourseResponse>(body)
            }
        } catch (e: IOException) {
            Log.e("CourseRepository", "Network error", e)
            null
        }
    }

    private suspend fun saveToLocal(data: CourseResponse) {
        val serialized = json.encodeToString(data)
        context.dataStore.edit { preferences ->
            preferences[COURSE_DATA_KEY] = serialized
        }
    }

    private suspend fun fetchFromLocal(): CourseResponse? {
        val serialized = context.dataStore.data.map { it[COURSE_DATA_KEY] }.first()
        return if (serialized != null) {
            try {
                json.decodeFromString<CourseResponse>(serialized)
            } catch (e: Exception) {
                Log.e("CourseRepository", "Error decoding local data", e)
                null
            }
        } else {
            null
        }
    }
}
