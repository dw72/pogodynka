package pl.gostyn.mojapogodynka.model;

import com.google.gson.annotations.SerializedName;

public class Main {
    public float temp;
    @SerializedName("temp_min")
    public float tempMin;
    @SerializedName("temp_max")
    public float tempMax;
    public float pressure;
    @SerializedName("sea_level")
    public float seaLevel;
    @SerializedName("grnd_level")
    public float grndLevel;
    public int humidity;
}
