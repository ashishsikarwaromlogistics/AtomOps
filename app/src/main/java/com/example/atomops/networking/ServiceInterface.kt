package com.example.atomops.networking
import com.example.atomops.models.CloseLoadingRequest
import com.example.atomops.models.CloseLoadingResponse
import com.example.atomops.models.TokenMod
import com.example.atomops.models.bulkids.BulkResp
import com.example.atomops.models.bulkload.BulkLoadingRequest
import com.example.atomops.models.bulkload.BulkLoadingResponse
import com.example.atomops.models.closem.CloseMod
import com.example.atomops.models.completeunload.CompleteUnloadmod
import com.example.atomops.models.dispatch.DispatchMod
import com.example.atomops.models.document.DocumentResp
import com.example.atomops.models.findbox.FindBoxResp
import com.example.atomops.models.loadphoto.LoadImageMod
import com.example.atomops.models.loadtrip.LoadTripMod
import com.example.atomops.models.localloadbox.DocketBoxesRequest
import com.example.atomops.models.localloadbox.DocketBoxesResponse
import com.example.atomops.models.localtripload.TripLoadingResponse
import com.example.atomops.models.localtrips.TripPage
import com.example.atomops.models.login.LoginMod
import com.example.atomops.models.login.LoginResp
import com.example.atomops.models.password.ChangePassMod
import com.example.atomops.models.scandoc.ScanDocMod
import com.example.atomops.models.scandoc.SubmitManifiest
import com.example.atomops.models.seal.SealMod
import com.example.atomops.models.tripdetails.TripDetailsResp
import com.example.atomops.models.trips.TripResponse
import com.example.atomops.models.uploadimage.UploadImageResp
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
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
    suspend fun getAllTrips(
        @Header("Authorization") auth: String,
        @Query("companyCode") companyCode: Int,
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

    @GET("atom-movement/api/v1/trips")
    suspend fun Allfiltertrips(
        @Header("Authorization") auth: String,
        @Query("companyCode") companyCode: Int,
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
    ): Response<BulkResp>


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

    @POST("atom-movement/api/v1/manifests/{manifestId}/loading/boxes/bulk")
    suspend fun bulkLoadBoxes(
        @Header("Authorization") auth: String,
        @Path("manifestId") manifestId: String,
        @Body mod: LoadTripMod
    ): Response<FindBoxResp>
    //anifests//loading/close


    @POST("atom-movement/api/v1/manifests/{manifestId}/unloading/boxes/bulk")
    suspend fun bulkUnLoadBoxes(
        @Header("Authorization") auth: String,
        @Path("manifestId") manifestId: String,
        @Body mod: LoadTripMod
    ): Response<FindBoxResp>

    @POST("atom-movement/api/v1/mm/manifests/{manifestId}/loading-photo")
    suspend fun loadingPhoto(
        @Header("Authorization") auth: String,
        @Path("manifestId") manifestId: String,
        @Query("companyCode") companyCode: String,
        @Body body: LoadImageMod
    ): Response<FindBoxResp>


    @POST("atom-movement/api/v1/manifests/{manifestId}/loading/close")
    suspend fun LoadingClose(
        @Header("Authorization") auth: String,
        @Path("manifestId") manifestId: String,
        @Body mod: CloseMod
    ): Response<FindBoxResp>

    @POST("atom-movement/api/v1/manifests/{manifestId}/unloading/close")
    suspend fun UnLoadingClose(
        @Header("Authorization") auth: String,
        @Path("manifestId") manifestId: String,
        @Body mod: CloseMod
    ): Response<FindBoxResp>

    @POST("atom-movement/api/v1/trips/{trip}/seal")
    suspend fun loadingseal(
        @Header("Authorization") auth: String,
        @Path("trip") trip: String,
        @Body mod: SealMod
    ): Response<FindBoxResp>


    @POST("atom-movement/api/v1/trips/{trip}/dispatch-ready")
    suspend fun readyfordispatch(
        @Header("Authorization") auth: String,
        @Path("trip") trip: String,
        @Body mod: DispatchMod
    ): Response<FindBoxResp>


    @GET("atom-movement/api/v1/trips/{trip}")
    suspend fun getDocumentData(
        @Header("Authorization") auth: String,
        @Path("trip") trip: String,
        @Query("companyCode") companyCode: String
    ): Response<DocumentResp>

    //https://apidev.omone.in/atom-movement/api/v1/trips/TRIP-1001-26000524/documents/scan
    @POST("atom-movement/api/v1/trips/{trip}/documents/scan")
    suspend fun scandocument(
        @Header("Authorization") auth: String,
        @Path("trip") trip: String,
        @Body body: ScanDocMod
    ): Response<DocumentResp>


    @GET("atom-commercial/api/v1/prqs")
    suspend fun getPrqs(
        @Header("Authorization") auth: String,
        @Query("status") status: String,
        @Query("pickupBranch") pickupBranch: String,
        @Query("fromDate") fromDate: String,   // yyyy-MM-dd
        @Query("toDate") toDate: String,       // yyyy-MM-dd
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<FindBoxResp>

    @Multipart
    @POST("document/documentUploadWithTemplateCode")
    suspend fun uploadDocument(
        @Header("Authorization") auth: String,
        @Query("templateCode") templateCode: String,      // e.g. "PHOTOGRAPH"
        @Query("isLinked") isLinked: Boolean = true,
        @Query("documentName") documentName: String,      // e.g. "MyDoc"
        @Part file: MultipartBody.Part                    // form field name: "file"
    ): Response<UploadImageResp>
//atom-movement/api/v1/trips/TRIP-1002-26000013/create
    @POST("atom-movement/api/v1/trips/{tripNo}/create")
    suspend fun submitManifest(
        @Header("Authorization") token: String,
        @Path("tripNo") tripNo: String,
        @Body body: SubmitManifiest
    ): Response<FindBoxResp>


    @POST("atom-movement/api/v1/trips/{tripNo}/complete")
    suspend fun CompleteUnload(
        @Header("Authorization") token: String,
        @Path("tripNo") tripNo: String,
        @Body body: CompleteUnloadmod
    ): Response<FindBoxResp>


    @GET("atom-movement/api/v1/lmfm/trips")
    suspend fun getTrips(
        @Header("Authorization") auth: String,
        @Query("companyCode") companyCode: String,
        @Query("loader") loader: String,
        @Query("status") status: String = "",
        @Query("fromDate") fromDate: String = "",
        @Query("toDate") toDate: String = "",
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 50
    ): Response<TripPage>

    @GET("atom-movement/api/v1/lmfm/trips/{tripNo}/loading")
    suspend fun getTripLoading(
        @Header("Authorization") auth: String,
        @Path("tripNo") tripNo: String,
        @Query("companyCode") companyCode: String
    ): Response<TripLoadingResponse>


    @POST("atom-scanning/api/v1/dockets/boxes/query")
    suspend fun getDocketBoxes(
        @Header("Authorization") auth: String,
        @Body request: DocketBoxesRequest
    ): Response<DocketBoxesResponse>


    @POST("atom-movement/api/v1/lmfm/trips/{tripNo}/loading/boxes/bulk")
    suspend fun submitBulkLoading(
        @Header("Authorization") auth: String,
        @Path("tripNo") tripNo: String,
        @Query("companyCode") companyCode: String,
        @Body request: BulkLoadingRequest
    ): Response<BulkLoadingResponse>

    @POST("atom-movement/api/v1/lmfm/trips/{tripNo}/loading/close")
    suspend fun closeLoading(
        @Header("Authorization") auth: String,
        @Path("tripNo") tripNo: String,
        @Query("companyCode") companyCode: String,
        @Body request: CloseLoadingRequest
    ): Response<CloseLoadingResponse>
}




