package fr.leottaro.storage_lib;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StorageLib {
    private static final char[] cryptingKey = String.format("!%s?%s?%s!", System.getProperty("os.arch"),
            System.getProperty("os.name"), System.getProperty("user.home")).toCharArray();
    private static String baseUrl = "http://localhost:9090/";

    private static String storagePath() {
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.indexOf("win") != -1) {
            // Windows
            return String.format("%s\\%s\\", System.getenv("APPDATA"), ".leottaro");
        } else if (OS.indexOf("mac") != -1) {
            // Mac
            return String.format("%s/Library/Application Support/%s/", System.getProperty("user.home"), ".leottaro");
        } else {
            // Linux and others
            return String.format("%s/%s/", System.getProperty("user.home"), ".leottaro");
        }
    }

    public static void setBaseUrl(String url) {
        baseUrl = url;
    }

    private static boolean createFile(Object object) {
        Path path = Paths.get(storagePath() + object.getClass().getName());
        if (path == null) {
            return false;
        }
        try {
            if (!Files.exists(path.getParent())) {
                Files.createDirectory(path.getParent());
            }
            if (read(object.getClass()) == null) {
                path.toFile().delete();
                path.toFile().createNewFile();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void write(Object object) {
        try {
            Path path = Paths.get(storagePath() + object.getClass().getName());
            if (!Files.exists(path)) {
                createFile(object);
            }

            FileOutputStream fos = new FileOutputStream(path.toString(), false);
            fos.write(cryptedBytes(toBytes(object)));
            fos.close();
        } catch (Exception e) {
            return;
        }
    }

    public static Object read(Class<? extends Object> clazz) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(storagePath() + clazz.getName());
            byte reading = (byte) fis.read();
            while (reading != -1) {
                bytes.write(reading);
                reading = (byte) fis.read();
            }
            fis.close();
            return fromBytes(decryptedBytes(bytes.toByteArray()));
        } catch (IOException e) {
            return null;
        }
    }

    private static byte[] toBytes(Object object) {
        try {
            ByteArrayOutputStream baOut = new ByteArrayOutputStream();
            ObjectOutputStream oOut = new ObjectOutputStream(baOut);
            oOut.writeObject(object);
            return baOut.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static Object fromBytes(byte[] data) {
        try {
            ByteArrayInputStream baIn = new ByteArrayInputStream(data);
            ObjectInputStream in = new ObjectInputStream(baIn);
            return in.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] cryptedBytes(byte[] data) {
        byte[] cryptedData = new byte[data.length];
        Integer temp;
        for (int i = 0; i < data.length; i++) {
            int tempChar = (int) cryptingKey[i % cryptingKey.length];
            temp = data[i] + tempChar + 1;
            while (temp > 127) {
                temp -= 256;
            }
            while (temp < -128) {
                temp += 256;
            }
            cryptedData[i] = temp.byteValue();
        }
        return cryptedData;
    }

    private static byte[] decryptedBytes(byte[] data) {
        byte[] decryptedData = new byte[data.length];
        Integer temp;
        for (int i = 0; i < data.length; i++) {
            int tempChar = (int) cryptingKey[i % cryptingKey.length];
            temp = data[i] - tempChar - 1;
            while (temp > 127) {
                temp -= 256;
            }
            while (temp < -128) {
                temp += 256;
            }
            decryptedData[i] = temp.byteValue();
        }
        return decryptedData;
    }

    private static boolean isWrapper(Class<? extends Object> clazz) {
        return (clazz == Integer.class)
                || (clazz == Long.class)
                || (clazz == Boolean.class)
                || (clazz == Byte.class)
                || (clazz == Character.class)
                || (clazz == Float.class)
                || (clazz == Double.class)
                || (clazz == Short.class)
                || (clazz == Void.class);
    }

    public static String getJsonFromObject(Object object) {
        try {
            PropertyDescriptor[] beanProperties = Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors();
            int N = 0;
            for (int i = 0; i < beanProperties.length; i++) {
                if (beanProperties[i].getReadMethod() == null) {
                    N++;
                }
            }
            String[] jsonData = new String[beanProperties.length - N];

            // I always want the class first !
            for (int i = 0; i < beanProperties.length; i++) {
                if (beanProperties[i].getName().equals("class")) {
                    PropertyDescriptor temp = beanProperties[0];
                    beanProperties[0] = beanProperties[i];
                    beanProperties[i] = temp;
                    break;
                }
            }

            int jsonI = 0;
            for (int beanI = 0; beanI < beanProperties.length; beanI++) {
                while (beanProperties[beanI].getReadMethod() == null) {
                    beanI++;
                }
                String keyName = beanProperties[beanI].getName();
                String valueName;
                Object value = beanProperties[beanI].getReadMethod().invoke(object);
                if (value instanceof Character) {
                    valueName = String.valueOf(value);
                } else if (value instanceof String) {
                    valueName = (String) value;
                } else if (value instanceof Class) {
                    valueName = ((Class<?>) value).getName();
                } else if (isWrapper(value.getClass())) {
                    valueName = value.toString();
                } else {
                    valueName = getJsonFromObject(value);
                }
                keyName = keyName.replaceAll("(?=[\\\"\\\\])", "\\\\");
                valueName = valueName.replaceAll("(?=[\\\"\\\\])", "\\\\");
                jsonData[jsonI] = String.format("\"%s\":\"%s\"", keyName, valueName);
                jsonI++;
            }
            return "{" + String.join(",", jsonData) + "}";
        } catch (Exception e) {
            return "{}";
        }
    }

    private static Class<? extends Object> toWrapper(Class<? extends Object> clazz) {
        if (!clazz.isPrimitive())
            return clazz;

        if (clazz == Integer.TYPE)
            return Integer.class;
        if (clazz == Long.TYPE)
            return Long.class;
        if (clazz == Boolean.TYPE)
            return Boolean.class;
        if (clazz == Byte.TYPE)
            return Byte.class;
        if (clazz == Character.TYPE)
            return Character.class;
        if (clazz == Float.TYPE)
            return Float.class;
        if (clazz == Double.TYPE)
            return Double.class;
        if (clazz == Short.TYPE)
            return Short.class;
        if (clazz == Void.TYPE)
            return Void.class;

        return clazz;
    }

    public static Object getObjectFromJson(String json) {
        try {
            Pattern jsonPattern = Pattern.compile(
                    "(?:\\\")(?<key>.*?(?<!\\\\)(?:\\\\\\\\)*)(?:\\\":\\\")(?<val>.*?(?<!\\\\)(?:\\\\\\\\)*)(?:\\\")",
                    Pattern.DOTALL);
            Matcher jsonMatcher = jsonPattern.matcher(json);

            int jsonSize = 0;
            while (jsonMatcher.find()) {
                jsonSize++;
            }
            jsonMatcher.reset();

            String[] jsonProperties = new String[jsonSize];
            String[] jsonValues = new String[jsonSize];

            Pattern bsPattern = Pattern.compile("\\\\(?<char>[\\\\\\\"])");
            Matcher bsMatcher;
            for (int jsonI = 0; jsonI < jsonSize && jsonMatcher.find(); jsonI++) {
                String key = jsonMatcher.group("key");
                bsMatcher = bsPattern.matcher(key);
                key = bsMatcher.replaceAll("${char}");

                String val = jsonMatcher.group("val");
                bsMatcher = bsPattern.matcher(val);
                val = bsMatcher.replaceAll("${char}");

                jsonProperties[jsonI] = key;
                jsonValues[jsonI] = val;
            }

            Class<? extends Object> className = Class.forName(jsonValues[0]);
            Object object = className.getDeclaredConstructor().newInstance();

            PropertyDescriptor[] beanProperties = Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors();
            beanLoop: for (int beanI = 1; beanI < beanProperties.length; beanI++) {
                while (beanProperties[beanI].getWriteMethod() == null) {
                    beanI++;
                    if (beanI == beanProperties.length)
                        break beanLoop;
                }
                jsonLoop: for (int jsonI = 0; jsonI < jsonProperties.length; jsonI++) {
                    while (!jsonProperties[jsonI].equals(beanProperties[beanI].getName())) {
                        jsonI++;
                        if (jsonI == jsonProperties.length)
                            break jsonLoop;
                    }
                    Class<? extends Object> valueClass = toWrapper(beanProperties[beanI].getPropertyType());
                    Object jsonValue;
                    if (valueClass == Character.class) {
                        jsonValue = jsonValues[jsonI].charAt(0);
                    } else if (valueClass == String.class) {
                        jsonValue = jsonValues[jsonI];
                    } else if (jsonValues[jsonI].charAt(0) != '{' || isWrapper(valueClass)
                            || jsonValues[jsonI].charAt(jsonValues[jsonI].length() - 1) != '}') {
                        Method m = valueClass.getMethod("valueOf", String.class);
                        jsonValue = m.invoke(valueClass, jsonValues[jsonI]);
                    } else {
                        jsonValue = getObjectFromJson(jsonValues[jsonI]);
                    }
                    beanProperties[beanI].getWriteMethod().invoke(object, valueClass.cast(jsonValue));
                }
            }

            return object;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean postServerJson(Object object) {
        try {
            String finalData = String.format("{\"userName\":\"%s\",\"class\":\"%s\",\"data\":%s}",
                    System.getProperty("user.name"), object.getClass().getName(), getJsonFromObject(object));
            URL url = new URL(baseUrl + object.getClass().getName() + "/postData");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            OutputStream output = connection.getOutputStream();
            output.write(finalData.getBytes("UTF-8"));
            output.flush();
            output.close();

            if (connection.getResponseCode() != 200) {
                throw new Exception(connection.getResponseMessage());
            }

            connection.disconnect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String getServerJson(URL url) {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.connect();

            String output = new String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            con.disconnect();

            return output;
        } catch (Exception e) {
            return null;
        }
    }

    public static String getServerJson(String game) {
        try {
            String url = baseUrl + game + "/getData?userName=" + System.getProperty("user.name");
            return getServerJson(new URL(url));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getServerJson() {
        try {
            return getServerJson(new URL(baseUrl));
        } catch (Exception e) {
            return null;
        }
    }

    public static void syncLocalServer(Class<? extends Object> clazz) {
        Object localObject = read(clazz);
        Object serverObject = getServerJson(clazz.getName());

        if (!postServerJson(localObject)) {
            write(serverObject);
        }
    }

    /*
     * Condition for the class and every parameter classes:
     * Local storing conditions:
     * - the classes must implements Serializable
     * Json transformation conditions:
     * - contains empty constructor
     * - getters for variables you want to be in the JSON
     * - setters for variables you want to translate from the JSON
     * Server storing conditions:
     * - the classes must be able to convert to JSON
     * - the server must have enabled Testit
     */
    public static void main(String[] args) {
        final TestStorage testObj = new TestStorage(new TestClass("He\"l\\loW\\\"orld\\", -1, 1), true, Byte.MAX_VALUE,
                Short.MIN_VALUE, Integer.MAX_VALUE,
                Long.MIN_VALUE, Float.MAX_VALUE, Double.MIN_VALUE, '\"', "He\"l\\loW\\\"orld\\");
        String jsonObj = getJsonFromObject(testObj);
        String test;
        boolean everythingOk = true;

        // test Json transformation
        test = getJsonFromObject(getObjectFromJson(jsonObj));
        if (!jsonObj.equals(test)) {
            System.out.println("LEOTTARO'S STORAGE LIBRARY ERROR MESSAGE: Json Transformation isn't working !");
            everythingOk = false;
        }

        // test local storage
        write(testObj);
        test = getJsonFromObject(read(TestStorage.class));
        if (!jsonObj.equals(test)) {
            System.out.println("LEOTTARO'S STORAGE LIBRARY ERROR MESSAGE: Local Storage isn't working !");
            everythingOk = false;
        }

        // test server storage
        if (getServerJson() != null) {
            postServerJson(testObj);
            test = getServerJson(TestStorage.class.getName());
            if (!jsonObj.equals(test)) {
                System.out.println("LEOTTARO'S STORAGE LIBRARY ERROR MESSAGE: Server Storage isn't working !");
                everythingOk = false;
            }

            // test local and server sync
            syncLocalServer(TestStorage.class);
            String localObject = getJsonFromObject(read(TestStorage.class));
            String serverObject = getServerJson(TestStorage.class.getName());
            if (!localObject.equals(serverObject)) {
                System.out.println("LEOTTARO'S STORAGE LIBRARY ERROR MESSAGE: Local/Server sync isn't working !");
                everythingOk = false;
            }
        } else {
            System.out.println("LEOTTARO'S STORAGE LIBRARY ERROR MESSAGE: Can't connect to the Server !");
            everythingOk = false;
        }

        if (everythingOk) {
            System.out.println("LEOTTARO'S STORAGE LIBRARY: Everything is working");
        }
    }
}