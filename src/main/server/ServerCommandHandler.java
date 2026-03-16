package main.server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ServerCommandHandler {
    
    private Map<String, String> users;
    private Map<String, String> houses;
    private Map<String, Map<String, String>> permissions;
    private Map<String, Map<String, Integer>> device;
    private Map<String, Map<String, Integer>> deviceState;


    private static String HOUSES_FILE = "extras/Houses.txt";
	private static String PERMISSIONS_FILE = "extras/Permissions.txt";


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


    }

    private void addHouseToFile(String House, String User){
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(HOUSES_FILE, true))) {
			writer.newLine();
			writer.write(House + ":" + User);
		} catch (IOException e) {
			System.err.println("Erro ao escrever em " + HOUSES_FILE + ": " + e.getMessage());
		}
    }

    //Método CREATE 
    private String createHouse(String user, String house) {
        if (houses.containsKey(house)) {
            return "NOK";
        }
        houses.put(house, user);
        addHouseToFile(house, user);
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
