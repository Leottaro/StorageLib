package fr.leottaro.storage_lib;

import java.io.Serializable;

class ExampleStorage implements Serializable {
    public static final String gameName = "Test";
    private int Id;
    private int Score;

    public ExampleStorage() {
        this(0, 0);
    }
    
    public ExampleStorage(int iD, int score) {
        this.Id = iD;
        this.Score = score;
    }

    public int getId() {
        return Id;
    }

    public int getScore() {
        return Score;
    }
}