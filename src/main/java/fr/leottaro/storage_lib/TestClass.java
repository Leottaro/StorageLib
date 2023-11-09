package fr.leottaro.storage_lib;

import java.io.Serializable;

class TestClass implements Serializable {
    private String name;
    private int x;
    private int y;

    public TestClass(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public TestClass() {
        this("", 0, 0);
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TestClass))
            return false;
        TestClass object = (TestClass) obj;

        if (this.name.equals(object.name))
            return false;
        if (this.x == object.x)
            return false;
        if (this.y == object.y)
            return false;

        return true;
    }
}