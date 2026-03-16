package main.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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


    public ServerCommandHandler(Map<String, String> users, Map<String, String> houses, Map<String, Map<String, String>> permissions, Map<String, Map<String, Integer>> device, Map<String, Map<String, Integer>> deviceState) {
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

    //Método CREATE 
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

        if (!deviceState.containsKey(house)) {
            deviceState.put(house, new HashMap<>());
        }
        return "OK";
    }

    
    //Método ADD
    private String addHouse(String user, String userAdd, String house, String section) {
        System.out.println("######################CASINHAS############################");
        for(String h : houses.keySet()){
            System.out.print("UMA CASA");
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
            housePermissions.put(userAdd, section);
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
        // logging creation of device
        updateDeviceLogs("RD", house, deviceName);
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
            return "NOHM";
        }
        Map<String, Integer> devices = deviceState.get(house);
        if (devices == null || !devices.containsKey(device)) {
            return "NOD";
        }
        String dono  = houses.get(house);
        boolean isDono = dono.equals(user);
        if (!isDono) {
            Map<String, String> permissionsMap = permissions.get(house);
            boolean isPermission = permissionsMap == null || !permissionsMap.containsKey(user);
            if (isPermission) {
                return "NOPERM";
            }

            String section = permissionsMap.get(user);
            if (section == null) {
                return "NOPERM";
            }

            boolean fullAccess = section.equalsIgnoreCase("all");
            boolean sectionAccess = device.regionMatches(true, 0, section, 0, section.length());
            if (!fullAccess && !sectionAccess) {
                return "NOPERM";
            }
        }

        devices.put(device, value);
        return "OK";
    }

    //Método RT (Obtém ficheiro de texto)

    //Método RH (Obter histórico)


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
        
        //Comando EC
        }else if (comando.equals("EC")) {
            if(parts.length != 4){
                return "ERRO: O correto é: EC <hm> <d> <int>";
            }
            String house = parts[1];
            String device = parts[2];
            String value = parts[3];
            return sendInt(user, house, device, value);
        }



        return "ERRO: Comando desconhecido: " + comando;
    }
}
