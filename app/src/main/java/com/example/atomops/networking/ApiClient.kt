package com.example.atomops.networking

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject


 class ApiClient @Inject constructor() {
//
    companion object{



    private val rettrofit: Retrofit by lazy {
        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        Retrofit.Builder()
            .baseUrl(ServiceInterface.lmbaseurl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    fun getRetrofitclient(): Retrofit {
        return rettrofit
    }

    private val retrofitjava2: Retrofit by lazy {
        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        Retrofit.Builder()
            .baseUrl(ServiceInterface.javaapi2)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    fun getjava2(): Retrofit {
        return retrofitjava2
    }





    private val retrofitjava: Retrofit by lazy {
        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        Retrofit.Builder()
            .baseUrl(ServiceInterface.javaapi)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    fun getjava(): Retrofit {
        return retrofitjava
    }





        private val retrofit3: Retrofit by lazy {
            val interceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
            Retrofit.Builder()
                .baseUrl(ServiceInterface.omsl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }
        fun getClientomsl(): Retrofit {
            return retrofit3
        }

        private val retrofit2: Retrofit by lazy {
            val interceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
            Retrofit.Builder()
                .baseUrl(ServiceInterface.omsanchar)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }

    fun getClientsanchar(): Retrofit {
            return retrofit2
        }


        private val retrofit: Retrofit by lazy {
            val interceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
            Retrofit.Builder()
                 .baseUrl(ServiceInterface.omapi)
               // .baseUrl("http://nicmapi.mhlprs.com/")
               .addConverterFactory(GsonConverterFactory.create())
             //   .addConverterFactory(NullOnEmptyConverterFactory())
                .client(client)
                .build()
        }
        @JvmStatic
        fun getClient(): Retrofit {
            return retrofit
        }




    private val scmretrofit: Retrofit by lazy {
        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        Retrofit.Builder()
            .baseUrl(ServiceInterface.omapp)
            // .baseUrl("http://nicmapi.mhlprs.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }
    @JvmStatic
    fun getscmclient(): Retrofit {
        return scmretrofit
    }






}









}