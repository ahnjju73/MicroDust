package com.example.joohonga.microdust.network;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class KakaoNetworkManger {
    private static KakaoNetworkManger instance;
    private static final String SERVICE_KEY = "0015847182366ecbe3156b38c8ae84b9";
    private APIService apiService;

    public static KakaoNetworkManger getInstance(){
        if(instance==null){
            instance = new KakaoNetworkManger();
        }
        return instance;
    }

    public APIService getApiService(){
        if(apiService == null){
            Retrofit retrofit = createRetrofit();
            apiService = retrofit.create(APIService.class);
        }
        return apiService;
    }

    private Retrofit createRetrofit(){

        Retrofit retrofit = new Retrofit.Builder()
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("http://openapi.airkorea.or.kr/openapi/services/rest/")
                .build();
        return retrofit;
    }

    private OkHttpClient createOkHttpClient(){
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        HttpUrl originalHttpUrl = original.url();

                        HttpUrl url = originalHttpUrl.newBuilder()
                                .addEncodedQueryParameter("ServiceKey",SERVICE_KEY)
                                .build();
                        Request.Builder requestBuilder = original.newBuilder()
                                .url(url);

                        Request request = requestBuilder.build();
                        return chain.proceed(request);
                    }
                }).build();
        return okHttpClient;
    }
}
