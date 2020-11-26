package br.pizao.copiloto.network

import br.pizao.copiloto.network.model.WatsonRequest
import br.pizao.copiloto.network.model.WatsonResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

private const val BASE_URL = "https://tcc-ecp7an-grupo-03-api.herokuapp.com/api/watson/"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
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
