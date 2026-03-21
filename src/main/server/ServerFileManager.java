package main.server;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manages all file I/O operations for the Sperta Server.
 * Handles reading/writing user data, houses, permissions, and device logs.
 */
public class ServerFileManager {

    private static final String DELIMITER = ":";
    private static final String CSV_EXTENSION = ".csv";
    private static final String LOG_HEADER = "time; command";
    private static final String EC_COMMAND = "EC";
    private static final int MIN_DEVICE_VALUE = 0;
    private static final int MAX_DEVICE_VALUE = 600;
    private static final String EXTRAS_DIR = "extras";
    private static final String LOGS_DIR_NAME = "logs";
    private static final String USERS_FILENAME = "Users.txt";
    private static final String HOUSES_FILENAME = "Houses.txt";
    private static final String PERMISSIONS_FILENAME = "Permissions.txt";
    private static final String ATTESTATION_FILENAME = "attestation.txt";

    private String usersFile;
    private String housesFile;
    private String permissionsFile;
    private String logsDir;
    private String attestationFile;

    public ServerFileManager() {
        initializeFilePaths();
    }

    /**
     * Initializes and validates all file paths based on project structure.
     * Searches for project root by locating extras/Users.txt file.
     */
    private void initializeFilePaths() {
        Path projectRoot = findProjectRoot();
        this.usersFile = projectRoot.resolve(EXTRAS_DIR).resolve(USERS_FILENAME).toString();
        this.housesFile = projectRoot.resolve(EXTRAS_DIR).resolve(HOUSES_FILENAME).toString();
        this.permissionsFile = projectRoot.resolve(EXTRAS_DIR).resolve(PERMISSIONS_FILENAME).toString();
        this.logsDir = projectRoot.resolve(LOGS_DIR_NAME).toString();
        this.attestationFile = projectRoot.resolve(EXTRAS_DIR).resolve(ATTESTATION_FILENAME).toString();
    }

    /**
     * Finds the project root directory by locating the extras/Users.txt marker file.
     */
    private Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path projectRoot = current;

        while (current != null) {
            Path usersFile = current.resolve(EXTRAS_DIR).resolve(USERS_FILENAME);
            if (usersFile.toFile().exists()) {
                projectRoot = current;
                break;
            }
            current = current.getParent();
        }

