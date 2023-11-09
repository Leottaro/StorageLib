package fr.leottaro.storage_lib;

import java.io.Serializable;

class TestStorage implements Serializable {
    private TestClass testClass;
    private boolean bit;
    private byte b;
    private short s;
    private int i;
    private long l;
    private float f;
    private double d;
    private char c;
    private String string;

    public TestStorage(TestClass testClass, boolean bit, byte b, short s, int i, long l, float f, double d, char c,
            String string) {
        this.testClass = testClass;
        this.bit = bit;
        this.b = b;
        this.s = s;
        this.i = i;
        this.l = l;
        this.f = f;
        this.d = d;
        this.c = c;
        this.string = string;
    }

    public TestStorage() {
        this(new TestClass(), false, (byte) 0, (short) 0, 0, (long) 0, (float) 0., 0., ' ', "");
    }

    public TestClass getTestClass() {
        return testClass;
    }

    public byte getB() {
        return b;
    }

    public short getS() {
        return s;
    }

    public int getI() {
        return i;
    }

    public long getL() {
        return l;
    }

    public float getF() {
        return f;
    }

    public double getD() {
        return d;
    }

    public char getC() {
        return c;
    }

    public String getString() {
        return string;
    }

    public void setTestClass(TestClass testClass) {
        this.testClass = testClass;
    }

    public void setBit(boolean bit) {
        this.bit = bit;
    }

    public void setB(byte b) {
        this.b = b;
    }

    public void setS(short s) {
        this.s = s;
    }

    public void setI(int i) {
        this.i = i;
    }

    public void setL(long l) {
        this.l = l;
    }

    public void setF(float f) {
        this.f = f;
    }

    public void setD(double d) {
        this.d = d;
    }

    public void setC(char c) {
        this.c = c;
    }

    public void setString(String string) {
        this.string = string;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TestStorage))
            return false;
        TestStorage object = (TestStorage) obj;

        if (this.bit != object.bit)
            return false;
        if (this.bit != object.bit)
            return false;
        if (this.b != object.b)
            return false;
        if (this.s != object.s)
            return false;
        if (this.i != object.i)
            return false;
        if (this.l != object.l)
            return false;
        if (this.f != object.f)
            return false;
        if (this.d != object.d)
            return false;
        if (this.c != object.c)
            return false;
        if (!this.string.equals(object.string))
            return false;
        if (!this.testClass.equals(object.testClass))
            return false;

        return true;
    }
}