package com.jjmr.econifypro.api;

import com.jjmr.econifypro.model.Goal;
import com.jjmr.econifypro.model.Transaction;
import com.jjmr.econifypro.model.LoginRequest;
import com.jjmr.econifypro.model.LoginResponse;
import com.jjmr.econifypro.model.UserRequest;
import com.jjmr.econifypro.model.Category;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PATCH;
import retrofit2.http.DELETE;
import retrofit2.http.Path;
import java.util.List;
import okhttp3.ResponseBody;

public interface ApiService {

    @POST("api/register/") // O la ruta que hayas definido en Django
    Call<ResponseBody> register(@Body UserRequest userRequest);

    // 1. Endpoint para obtener el Token (Login)
    @POST("api/token/")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    // 2. Endpoint para obtener los gastos (Protegido por Token)
    @GET("api/transactions/")
    Call<List<Transaction>> getTransactions(@Header("Authorization") String token);

    // 3. Endpoint para obtener las metas
    @GET("api/goals/")
    Call<List<Goal>> getGoals(@Header("Authorization") String token);

    // 4. Endpoint para actualizar información del perfil de usuario
    @PATCH("api/user/profile/")
    Call<ResponseBody> updateProfile(@Body java.util.Map<String, String> userData);

    // 5. Endpoint Para obtener todas las categorías
    @GET("api/categories/")
    Call<List<Category>> getCategories(@Header("Authorization") String token);

    // 6. Endpoint para crear una categoría
    @POST("api/categories/")
    Call<Category> createCategory(@Header("Authorization") String token, @Body Category category);

    // 7. Endpoint para actualizar una categoría existente
    @PATCH("api/categories/{id}/")
    Call<Category> updateCategory(@Header("Authorization") String token, @Path("id") int id, @Body Category category);

    //  8. Endpoint para eliminar una categoría
    @DELETE("api/categories/{id}/")
    Call<ResponseBody> deleteCategory(@Header("Authorization") String token, @Path("id") int id);
}