package app.dauphin.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.dauphin.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "course_prefs")

class CourseRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = OkHttpClient()

    private val COURSE_DATA_KEY = stringPreferencesKey("course_data")
    private val COOKIES_KEY = stringPreferencesKey("asp_net_cookies")

    val cookiesFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[COOKIES_KEY]
    }

    suspend fun saveCookies(cookies: String) {
        context.dataStore.edit { preferences ->
            preferences[COOKIES_KEY] = cookies
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(COOKIES_KEY)
            preferences.remove(COURSE_DATA_KEY)
        }
    }

    suspend fun getCourseData(): CourseResponse? = withContext(Dispatchers.IO) {
        val cookies = cookiesFlow.first() ?: return@withContext null

        val url = "https://ilifeapp.az.tku.edu.tw/api/stu/course"

        Log.d("CourseRepository", "Fetching from URL: $url")

        return@withContext try {
            val remoteData = fetchFromRemote(url, cookies)
            val tempChanges = fetchTempChanges(cookies)
            
            if (remoteData != null) {
                var mappedData = CourseResponse(stuelelist = groupRawToItems(remoteData))
                
                if (tempChanges != null) {
                    mappedData = patchWithTempChanges(mappedData, tempChanges)
                }

                Log.d("CourseRepository", "Successfully fetched ${mappedData.stuelelist.size} grouped classes")
                saveToLocal(mappedData)
                mappedData
            } else {
                Log.w("CourseRepository", "Remote fetch returned null, falling back to local")
                fetchFromLocal()
            }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error fetching remote data", e)
            fetchFromLocal()
        }
    }

    private fun fetchFromRemote(url: String, cookies: String): List<RawCourseItem>? {
        val request = Request.Builder()
            .url(url)
            .addHeader("Cookie", cookies)
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("CourseRepository", "HTTP Error: ${response.code}")
                    return null
                }
                val body = response.body?.string() ?: return null
                Log.d("CourseRepository", "Response body: $body")
                json.decodeFromString<List<RawCourseItem>>(body)
            }
        } catch (e: IOException) {
            Log.e("CourseRepository", "Network error", e)
            null
        }
    }

    private fun fetchTempChanges(cookies: String): List<TempCourseChange>? {
        val url = "https://ilifeapp.az.tku.edu.tw/api/stu/levreg"
        val request = Request.Builder()
            .url(url)
            .addHeader("Cookie", cookies)
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                json.decodeFromString<List<TempCourseChange>>(body)
            }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error fetching temp changes", e)
            null
        }
    }

    private fun patchWithTempChanges(courseResponse: CourseResponse, tempChanges: List<TempCourseChange>): CourseResponse {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val now = Calendar.getInstance()
        
        // Reset current time to beginning of day for comparison
        now.set(Calendar.HOUR_OF_DAY, 0)
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)

        val patchedList = courseResponse.stuelelist.map { course ->
            val change = tempChanges.find { change ->
                // 1. Teacher match (exact)
                val teacherMatch = change.teachname.trim() == course.teach_name.trim()
                
                // 2. Name match (partial match)
                val nameMatch = course.ch_cos_name.contains(change.cosname.trim()) || 
                             change.cosname.trim().contains(course.ch_cos_name.take(minOf(5, course.ch_cos_name.length)))

                if (!teacherMatch || !nameMatch) return@find false

                // 3. Date and Weekday check
                try {
                    val changeDate = sdf.parse(change.d) ?: return@find false
                    val changeCal = Calendar.getInstance().apply { time = changeDate }
                    
                    // Weekday mapping (Course API uses 1-7 for Mon-Sun)
                    val changeWeekNo = when (changeCal.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> "1"
                        Calendar.TUESDAY -> "2"
                        Calendar.WEDNESDAY -> "3"
                        Calendar.THURSDAY -> "4"
                        Calendar.FRIDAY -> "5"
                        Calendar.SATURDAY -> "6"
                        Calendar.SUNDAY -> "7"
                        else -> ""
                    }
                    
                    if (changeWeekNo != course.week) return@find false

                    // Window check: Only show changes within the current week cycle.
                    // We accept changes from 1 day ago up to 7 days in the future.
                    val timeDiff = changeDate.time - now.timeInMillis
                    val daysDiff = timeDiff / (1000 * 60 * 60 * 24)
                    
                    daysDiff in -1..7
                } catch (e: Exception) {
                    false
                }
            }
            if (change != null) {
                course.copy(tempChange = change.crschg.trim())
            } else {
                course
            }
        }
        return courseResponse.copy(stuelelist = patchedList)
    }

    private fun groupRawToItems(rawItems: List<RawCourseItem>): List<CourseItem> {
        // Filter out empty courses and group by name and week
        return rawItems.filter { it.ch_cos_name.isNotBlank() }
            .groupBy { it.ch_cos_name + it.weekno }
            .map { (_, sessions) ->
                val first = sessions.first()
                val sortedSno = sessions.map { it.sessno }.sorted()
                
                CourseItem(
                    ch_cos_name = first.ch_cos_name,
                    en_cos_name = first.en_cos_name,
                    time_plase = "${first.week}${sortedSno.joinToString(",")}",
                    seat_no = first.seatno,
                    teach_name = first.teach_name,
                    teach_name_en = first.teach_name_en,
                    note = first.note,
                    week = first.weekno,
                    sess1 = sortedSno.getOrNull(0) ?: "",
                    sess2 = sortedSno.getOrNull(1) ?: "",
                    sess3 = sortedSno.getOrNull(2) ?: "",
                    cos_no = "", // Not available in new API
                    cos_ele_seq = "", // Not available in new API
                    remark = "",
                    room = first.room,
                    timePlase = TimePlaseInfo(
                        week = first.weekno,
                        sesses = sortedSno,
                        room = first.room
                    )
                )
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
