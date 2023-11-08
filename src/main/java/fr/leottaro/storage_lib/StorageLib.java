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
        } catch (Exception e) {
            return false;
        }
        return true;
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

    private static Object fromBytes(byte[] data) {
        try {
            ByteArrayInputStream baIn = new ByteArrayInputStream(data);
            ObjectInputStream in = new ObjectInputStream(baIn);
            return in.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean postServerJson(String gameName, Object object) {
        try {
            String finalData = String.format("{\"userName\":\"%s\",\"class\":\"%s\",\"data\":%s}",
                    System.getProperty("user.name"), object.getClass().getName(), getJsonFromObject(object, false));
            URL url = new URL(baseUrl + gameName + "/postData");
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

    public static String getJsonFromObject(Object object) {
        return getJsonFromObject(object, true);
    }

    public static String getJsonFromObject(Object object, boolean getClass) {
        try {
            PropertyDescriptor[] beanProperties = Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors();
            String[] jsonData = new String[beanProperties.length];

            // I always want the class first !
            for (int i = 0; i < beanProperties.length; i++) {
                if (beanProperties[i].getName().equals("class")) {
                    PropertyDescriptor temp = beanProperties[0];
                    beanProperties[0] = beanProperties[i];
                    beanProperties[i] = temp;
                    break;
                }
            }

            for (int i = 0; i < beanProperties.length; i++) {
                String keyName = beanProperties[i].getName();
                String valueName;
                Object value = beanProperties[i].getReadMethod().invoke(object);
                if (value instanceof Character) {
                    valueName = String.valueOf(value);
                } else if (value instanceof String) {
                    valueName = (String) value;
                } else if (value instanceof Class) {
                    valueName = ((Class<?>) value).getName();
                } else {
                    valueName = value.toString();
                }
                keyName = keyName.replaceAll("(?=[\\\"\\\\])", "\\\\");
                valueName = valueName.replaceAll("(?=[\\\"\\\\])", "\\\\");
                jsonData[i] = String.format("\"%s\":\"%s\"", keyName, valueName);
            }
            if (getClass) {
                return "{" + String.join(",", jsonData) + "}";
            }
            String[] withoutClassData = new String[jsonData.length - 1];
            for (int i = 0; i < jsonData.length - 1; i++) {
                withoutClassData[i] = jsonData[i + 1];
            }
            return "{" + String.join(",", withoutClassData) + "}";
        } catch (Exception e) {
            return "{}";
        }
    }

    public static Object getObjectFromJson(String json) {
        try {
            if (!json.startsWith("{\"class\":\"")) {
                throw new Exception(
                        "The given string has to contain the class !");
            }

            Pattern jsonPattern = Pattern.compile(
                    "(?:\\\")(?<key>.*?(?<!\\\\)(?:\\\\\\\\)*)(?:\\\":\\\")(?<val>.*?(?<!\\\\)(?:\\\\\\\\)*)(?:\\\")",
                    Pattern.DOTALL);
            Matcher jsonMatcher = jsonPattern.matcher(json);

            int N = 0;
            while (jsonMatcher.find()) {
                N++;
            }

            String[] jsonProperties = new String[N];
            String[] jsonValues = new String[N];

            Pattern bsPattern = Pattern.compile("\\\\(?<char>[\\\\\\\"])");
            Matcher bsMatcher;
            jsonMatcher.find(0);
            N = 0;
            do {
                String key = jsonMatcher.group("key");
                bsMatcher = bsPattern.matcher(key);
                key = bsMatcher.replaceAll("${char}");

                String val = jsonMatcher.group("val");
                bsMatcher = bsPattern.matcher(val);
                val = bsMatcher.replaceAll("${char}");

                jsonProperties[N] = key;
                jsonValues[N] = val;
                N++;
            } while (jsonMatcher.find());

            Class<? extends Object> className = Class.forName(jsonValues[0]);
            Object object = className.getDeclaredConstructor().newInstance();

            PropertyDescriptor[] beanProperties = Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors();
            for (int i = 1; i < beanProperties.length; i++) {
                for (int j = 1; j < jsonProperties.length; j++) {
                    if (beanProperties[i].getName().equals(jsonProperties[j])) {
                        Class<? extends Object> valueClass = toWrapper(beanProperties[i].getPropertyType());
                        Object jsonValue;
                        if (valueClass == Character.class) {
                            jsonValue = jsonValues[j].charAt(0);
                        } else if (valueClass == String.class) {
                            jsonValue = jsonValues[j];
                        } else {
                            Method m = valueClass.getMethod("valueOf", String.class);
                            jsonValue = m.invoke(valueClass, jsonValues[j]);
                        }
                        beanProperties[i].getWriteMethod().invoke(object, valueClass.cast(jsonValue));
                    }
                }
            }

            return object;
        } catch (Exception e) {
            return null;
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

    public static void syncLocalServer(Class<? extends Object> clazz, String gameName) {
        Object localObject = read(clazz);
        Object serverObject = getServerJson(gameName);

        if (!postServerJson(gameName, localObject)) {
            write(serverObject);
        }
    }

    public static void main(String[] args) {
        final TestStorage testObj = new TestStorage(true, Byte.MAX_VALUE, Short.MIN_VALUE, Integer.MAX_VALUE,
                Long.MIN_VALUE, Float.MAX_VALUE, Double.MIN_VALUE, '\"', "He\"l\\lo W\\\"orld\\");
        boolean everythingOk = true;
        String objJson;
        String test;

        // test Json transformation
        objJson = getJsonFromObject(testObj);
        test = getJsonFromObject(getObjectFromJson(objJson));
        if (!objJson.equals(test)) {
            System.out.println("Json Transformation isn't working !");
            everythingOk = false;
        }

        // test local storage
        write(testObj);
        objJson = getJsonFromObject(testObj);
        test = getJsonFromObject(read(TestStorage.class));
        if (!objJson.equals(test)) {
            System.out.println("Local Storage isn't working !");
            everythingOk = false;
        }

        // test server storage
        if (getServerJson() != null) {
            postServerJson(TestStorage.gameName, testObj);
            objJson = getJsonFromObject(testObj, false);
            test = getServerJson(TestStorage.gameName);
            if (!objJson.equals(test)) {
                System.out.println("Server Storage isn't working !");
                everythingOk = false;
            }
        } else {
            System.out.println("Server Storage isn't working !");
            everythingOk = false;
        }

        // test local and server sync
        if (getServerJson() != null) {
            syncLocalServer(TestStorage.class, TestStorage.gameName);
            String localObject = getJsonFromObject(read(TestStorage.class), false);
            String serverObject = getServerJson(TestStorage.gameName);
            if (!localObject.equals(serverObject)) {
                System.out.println("Local/Server sync isn't working !");
                everythingOk = false;
            }
        } else {
            System.out.println("Local/Server sync isn't working !");
            everythingOk = false;
        }

        if (everythingOk) {
            System.out.println("Everything is working");
        }
    }
}