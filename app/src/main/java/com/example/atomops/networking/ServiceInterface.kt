package com.example.atomops.networking
import com.example.atomops.models.TokenMod
import com.example.atomops.models.findbox.FindBoxResp
import com.example.atomops.models.login.LoginMod
import com.example.atomops.models.login.LoginResp
import com.example.atomops.models.password.ChangePassMod
import com.example.atomops.models.tripdetails.TripDetailsResp
import com.example.atomops.models.trips.TripResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface ServiceInterface   {

  //production

     companion object{
     val omapi: String by lazy {
            "https://scmomsanchar.omlogistics.co.in/oracle/android_api/"
            //"https://qa.omlogistics.co.in/oracle/android_api/"
        }
     val omsanchar: String by lazy {
            //"https://omsanchar.omlogistics.co.in/"
            "https://scmomsanchar.omlogistics.co.in/"
        }
     val omapp: String by lazy {
           // "https://omapp.omlogistics.co.in/"
            "https://scm.omlogistics.co.in/"
        }
     val omsl: String by lazy {
         "https://scmomsanchar.omlogistics.co.in/"
        }
         val javaapi: String by lazy {
             "https://apiload.omone.in/"

         }

         val javaapi2: String by lazy {
             "https://apiload.omone.in/challan-processing-service/"
            // "https://load.omone.in/utility-service/"

         }
         val lmbaseurl: String by lazy {
             "https://apidev.omone.in/"
           //      "http://10.10.130.123:30088/"
            // "https://load.omone.in/utility-service/"

         }
    }


    @POST("user-iam/v1/login")
    suspend fun   Login( @Body mod: LoginMod
    ): Response<LoginResp?>?

    @GET("atom-movement/api/v1/trips")
    suspend fun getTrips(
        @Header("Authorization") auth: String,
        @Query("companyCode") companyCode: Int,
        @Query("status") status: String? = null,   // null = parameter is omitted
        @Query("branch") branch: Int,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<TripResponse>

    @GET("atom-movement/api/v1/trips")
    suspend fun filtertrips(
        @Header("Authorization") auth: String,
        @Query("companyCode") companyCode: Int,
        @Query("status") status: String? = null,   // null = parameter is omitted
        @Query("branch") branch: Int,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("q") q: String ? = null
    ): Response<TripResponse>


    @GET("atom-movement/api/v1/trips/{tripId}")
    suspend fun getTripDetails(
        @Header("Authorization") auth: String,
        @Path("tripId") tripid: String,
    ): Response<TripDetailsResp>

    @GET("atom-movement/api/v1/trips/{tripId}")
    suspend fun filterTripDetails(
        @Header("Authorization") auth: String,
        @Path("tripId") tripid: String,
    ): Response<TripDetailsResp>


    @POST("user-iam/v1/refreshToken")
    suspend fun refreshToken(
       @Body mod: TokenMod
    ): Response<TripDetailsResp>


    @POST("/atom-scanning/api/v1/scans/bulk")
    suspend fun bulkScan(
        @Header("Authorization") auth: String,
       @Body mod: Any
    ): Response<TripDetailsResp>


    @POST("atom-scanning/api/v1/dockets/boxes/query")
    suspend fun getAllboxes(
        @Header("Authorization") auth: String,
       @Body mod: Any
    ): Response<FindBoxResp>


    @PUT("user-iam/v1/profile/password")
    suspend fun ChangePassword(
        @Header("Authorization") auth: String,
       @Body mod: ChangePassMod
    ): Response<FindBoxResp>



}




