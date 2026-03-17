package main.server;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ServerCommandHandler {
    
    private Map<String, String> users;
    private Map<String, String> houses;
    private Map<String, Map<String, String>> permissions;
    private Map<String, Map<String, Integer>> device;
    private Map<String, Map<String, Integer>> deviceState;


    private static String HOUSES_FILE = "extras/Houses.txt";
	private static String PERMISSIONS_FILE = "extras/Permissions.txt";
    private static String HOUSE_METADATA_FILE = "extras/HouseMetadata.txt";
    private static String COUNTERS_FILE = "extras/SectionCounters.txt";
    private static String HOUSE_AUDIT_LOG = "logs/houses.log";
    private static String LOG_DIR= "logs/";
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private ServerNetwork serverNetwork;

    public ServerCommandHandler(Map<String, String> users, Map<String, String> houses, Map<String, Map<String, String>> permissions, Map<String, Map<String, Integer>> device, Map<String, Map<String, Integer>> deviceState, ServerNetwork serverNetwork) {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
		Path projectRoot = current;
        this.users = users;
        this.houses = houses;
        this.permissions = permissions;
        this.device = device;
        this.deviceState = deviceState;
		PERMISSIONS_FILE = projectRoot.resolve("extras").resolve("Permissions.txt").toString();
    	HOUSES_FILE = projectRoot.resolve("extras").resolve("Houses.txt").toString();
        HOUSE_METADATA_FILE = projectRoot.resolve("extras").resolve("HouseMetadata.txt").toString();
        COUNTERS_FILE = projectRoot.resolve("extras").resolve("SectionCounters.txt").toString();
        HOUSE_AUDIT_LOG = projectRoot.resolve("logs").resolve("houses.log").toString();
        this.serverNetwork = serverNetwork;

    }

    /**
     * makes the logs for a single device, if the file doesnt exist yet it is created with a header `time; command `
     *
      */
    private void updateDeviceLogs(String command, String house, String device){
        // the logs should be one per device, all in csv and with a header (time; command (turn on, turn on with timer, turn off))
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
            Path logPath = Paths.get(LOG_DIR, house + "_" + device + ".csv");
            if (Files.exists(logPath)) {
                appendLine(logPath.toString(), command + ";" + LocalDateTime.now().format(TS_FORMAT));            }
            else{
                appendLine(logPath.toString(), "time; command");
                appendLine(logPath.toString(), command + ";" + LocalDateTime.now().format(TS_FORMAT));
            }

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

    private void appendLine(String filePath, String line) throws IOException {
        ensureParentDir(filePath);
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            Files.createFile(path);
        }

        boolean empty = Files.size(path) == 0L;
        String payload = empty ? line : System.lineSeparator() + line;
        Files.writeString(path, payload, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private synchronized boolean persistPermissionsSnapshot() {
        try {
            ensureParentDir(PERMISSIONS_FILE);
            Path target = Paths.get(PERMISSIONS_FILE).toAbsolutePath().normalize();
            Path temp = target.resolveSibling(target.getFileName().toString() + ".tmp");

            StringBuilder content = new StringBuilder("House:User:Section");
            for (Map.Entry<String, Map<String, String>> houseEntry : permissions.entrySet()) {
                String house = houseEntry.getKey();
                Map<String, String> housePermissions = houseEntry.getValue();
                if (housePermissions == null) {
                    continue;
                }
                for (Map.Entry<String, String> userEntry : housePermissions.entrySet()) {
                    String normalized = normalizePermissionValue(userEntry.getValue());
                    if (normalized == null) {
                        continue;
                    }
                    content.append(System.lineSeparator())
                            .append(house)
                            .append(":")
                            .append(userEntry.getKey())
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
            System.err.println("Erro ao persistir permissoes: " + e.getMessage());
            return false;
        }
    }

    private Map<String, Integer> initialSectionCounters() {
        Map<String, Integer> counters = new LinkedHashMap<>();
        // Covers sections used in the project text/examples.
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
            if (sb.length() > 0) {
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

    private String mergePermissions(String current, String incoming) {
        String normalizedIncoming = normalizePermissionValue(incoming);
        if (normalizedIncoming == null) {
            return current;
        }
        String normalizedCurrent = normalizePermissionValue(current);
        if ("all".equalsIgnoreCase(normalizedCurrent) || "all".equalsIgnoreCase(normalizedIncoming)) {
            return "all";
        }
        if (normalizedCurrent == null) {
            return normalizedIncoming;
        }

        Set<String> merged = new LinkedHashSet<>();
        for (String token : normalizedCurrent.split(",")) {
            if (!token.isBlank()) {
                merged.add(token);
            }
        }
        for (String token : normalizedIncoming.split(",")) {
            if (!token.isBlank()) {
                merged.add(token);
            }
        }
        return String.join(",", merged);
    }

    private boolean hasPermissionAssignment(String house, String user) {
        Map<String, String> housePermissions = permissions.get(house);
        return housePermissions != null && housePermissions.containsKey(user);
    }

    private boolean hasHouseAccess(String house, String user) {
        if (houses.containsKey(house) && user.equals(houses.get(house))) {
            return true;
        }
        return hasPermissionAssignment(house, user);
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

        Map<String, String> housePermissions = permissions.get(house);
        if (housePermissions == null) {
            housePermissions = new HashMap<>();
            permissions.put(house, housePermissions);
        }
        housePermissions.put(user, "all");
        if (!persistPermissionsSnapshot()) {
            System.err.println("Aviso: ficheiro de permissoes pode estar desatualizado apos CREATE.");
        }

        if (!deviceState.containsKey(house)) {
            deviceState.put(house, new HashMap<>());
        }
        return "OK";
    }

    
    //Método ADD
    private String addHouse(String user, String userAdd, String house, String section) {
        System.out.println("######################CASINHAS############################");
        for(String h : houses.keySet()){
            System.out.print("UMA CASA: ");
            System.out.println(h);
        }
        System.out.println("#######################END CASINHAS#######################");

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

        boolean access = section.equalsIgnoreCase("all");
        if (access) {
            housePermissions.put(userAdd, "all");
        } else {
            String merged = mergePermissions(housePermissions.get(userAdd), section);
            if (merged != null) {
                housePermissions.put(userAdd, merged);
            }
        }
        if (!persistPermissionsSnapshot()) {
            System.err.println("Aviso: ficheiro de permissoes pode estar desatualizado apos ADD.");
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

        Map<String, Integer> devices = deviceState.computeIfAbsent(house, k -> new HashMap<>());

        int count = 0;
        for (String dev : devices.keySet()) {
            if (!dev.isEmpty() && Character.toUpperCase(dev.charAt(0)) == sectionLetter.charAt(0)) {
                count++;
            }
        }

        String deviceName = sectionLetter + (count + 1);
        devices.put(deviceName, 0);
        // logging creation of device
        // RD <hm> <s>
        updateDeviceLogs("RD " + house + " " + section, house, deviceName);
        return "OK";
    }

    //Método EC (Enviar valor)
    public String sendInt(String user, String house, String device, String valueStr){
        int value;
        try {
            value = Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            return "NOK";
        }
        if (value <= 1 || value > 600) {
            return "NOK";
        }
        if (!houses.containsKey(house)) {
            return hasPermissionAssignment(house, user) ? "NOHM" : "NOPERM";
        }
        if (!hasHouseAccess(house, user)) {
            return "NOPERM";
        }
        if (!checkPermissions(house, user, deviceSection(device))) {
            return "NOPERM";
        }
        Map<String, Integer> devices = deviceState.get(house);
        if (devices == null || !devices.containsKey(device)) {
            return "NOD";
        }

        devices.put(device, value);
        // EC <hm> <d> <int>
        updateDeviceLogs("EC " + house + " " + device + " " + value, house, device);
        return "OK";
    }

    private boolean checkPermissions(String house, String user, String section) {
        if (!houses.containsKey(house)) {
            return false;
        }
        if (user.equals(houses.get(house))) {
            return true;
        }
        Map<String, String> housePermissions = permissions.get(house);
        if (housePermissions == null || !housePermissions.containsKey(user)) {
            return false;
        }
        return isAllowedSection(housePermissions.get(user), section);
    }

    //TODO Método RT (Obtém ficheiro de texto)
    // should be implemented like RH and RH_sendFile
    /*
    O comando RT <hm> permite à aplicação cliente obter um ficheiro de texto com os últimos
    comandos de alteração de estado/temporização gravados no servidor referentes a todos os
    dispositivos de uma casa <hm>, ou aqueles que o utilizador tem permissão. Assim, o cliente deverá
    receber uma mensagem OK, depois um número LONG com o tamanho do ficheiro, e em seguida o
    conteúdo do ficheiro
     */
    public String RT(String house, String user) {
        // maybe return NOHM anyways even though user doesnt have permission??
        if (!houses.containsKey(house)) return hasPermissionAssignment(house, user) ? "NOHM" : "NOPERM";
        if (!hasHouseAccess(house, user)) return "NOPERM";
        if (device.get(house) == null) return "NOD";
        return "OK";
    }

    public void RT_sendFile(String house, String user) {
        // send the file resulting from the concatenation of all the devices logs that the user has access to in the given house
    }

    //Método RH (Obter histórico)
    public String RH(String house, String user, String device) {
        if (!houses.containsKey(house)) return hasPermissionAssignment(house, user) ? "NOHM" : "NOPERM";
        String section = deviceSection(device);
        if (!checkPermissions(house, user, section)) return "NOPERM";
        if (!deviceState.containsKey(house)) return "NOHM";
        if (!deviceState.get(house).containsKey(device)) return "NOD";
        File logFile = new File(LOG_DIR, house + "_" + device + ".csv");
        if (!logFile.exists()) {
            System.out.println("No logs found for device " + device + " in house " + house);
            return "NODATA";
        }
        return "OK";
    }

    /**
     * Sends the device log file to the client. Should only be called if the user has permissions to access
     * the device logs. It should only be called after calling RH and getting a return of "OK"
     *
     * @param house  house which the device belongs to
     * @param device device which log file should be sent
     * @requires user must have permissions to access the device logs; otherwise the method should not be called
     */
    public void RH_sendFile(String cmd, String device) {
        // if cmd wasn't correct, it never should reach here
        String[] parts = cmd.split(" ");
        String house = parts[1];
        // send OK
        serverNetwork.send("OK");
        // send file size (in the sendFile function already) and send file bytes
        File logFile = new File(LOG_DIR, house + "_" + device + ".csv");
        if (logFile.exists()) { // redundant check, but for safety it's checked again
            serverNetwork.sendFile(logFile.toPath());
        } else {
            System.out.println("No logs found for device " + device + " in house " + house);
        }
    }


    public String handleCommand(String cmd, String user) {
        if (cmd == null || cmd.trim().isEmpty()) {
            return "ERRO: Não colocaste o comando!";
        }
        
        cmd = cmd.trim();
        String[] parts = cmd.split(" ");
        String comando = parts[0].toUpperCase();
    
        // Comando CREATE
        if (comando.equals("CREATE")) {
            if (parts.length != 2) {
                return "ERRO: O correto é: CREATE <hm>";
            }
            String houseName = parts[1];
            return createHouse(user, houseName);

        // Comando ADD
        } else if (comando.equals("ADD")) {
            if (parts.length != 4) {
                return "ERRO: O correto é:  ADD <user1> <hm> <s>";
            }
            String userAdd = parts[1];
            String house = parts[2];
            String section = parts[3];
            return addHouse(user, userAdd, house, section);
        
        // Comando RD
        } else if (comando.equals("RD")) {
            if (parts.length != 3) {
                return "ERRO: O correto é:  RD <hm> <s>";
            }
            String house = parts[1];
            String section = parts[2];
            return registerDevice(user, house, section);

            // Comando EC
        }else if (comando.equals("EC")) {
            if (parts.length != 4) {
                return "ERRO: O correto é: EC <hm> <d> <int>";
            }
            String house = parts[1];
            String device = parts[2];
            String value = parts[3];
            return sendInt(user, house, device, value);
        } // Comando RH phase of: OK, NOPERMS, NOHM, NOD, NODATA (other phases of RH are the responsibility of the caller using the helper function)
        else if (comando.equals("RH")) {
            if (parts.length != 3) {
                return "ERRO: O correto é: RH <hm> <d>";
            }
            String house = parts[1];
            String device = parts[2];
            return RH(house, user, device);
        }
        // Comando RT phase of: OK, NOPERMS, NOHM, NOD (other phases of RT are the responsibility of the caller using the helper function)
        else if (comando.equals("RT")) {
            if (parts.length != 2) {
                return "ERRO: O correto é: RT <hm>";
            }
            String house = parts[1];
            return RT(house, user);
        }


        return "ERRO: Comando desconhecido: " + comando;
    }
}