        return projectRoot;
    }

    /**
     * Gets the project root directory.
     */
    public String getProjectRoot() {
        return findProjectRoot().toString();
    }

    // ============ FILE LOADING METHODS ============


    /**
     * Loads all users from the users file into a Map.
     */
    public Map<String, String> loadUsers() {
        Map<String, String> users = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(usersFile))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split(DELIMITER, 2);
                if (parts.length == 2) {
                    String user = parts[0].trim();
                    String pass = parts[1].trim();
                    if (!user.isEmpty()) {
                        users.put(user, pass);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler " + usersFile + ": " + e.getMessage());
            System.exit(-1);
        }

        return users;
    }


    /**
     * Loads all houses from the houses file into a Map.
     */
    public Map<String, String> loadHouses() {
        Map<String, String> houses = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(housesFile))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split(DELIMITER, 2);
                if (parts.length == 2) {
                    String house = parts[0].trim();
                    String owner = parts[1].trim();
                    if (!house.isEmpty() && !owner.isEmpty()) {
                        houses.put(house, owner);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("ERRO ao ler " + housesFile + ": " + e.getMessage());
        }

        return houses;
    }


    /**
     * Loads all permissions from the permissions file into a nested Map.
     * Map structure: house -> (user -> section)
     */
    public Map<String, Map<String, String>> loadPermissions() {
        Map<String, Map<String, String>> permissions = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(permissionsFile))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split(DELIMITER, 3);
                if (parts.length == 3) {
                    String house = parts[0].trim();
                    String user = parts[1].trim();
                    String section = parts[2].trim();
                    if (!house.isEmpty() && !user.isEmpty() && !section.isEmpty()) {
                        Map<String, String> housePerms = permissions.get(house);
                        if (housePerms == null) {
                            housePerms = new HashMap<>();
                            permissions.put(house, housePerms);
                        }
                        housePerms.put(user, section);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Aviso ao ler " + permissionsFile + ": " + e.getMessage());
        }

        return permissions;
    }

    // ============ DEVICE STATE METHODS ============


    /**
     * Rebuilds device state from persistence (CSV log files).
     * Returns a Map of houses to device states.
     */
    public Map<String, Map<String, Integer>> rebuildDeviceStateFromPersistence(Map<String, String> houses) {
        Map<String, Map<String, Integer>> deviceState = new HashMap<>();
        Map<String, Integer> restoredPerHouse = new HashMap<>();
        int totalRestored = 0;

        for (String house : houses.keySet()) {
            deviceState.putIfAbsent(house, new HashMap<>());
            restoredPerHouse.putIfAbsent(house, 0);
        }

        Path logsDirPath = Paths.get(logsDir).toAbsolutePath().normalize();
        if (!Files.exists(logsDirPath) || !Files.isDirectory(logsDirPath)) {
            printRebuildSummary(restoredPerHouse, totalRestored);
            return deviceState;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsDirPath, "*" + CSV_EXTENSION)) {
            for (Path logPath : stream) {
                String fileName = logPath.getFileName().toString();
                if (!fileName.endsWith(CSV_EXTENSION)) {
                    continue;
                }

                normalizeLogHeader(logPath);

                String baseName = fileName.substring(0, fileName.length() - 4);
                int separator = baseName.lastIndexOf('_');
                if (separator <= 0 || separator >= baseName.length() - 1) {
                    continue;
                }

                String house = baseName.substring(0, separator);
                String device = baseName.substring(separator + 1);
                if (!houses.containsKey(house)) {
                    continue;
                }

                int value = readLastDeviceValue(logPath, house, device);

                Map<String, Integer> houseDevices = deviceState.get(house);
                if (houseDevices == null) {
                    houseDevices = new HashMap<>();
                    deviceState.put(house, houseDevices);
                }
                Integer previous = houseDevices.put(device, value);

                if (previous == null) {
                    restoredPerHouse.put(house, restoredPerHouse.getOrDefault(house, 0) + 1);
                    totalRestored++;
                }
            }
        } catch (IOException e) {
            System.err.println("Aviso ao reconstruir estado dos dispositivos: " + e.getMessage());
        }

        printRebuildSummary(restoredPerHouse, totalRestored);
        return deviceState;
    }

    // ============ LOG FILE METHODS ============

    /**
     * Normalizes the header of a CSV log file.
     * Ensures the file starts with "time; command" header.
     */
    private void normalizeLogHeader(Path logPath) {
        try {
            if (!Files.exists(logPath)) {
                return;
            }

            String firstLine;
            try (BufferedReader reader = Files.newBufferedReader(logPath)) {
                firstLine = reader.readLine();
            }

            if (firstLine == null) {
                Files.writeString(logPath, LOG_HEADER + System.lineSeparator());
                return;
            }

            if (!LOG_HEADER.equalsIgnoreCase(firstLine)) {
                String content = Files.readString(logPath);
                Files.writeString(logPath, LOG_HEADER + System.lineSeparator() + content);
            }
        } catch (IOException e) {
            System.err.println("Erro ao normalizar cabeçalho do log " + logPath + ": " + e.getMessage());
        }
    }

    /**
     * Reads the last device value from a CSV log file.
     * Parses EC commands and extracts the latest value for the specified device.
     */
    private int readLastDeviceValue(Path logPath, String expectedHouse, String expectedDevice) {
        int lastValue = 0;
        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("time;")) {
                    continue;
                }

                String commandPart = extractCommandPart(line);
                if (commandPart == null || commandPart.isEmpty()) {
                    continue;
                }

                String[] cmdParts = commandPart.split("\\s+");
                if (cmdParts.length >= 4 && EC_COMMAND.equalsIgnoreCase(cmdParts[0])) {
                    if (!expectedHouse.equalsIgnoreCase(cmdParts[1]) || !expectedDevice.equalsIgnoreCase(cmdParts[2])) {
                        continue;
                    }
                    try {
                        int parsed = Integer.parseInt(cmdParts[3]);
                        if (parsed >= MIN_DEVICE_VALUE && parsed <= MAX_DEVICE_VALUE) {
                            lastValue = parsed;
                        }
                    } catch (NumberFormatException ignored) {
                        System.err.println("Valor inválido no log: " + cmdParts[3]);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Aviso ao ler log de dispositivo " + logPath + ": " + e.getMessage());
        }
        return lastValue;
    }

    /**
     * Extracts the command part from a CSV line.
     * Handles both current (timestamp;command) and legacy (command;timestamp) formats.
     */
    private String extractCommandPart(String line) {
        return extractCommandFromLine(line);
    }

    /**
     * Helper method to extract command from CSV line.
     */
    private String extractCommandFromLine(String line) {
        String[] csvParts = line.split(";", 2);
        if (csvParts.length == 2) {
            String first = csvParts[0].trim();
            String second = csvParts[1].trim();

            if (second.toUpperCase().startsWith("EC ")) {
                return second;
            }
            if (first.toUpperCase().startsWith("EC ")) {
                return first;
            }
            return null;
        }

        String raw = line.trim();
        String upperRaw = raw.toUpperCase();
        if (upperRaw.startsWith("EC ")) {
            return raw;
        }
        return null;
    }

    /**
     * Appends a new user to the users file.
     * Thread-safe operation.
     */
    public synchronized void appendUser(String user, String pass) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(usersFile, true))) {
            writer.newLine();
            writer.write(user + DELIMITER + pass);
        } catch (IOException e) {
            System.err.println("Erro ao escrever em " + usersFile + ": " + e.getMessage());
        }
    }

    // ============ UTILITY METHODS ============

    /**
     * Retrieves the expected attestation data from the attestation file.
     */
    public String getAttestationData() {
        String expectedAttestation = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(attestationFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    expectedAttestation = line;
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler " + attestationFile + ": " + e.getMessage());
        }
        return expectedAttestation;
    }

    /**
     * Prints a summary of the device state rebuild process.
     */
    private void printRebuildSummary(Map<String, Integer> restoredPerHouse, int totalRestored) {
        List<String> sortedHouses = new ArrayList<>(restoredPerHouse.keySet());
        Collections.sort(sortedHouses);

        int housesWithRestoredDevices = 0;
        for (String house : sortedHouses) {
            int restored = restoredPerHouse.getOrDefault(house, 0);
            if (restored > 0) {
                housesWithRestoredDevices++;
            }
            System.out.println("[startup][rebuild] house=" + house + " restoredDevices=" + restored);
        }
        System.out.println("[startup][rebuild] totalRestoredDevices=" + totalRestored);
        System.out.println("[startup][rebuild] housesLoaded=" + restoredPerHouse.size() + " housesWithRestoredDevices=" + housesWithRestoredDevices);
    }

    // ============ FILE PATH GETTERS ============

    public String getUsersFile() {
        return usersFile;
    }

    public String getHousesFile() {
        return housesFile;
    }

    public String getPermissionsFile() {
        return permissionsFile;
    }

    public String getLogsDir() {
        return logsDir;
    }

    public String getAttestationFile() {
        return attestationFile;
    }
}
