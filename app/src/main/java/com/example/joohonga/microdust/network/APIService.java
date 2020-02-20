package com.example.joohonga.microdust.network;

import com.example.joohonga.microdust.microdustInfo.Microdust;
import com.example.joohonga.microdust.stationInfo.Station;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface APIService {
    @GET("MsrstnInfoInqireSvc/getNearbyMsrstnList")
    Call<Station> getStationList(
            @Query("_returnType") String json,
            @Query("tmX") double tmX ,
            @Query("tmY") double tmY
    );

    @GET("ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty")
    Call<Microdust> getMicroDustSatus(
            @Query("_returnType") String json,
            @Query("stationName") String stationName ,
            @Query("dataTerm") String dataTerm
    );


}
