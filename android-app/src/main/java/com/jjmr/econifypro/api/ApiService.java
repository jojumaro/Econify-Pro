package com.jjmr.econifypro.api;

import com.jjmr.econifypro.model.DashboardResponse;
import com.jjmr.econifypro.model.Goal;
import com.jjmr.econifypro.model.SecurityQuestion;
import com.jjmr.econifypro.model.Transaction;
import com.jjmr.econifypro.model.LoginRequest;
import com.jjmr.econifypro.model.LoginResponse;
import com.jjmr.econifypro.model.UserQuestionsResponse;
import com.jjmr.econifypro.model.UserRequest;
import com.jjmr.econifypro.model.Category;
import com.jjmr.econifypro.model.VerifyResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PATCH;
import retrofit2.http.DELETE;
import retrofit2.http.Path;
import retrofit2.http.Query;
import java.util.List;
import okhttp3.ResponseBody;

public interface ApiService {

    @POST("api/register/")
    Call<ResponseBody> register(@Body UserRequest userRequest);

    @GET("api/security-questions/")
    Call<List<SecurityQuestion>> getSecurityQuestions();

    @GET("api/get-user-questions/")
    Call<UserQuestionsResponse> getUserQuestions(@Query("email") String email);

    @POST("api/verify-identity/")
    Call<VerifyResponse> verifyIdentity(@Body java.util.Map<String, String> body);

    @POST("api/reset-password-confirm/")
    Call<ResponseBody> resetPasswordConfirm(@Body java.util.Map<String, String> body);

    @POST("api/token/")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @PATCH("api/user/profile/")
    Call<ResponseBody> updateProfile(
            @Header("Authorization") String token,
            @Body java.util.Map<String, String> userData
    );

    //===============================================================================//
    // Transactions
    //===============================================================================//
    @GET("api/transactions/")
    Call<List<Transaction>> getTransactions(
            @Header("Authorization") String token,
            @Query("month") Integer month,
            @Query("year") Integer year,
            @Query("category") Integer categoryId,
            @Query("page") Integer page
    );

    @POST("api/transactions/")
    Call<Transaction> createTransaction(@Header("Authorization") String token, @Body Transaction transaction);

    @PATCH("api/transactions/{id}/")
    Call<Transaction> updateTransaction(@Header("Authorization") String token, @Path("id") int id, @Body Transaction transaction);

    @DELETE("api/transactions/{id}/")
    Call<ResponseBody> deleteTransaction(@Header("Authorization") String token, @Path("id") int id);

    //===============================================================================//
    // Goals
    //===============================================================================//
    @GET("api/goals/")
    Call<List<Goal>> getGoals(@Header("Authorization") String token);

    @POST("api/goals/")
    Call<Goal> createGoal(@Header("Authorization") String token, @Body Goal goal);

    @PATCH("api/goals/{id}/")
    Call<Goal> updateGoal(@Header("Authorization") String token, @Path("id") int id, @Body Goal goal);

    @DELETE("api/goals/{id}/")
    Call<ResponseBody> deleteGoal(@Header("Authorization") String token, @Path("id") int id);

    //===============================================================================//
    // Categories
    //===============================================================================//
    @GET("api/categories/")
    Call<List<Category>> getCategories(@Header("Authorization") String token);

    @POST("api/categories/")
    Call<Category> createCategory(@Header("Authorization") String token, @Body Category category);

    @PATCH("api/categories/{id}/")
    Call<Category> updateCategory(@Header("Authorization") String token, @Path("id") int id, @Body Category category);

    @DELETE("api/categories/{id}/")
    Call<ResponseBody> deleteCategory(@Header("Authorization") String token, @Path("id") int id);

    //===============================================================================//
    // Dashboard
    //===============================================================================//
    @GET("api/dashboard/")
    Call<DashboardResponse> getDashboard(@Header("Authorization") String token);
}