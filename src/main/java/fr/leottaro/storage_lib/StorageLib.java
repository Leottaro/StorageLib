package fr.leottaro.storage_lib;

import java.beans.BeanInfo;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    public static boolean createFile(Class<? extends Object> c) {
        Path path = Paths.get(storagePath() + c.getName());
        if (path == null) {
            return false;
        }
        try {
            Object object = c.getDeclaredConstructor().newInstance();
            if (!Files.exists(path.getParent())) {
                Files.createDirectory(path.getParent());
            }
            if (!path.toFile().canRead()) {
                path.toFile().createNewFile();
                write(object);
            }
            if (read(c) == null) {
                path.toFile().delete();
                path.toFile().createNewFile();
                write(object);
            }
            write(read(c));
        } catch (Exception e) {
            System.out.format("an error occured in Storage.createFile() : \n%s", e);
            return false;
        }
        return true;
    }

    public static void write(Object object) {
        try {
            FileOutputStream fos = new FileOutputStream(storagePath() + object.getClass().getName(), false);
            fos.write(cryptedBytes(toBytes(object)));
            fos.close();
        } catch (Exception e) {
            System.out.format("an error occured in Storage.write() : %s\n", e);
        }
    }

    private static byte[] toBytes(Object object) {
        try {
            ByteArrayOutputStream baOut = new ByteArrayOutputStream();
            ObjectOutputStream oOut = new ObjectOutputStream(baOut);
            oOut.writeObject(object);
            return baOut.toByteArray();
        } catch (Exception e) {
            System.out.println(e.getMessage());
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

    public static Object read(Class<? extends Object> c) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(storagePath() + c.getName())) {
            byte reading = (byte) fis.read();
            while (reading != -1) {
                bytes.write(reading);
                reading = (byte) fis.read();
            }
            fis.close();
            return fromBytes(decryptedBytes(bytes.toByteArray()));
        } catch (IOException e) {
            System.out.format("an error occured in Storage.read() : %s\n", e);
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
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static boolean postJsonRequest(String gameName, Object object) {
        try {
            String finalData = String.format("{\"userName\":\"%s\",\"class\":\"%s\",\"data\":%s}",
                    System.getProperty("user.name"), object.getClass().getName(), getJsonObject(object, false));
            URL url = new URL(baseUrl + gameName + "/postData");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod("POST");

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
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static String getJsonObject(Object object, boolean getClass) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass());
            PropertyDescriptor[] beanProperties = beanInfo.getPropertyDescriptors();
            String[] jsonData = new String[beanProperties.length];
            for (int i = 0; i < beanProperties.length; i++) {
                jsonData[i] = "\"" + beanProperties[i].getName() + "\":";
                Object value = beanProperties[i].getReadMethod().invoke(object);
                if (value instanceof String) {
                    jsonData[i] += "\"" + value + "\"";
                } else if (value instanceof Class) {
                    jsonData[i] += "\"" + ((Class<?>) value).getName() + "\"";
                } else {
                    jsonData[i] += value.toString();
                }
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

    private static String getJsonRequest(URL url) {
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
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static String getJsonRequest(String game) {
        try {
            String url = baseUrl + game + "/getData?userName=" + System.getProperty("user.name");
            return getJsonRequest(new URL(url));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static String getJsonRequest() {
        try {
            return getJsonRequest(new URL(baseUrl));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static void syncLocalServer(Class<? extends Object> c, String gameName) {
        Object localObject = read(c);
        Object serverObject = getJsonRequest(gameName);

        if (!StorageLib.postJsonRequest(gameName, localObject)) {
            StorageLib.write(serverObject);
        }
    }

    public static void main(String[] args) {
        ExampleStorage example = new ExampleStorage(119834, 120); // 53040
        StorageLib.write(example);
        System.out.println(StorageLib.getJsonObject(StorageLib.read(ExampleStorage.class), false));

        boolean localStoring = StorageLib.createFile(ExampleStorage.class);
        boolean serverStoring = StorageLib.getJsonRequest() != null;

        if (localStoring == serverStoring) {
            StorageLib.syncLocalServer(ExampleStorage.class, ExampleStorage.gameName);
        }

        System.out.println("\n\nA LA FIN:");
        if (localStoring) {
            System.out.print("Local:  ");
            System.out.println(StorageLib.getJsonObject(StorageLib.read(ExampleStorage.class), false));
        }
        if (serverStoring) {
            System.out.print("Server: ");
            System.out.println(StorageLib.getJsonRequest(ExampleStorage.gameName));
        }
    }
}