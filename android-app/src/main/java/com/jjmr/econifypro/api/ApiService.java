package com.jjmr.econifypro.api;

import com.jjmr.econifypro.model.Goal;
import com.jjmr.econifypro.model.Transaction;
import com.jjmr.econifypro.model.LoginRequest;
import com.jjmr.econifypro.model.TokenResponse;
import com.jjmr.econifypro.model.UserRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import java.util.List;
import okhttp3.ResponseBody;

public interface ApiService {

    @POST("api/register/") // O la ruta que hayas definido en Django
    Call<ResponseBody> register(@Body UserRequest userRequest);

    // 1. Endpoint para obtener el Token (Login)
    @POST("api/token/")
    Call<TokenResponse> login(@Body LoginRequest loginRequest);

    // 2. Endpoint para obtener los gastos (Protegido por Token)
    @GET("api/transactions/")
    Call<List<Transaction>> getTransactions(@Header("Authorization") String token);

    // 3. Endpoint para obtener las metas
    @GET("api/goals/")
    Call<List<Goal>> getGoals(@Header("Authorization") String token);
}