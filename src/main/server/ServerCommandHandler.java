package main.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ServerCommandHandler {
    
    private Map<String, String> users;
    private Map<String, String> houses;
    private Map<String, Map<String, String>> permissions;
    private Map<String, Map<String, Integer>> deviceState;

    private static String HOUSES_FILE = "extras/Houses.txt";
	private static String PERMISSIONS_FILE = "extras/Permissions.txt";
    private static String HOUSE_METADATA_FILE = "extras/HouseMetadata.txt";
    private static String COUNTERS_FILE = "extras/SectionCounters.txt";
    private static String HOUSE_AUDIT_LOG = "logs/houses.log";
    private static String LOG_DIR = "logs/";
    private static final String LOGS_HEADER = "time; command";
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private ServerNetwork serverNetwork;

    public ServerCommandHandler(Map<String, String> users, Map<String, String> houses, Map<String, Map<String, String>> permissions, Map<String, Map<String, Integer>> deviceState, ServerNetwork serverNetwork) {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
		Path projectRoot = current;
        this.users = users;
        this.houses = houses;
        this.permissions = permissions;
        this.deviceState = deviceState;
		PERMISSIONS_FILE = projectRoot.resolve("extras").resolve("Permissions.txt").toString();
    	HOUSES_FILE = projectRoot.resolve("extras").resolve("Houses.txt").toString();
        HOUSE_METADATA_FILE = projectRoot.resolve("extras").resolve("HouseMetadata.txt").toString();
        COUNTERS_FILE = projectRoot.resolve("extras").resolve("SectionCounters.txt").toString();
        HOUSE_AUDIT_LOG = projectRoot.resolve("logs").resolve("houses.log").toString();
        LOG_DIR = projectRoot.resolve("logs").toString();
        this.serverNetwork = serverNetwork;
    }


    private void updateDeviceLogs(String command, String house, String device){
        // the logs should be one per device, all in csv and with a header (time; command (turn on, turn on with timer, turn off))
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
            Path logPath = Paths.get(LOG_DIR, house + "_" + device + ".csv");
            ensureLogHeaderAtTop(logPath, LOGS_HEADER);
            appendLine(logPath.toString(), LocalDateTime.now().format(TS_FORMAT) + ";" + command);

        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private void ensureParentDir(String filePath) throws IOException {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private void ensureFileHeader(String filePath, String header) throws IOException {
        ensureParentDir(filePath);
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        if (!Files.exists(path) || Files.size(path) == 0L) {
            Files.writeString(path, header + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private void ensureLogHeaderAtTop(Path logPath, String header) throws IOException {
        ensureParentDir(logPath.toString());
        if (!Files.exists(logPath)) {
            Files.writeString(
                    logPath,
                    header + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            return;
        }

        String content = Files.readString(logPath, StandardCharsets.UTF_8);
        if (content.isEmpty()) {
            Files.writeString(
                    logPath,
                    header + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            return;
        }

        String[] lines = content.split("\\R", 2);
        String firstLine;
        if (lines.length > 0) {
            firstLine = lines[0].trim();
        } else {
            firstLine = "";
        }
        if (!header.equalsIgnoreCase(firstLine)) {
            Files.writeString(
                    logPath,
                    header + System.lineSeparator() + content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        }
    }

    private void appendLine(String filePath, String line) throws IOException {
        ensureParentDir(filePath);
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            Files.createFile(path);
        }

        boolean empty = Files.size(path) == 0L;
        String payload;
        if (empty) {
            payload = line;
        } else {
            payload = System.lineSeparator() + line;
        }
        Files.writeString(path, payload, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private synchronized boolean persistPermissionsSnapshot() {
        try {
            ensureParentDir(PERMISSIONS_FILE);
            Path target = Paths.get(PERMISSIONS_FILE).toAbsolutePath().normalize();
            Path temp = target.resolveSibling(target.getFileName().toString() + ".tmp");

            StringBuilder content = new StringBuilder("House:User:Section");

            List<String> sortedHouses = new ArrayList<>(permissions.keySet());
            Collections.sort(sortedHouses);

            for (String house : sortedHouses) {
                Map<String, String> housePermissions = permissions.get(house);
                if (housePermissions == null || housePermissions.isEmpty()) {
                    continue;
                }

                List<String> sortedUsers = new ArrayList<>(housePermissions.keySet());
                Collections.sort(sortedUsers);

                for (String user : sortedUsers) {
                    String normalized = normalizePermissionValue(housePermissions.get(user));
                    if (normalized == null) {
                        continue;
                    }
                    content.append(System.lineSeparator())
                            .append(house)
                            .append(":")
                            .append(user)
                            .append(":")
                            .append(normalized);
                }
            }

            Files.writeString(
                    temp,
                    content.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            System.err.println("Erro ao persistir permissões: " + e.getMessage());
            return false;
        }
    }

    private Map<String, Integer> initialSectionCounters() {
        Map<String, Integer> counters = new LinkedHashMap<>();
        counters.put("M", 1);
        counters.put("G", 1);
        counters.put("L", 1);
        counters.put("P", 1);
        counters.put("B", 1);
        counters.put("E", 1);
        return counters;
    }

    private String countersToString(Map<String, Integer> counters) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counters.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private synchronized boolean addHouseToFile(String house, String owner) {
        String now = LocalDateTime.now().format(TS_FORMAT);
        Map<String, Integer> counters = initialSectionCounters();

        String houseRecord = house + ":" + owner;
        String metadataRecord = house + ":createdAt=" + now;
        String permissionRecord = house + ":" + owner + ":all";
        String counterRecord = house + ":" + countersToString(counters);
        String auditRecord = now + " CREATE " + house + " owner=" + owner;

        try {
            ensureFileHeader(HOUSES_FILE, "House:Owner");
            ensureFileHeader(PERMISSIONS_FILE, "House:User:Section");
            ensureFileHeader(HOUSE_METADATA_FILE, "House:Metadata");
            ensureFileHeader(COUNTERS_FILE, "House:Section=NextCounter");

            appendLine(HOUSES_FILE, houseRecord);
            appendLine(PERMISSIONS_FILE, permissionRecord);
            appendLine(HOUSE_METADATA_FILE, metadataRecord);
            appendLine(COUNTERS_FILE, counterRecord);
            appendLine(HOUSE_AUDIT_LOG, auditRecord);
            return true;
        } catch (IOException e) {
            System.err.println("Erro ao persistir CREATE da casa " + house + ": " + e.getMessage());
            return false;
        }
    }

    private String normalizePermissionValue(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.equalsIgnoreCase("all")) {
            return "all";
        }

        Set<String> sections = new LinkedHashSet<>();
        String[] parts = trimmed.split(",");
        for (String part : parts) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            sections.add(String.valueOf(Character.toUpperCase(token.charAt(0))));
        }

        if (sections.isEmpty()) {
            return null;
        }
        return String.join(",", sections);
    }

    private boolean hasHouseAccess(String house, String user) {
        if (houses.containsKey(house) && user.equals(houses.get(house))) {
            return true;
        }
        Map<String, String> housePermissions = permissions.get(house);
        return housePermissions != null && housePermissions.containsKey(user);
    }

    private boolean isAllowedSection(String granted, String requestedSection) {
        String normalizedGranted = normalizePermissionValue(granted);
        String normalizedRequested = normalizePermissionValue(requestedSection);
        if (normalizedGranted == null || normalizedRequested == null) {
            return false;
        }
        if (normalizedGranted.equalsIgnoreCase("all")) {
            return true;
        }
        if (normalizedRequested.equalsIgnoreCase("all")) {
            return false;
        }

        Set<String> allowed = new LinkedHashSet<>();
        for (String token : normalizedGranted.split(",")) {
            if (!token.isBlank()) {
                allowed.add(token);
            }
        }
        for (String token : normalizedRequested.split(",")) {
            if (!allowed.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private String deviceSection(String deviceName) {
        if (deviceName == null || deviceName.isEmpty()) {
            return null;
        }
        return String.valueOf(Character.toUpperCase(deviceName.charAt(0)));
    }


    //Método CREATE
    // TODO maybe verify for malicious input in the house name
    private String createHouse(String user, String house) {
        if (houses.containsKey(house)) {
            return "NOK";
        }
        houses.put(house, user);
        if (!addHouseToFile(house, user)) {
            houses.remove(house);
            return "NOK";
        }

        Map<String, String> housePermissions = permissions.computeIfAbsent(house, k -> new HashMap<>());
        housePermissions.put(user, "all");
        if (!persistPermissionsSnapshot()) {
            System.err.println("Aviso: Ficheiro de permissões pode estar desatualizado após CREATE.");
        }

        if (!deviceState.containsKey(house)) {
            deviceState.put(house, new HashMap<>());
        }

        return "OK";
    }

    
    //Método ADD
    private String addHouse(String user, String userAdd, String house, String section) {
        if (!houses.containsKey(house)) {
            return "NOHM";
        }
        if (!users.containsKey(userAdd)) {
            return "NOUSER";
        }
        if (!houses.get(house).equals(user)) {
            return "NOPERM";
        }

        Map<String, String> housePermissions = permissions.get(house);
        if (housePermissions == null) {
            housePermissions = new HashMap<>();
            permissions.put(house, housePermissions);
        }

        String normalized = normalizePermissionValue(section);
        if (normalized != null) {
            housePermissions.put(userAdd, normalized);
        }
        if (!persistPermissionsSnapshot()) {
            System.err.println("Aviso: Ficheiro de permissões pode estar desatualizado após ADD.");
        }
        
        return "OK";
    }


    //Método RD (Registar)
    private String registerDevice(String user, String house, String section) {
        if (!houses.containsKey(house)) {
            return "NOHM";
        }
        if (!houses.get(house).equals(user)) {
            return "NOPERM";
        }

        String sectionLetter = String.valueOf(Character.toUpperCase(section.charAt(0)));

        Map<String, Integer> devices = deviceState.get(house);
        if (devices == null) {
            devices = new HashMap<>();
            deviceState.put(house, devices);
        }

        int count = 0;
        for (String dev : devices.keySet()) {
            if (!dev.isEmpty() && Character.toUpperCase(dev.charAt(0)) == sectionLetter.charAt(0)) {
                count++;
            }
        }

        String deviceName = sectionLetter + (count + 1);
        devices.put(deviceName, 0);
  
        updateDeviceLogs("RD " + house + " " + section, house, deviceName);
        return "OK";
    }

    // Método EC (Enviar valor)
    public String sendInt(String user, String house, String device, String valueStr){
        if (device != null) {
            device = device.toUpperCase();
        }
        int value;
        try {
            value = Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            return "NOK";
        }
        if (value < 0 || value > 600) {
            return "NOK";
        }
        if (!houses.containsKey(house)) {
            return "NOHM";
        }
        Map<String, Integer> devices = deviceState.get(house);
        if (devices == null || !devices.containsKey(device)) {
            return "NOD";
        }
        if (!hasDeviceAccess(house, user, device)) {
            return "NOPERM";
        }
        devices.put(device, value);

        updateDeviceLogs("EC " + house + " " + device + " " + value, house, device);
        return "OK";
    }   


    //Método RT (Obtém ficheiro de texto)
    public String RT(String house, String user) {
        if (!houses.containsKey(house)){
            return "NOHM";
        }
        if (!hasHouseAccess(house, user)){
            return "NOPERM"; 
        } 
        Map<String, Integer> devices = deviceState.get(house);
        if (devices == null || devices.isEmpty() || devices.keySet().stream().noneMatch(d -> hasDeviceAccess(house, user, d))){
            return "NODATA";
        }
            
        return "OK";
    }

    public void RT_sendFile(String house, String user) {
        if (!houses.containsKey(house) || deviceState.get(house) == null){
            return;
        }

        List<String> allowedDevices = new LinkedList<>();

        for (String deviceName : deviceState.get(house).keySet()) {
            if (hasDeviceAccess(house, user, deviceName)) {
                allowedDevices.add(deviceName);
            }
        }

        Collections.sort(allowedDevices);
        File lineSeparatorFile = new File(LOG_DIR, "lineSeparator.txt");
        try {
            if (!lineSeparatorFile.exists()) {
                Files.writeString(lineSeparatorFile.toPath(), System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<FileInputStream> files = new LinkedList<>();
        final long[] totalSize = {0};
        allowedDevices.stream().map(deviceName -> house + "_" + deviceName + ".csv").forEach(fileName -> {
            try {
                File f = new File(LOG_DIR, fileName);
                files.add(new FileInputStream(f));
                files.add(new FileInputStream(lineSeparatorFile)); 
                totalSize[0] += f.length() + lineSeparatorFile.length();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
        try (SequenceInputStream s = new SequenceInputStream(Collections.enumeration(files))) {
            serverNetwork.send("OK");
            serverNetwork.sendFile(s, totalSize[0]);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    // Método RH (Obter histórico)
    public String historic(String house, String user, String device){
        if (device != null) {
            device = device.toUpperCase();
        }
        if(!houses.containsKey(house)){
            return "NOHM";
        }
        if (!hasDeviceAccess(house, user, device)) {
            return "NOPERM";
        }
        Map<String, Integer> devices = deviceState.get(house);
        if (devices == null || !devices.containsKey(device)) {
            return "NOD";
        }
        File logFile = new File(LOG_DIR, house + "_" + device + ".csv");
        if (!logFile.exists()) {
            System.out.println("Nenhum log encontrado para o dispositivo: " + device + " na casa: " + house);
            return "NODATA";
        }
        
        return "OK";
    }

 
    public void RH_sendFile(String house, String device) {
        if (device != null) {
            device = device.toUpperCase();
        }

        serverNetwork.send("OK");

        File logFile = new File(LOG_DIR, house + "_" + device + ".csv");
        if (logFile.exists()) { 
            serverNetwork.sendFile(logFile.toPath());
        } else {
            System.out.println("Nenhum log encontrado para o dispositivo: " + device + " na casa: " + house);
        }
    }


//////////////////////////FUNÇÕES AUXILIARES/////////////////////////////////////////

    private boolean checkPermissions(String house, String user, String section) {
        if (!houses.containsKey(house)) {
            System.out.println("A casa não existe.");
            return false;
        }
        if (user.equals(houses.get(house))) {
            return true;
        }
        Map<String, String> housePermissions = permissions.get(house);
        if (housePermissions == null || !housePermissions.containsKey(user)) {
            System.out.println("As permissões da casa são null ou o utilizador não tem permissão.");
            return false;
        }
        return isAllowedSection(housePermissions.get(user), section);
    }

    private boolean hasDeviceAccess(String house, String user, String deviceName) {
        return checkPermissions(house, user, deviceSection(deviceName));
    }

//////////////////////////FUNÇÕES AUXILIARES/////////////////////////////////////////


    public String handleCommand(String cmd, String user) {
        if (cmd == null || cmd.trim().isEmpty()) {
            return "ERRO: Não colocou o comando!";
        }

        String[] parts = cmd.trim().split("\\s+");
        System.out.println(Arrays.toString(parts));
        System.out.println(parts.length);
        String comando = parts[0].toUpperCase();
    
        // Comando CREATE
        switch (comando) {
            case "LS" -> {
                if (parts.length > 2) {
                    return "Erro: O correto é: LS ou LS <hm>";
                }
                else if (parts.length == 1) {
                    return lsCommand(user, null);
                } else {
                    String house = parts[1];
                    return lsCommand(user, house);
                }

            }

            case "CREATE" -> {
                if (parts.length != 2) {
                    return "ERRO: O correto é: CREATE <hm>";
                }
                String houseName = parts[1];
                return createHouse(user, houseName);
            }

            // Comando ADD
            case "ADD" -> {
                if (parts.length != 4) {
                    return "ERRO: O correto é:  ADD <user1> <hm> <s>";
                }
                String userAdd = parts[1];
                String house = parts[2];
                String section = parts[3];
                return addHouse(user, userAdd, house, section);

            // Comando RD
            }
            case "RD" -> {
                if (parts.length != 3) {
                    return "ERRO: O correto é:  RD <hm> <s>";
                }
                String house = parts[1];
                String section = parts[2];
                return registerDevice(user, house, section);

            // Comando EC
            }
            case "EC" -> {
                if (parts.length != 4) {
                    return "ERRO: O correto é: EC <hm> <d> <int>";
                }
                String house = parts[1];
                String device = parts[2];
                String value = parts[3];
                return sendInt(user, house, device, value);

            // Comando RH
            }
            case "RH" -> {
                if (parts.length != 3) {
                    return "ERRO: O correto é: RH <hm> <d>";
                }
                String house = parts[1];
                String device = parts[2];
                return historic(house, user, device);

            // Comando RT
            }
            case "RT" -> {
                if (parts.length != 2) {
                    return "ERRO: O correto é: RT <hm>";
                }
                String house = parts[1];
                return RT(house, user);
            }
        }

        return "ERRO: Comando desconhecido: " + comando;
    }

    //Lists the houses that the user has access to, and if a house is specified, lists the devices in that house that the user has access to, with their current state.
    private String lsCommand(String user, String house) {
        if (house != null) {
            if (!houses.containsKey(house)) {
                return "NOHM";
            }
            if (!hasHouseAccess(house, user)) {
                return "NOPERM";
            }
            Map<String, Integer> devices = deviceState.get(house);
            if (devices == null || devices.isEmpty() || devices.keySet().stream().noneMatch(d -> hasDeviceAccess(house, user, d))) {
                return "NODATA";
            }
            StringBuilder result = new StringBuilder();
            devices.keySet().stream().filter(deviceName -> hasDeviceAccess(house, user, deviceName)).forEach(deviceName -> {
                result.append(deviceName).append(" ").append(devices.get(deviceName)).append("\n");
            });
            return result.toString();
        } else {
            StringBuilder result = new StringBuilder();
            houses.keySet().stream().filter(h -> hasHouseAccess(h, user)).forEach(h -> result.append(h).append("\n"));
            if (result.isEmpty()) {
                return "NODATA";
            }
            return result.toString();
        }
    }


}