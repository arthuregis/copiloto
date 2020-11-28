package br.pizao.copiloto.network

import br.pizao.copiloto.network.model.WatsonRequest
import br.pizao.copiloto.network.model.WatsonResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit


private const val BASE_URL = "https://tcc-ecp7an-grupo-03-api.herokuapp.com/api/watson/"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(20, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

private val retrofit = Retrofit.Builder().client(okHttpClient)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

interface WatsonApiService {
    @POST("message")
    suspend fun getResponse(@Body watsonRequest: WatsonRequest): WatsonResponse
}

object WatsonApi {
    val retrofitService: WatsonApiService by lazy {
        retrofit.create(WatsonApiService::class.java)
    }
}
