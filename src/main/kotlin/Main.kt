import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import model.Author
import model.Comment
import model.Post
import model.PostModel
import okhttp3.*
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

const val BASE_URL = "http://localhost:9999"

val client = OkHttpClient.Builder().build()
val gson = Gson()

fun main() {

    with(CoroutineScope(EmptyCoroutineContext)) {
        launch {
            try {
                val posts = getPosts(client)
                    .map { post ->
                        async {
                            PostModel(post, getAuthors(client, post.authorId), getComments(client, post.id))
                        }
                    }.awaitAll()
                println(posts)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    Thread.sleep(30_000L)
}

suspend fun OkHttpClient.apiCall(url: String): Response {
    return suspendCoroutine { continuation ->
        Request.Builder()
            .url(url)
            .build()
            .let(::newCall)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }
            })
    }
}

suspend fun <T> makeRequest(url: String, client: OkHttpClient, typeToken: TypeToken<T>): T =
    withContext(Dispatchers.IO) {
        client.apiCall(url)
            .let { response ->
                if (!response.isSuccessful) {

                    println(response.body?.string())
                    response.close()
                    throw RuntimeException(response.code.toString())
                }

                val body = response.body ?: throw RuntimeException("null body exception")
                gson.fromJson(body.string(), typeToken.type)
            }
    }

suspend fun getPosts(client: OkHttpClient): List<Post> =
    makeRequest("$BASE_URL/api/slow/posts", client, object : TypeToken<List<Post>>() {})

suspend fun getComments(client: OkHttpClient, id: Long): List<Comment> =
    makeRequest("$BASE_URL/api/slow/posts/$id/comments", client, object : TypeToken<List<Comment>>() {})

suspend fun getAuthors(client: OkHttpClient, id: Long): Author =
    makeRequest("$BASE_URL/api/slow/authors/$id", client, object : TypeToken<Author>() {})
