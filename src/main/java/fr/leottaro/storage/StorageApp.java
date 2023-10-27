package fr.leottaro.storage;

public class StorageApp {
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