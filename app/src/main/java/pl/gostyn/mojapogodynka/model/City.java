package pl.gostyn.mojapogodynka.model;

public class City {
    public final String name;
    public final int id;

    public City(String name, int id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String toString() {
        return name;
    }
}
