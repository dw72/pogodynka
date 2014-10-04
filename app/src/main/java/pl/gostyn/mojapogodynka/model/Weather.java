package pl.gostyn.mojapogodynka.model;

import com.google.gson.annotations.SerializedName;

public class Weather {
    @SerializedName("sys")
    public Sun sun;
    @SerializedName("weather")
    public Conditions conditions[];
    @SerializedName("main")
    public Parameters parameters;
    public Wind wind;
    public int id;
    @SerializedName("cod")
    public int httpResponseCode;

    public class Sun {
        public int sunrise;
        public int sunset;
    }

    public class Conditions {
        public int id;
        public String main;
        public String description;
        public String icon;
    }

    public class Parameters {
        public float temp;
        public float pressure;
        public int humidity;
    }

    public class Wind {
        public float speed;
        public float deg;
    }
}
