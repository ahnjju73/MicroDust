package com.example.joohonga.microdust.stationInfo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;


public class Station {


    @SerializedName("list")
    @Expose
    public List<com.example.joohonga.microdust.stationInfo.List> list;

}