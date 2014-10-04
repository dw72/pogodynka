package pl.gostyn.mojapogodynka.model;

public class City {
    public String name;
    public int id;

    public City(String name, int id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String toString() {
        return name;
    }
}
