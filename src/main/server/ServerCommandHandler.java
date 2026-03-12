package main.server;

import java.util.Map;

public class ServerCommandHandler {
    
    private Map<String, String> users;
    private Map<String, String> houses;
    private Map<String, Map<String, String>> permissions;
    private Map<String, Map<String, Integer>> device;
    private Map<String, Map<String, Integer>> deviceState;

    public ServerCommandHandler(Map<String, String> users, Map<String, String> houses, Map<String, Map<String, String>> permissions, Map<String, Map<String, Integer>> device, Map<String, Map<String, Integer>> deviceState) {
        this.users = users;
        this.houses = houses;
        this.permissions = permissions;
        this.device = device;
        this.deviceState = deviceState;
    }

    //Método CREATE 
    private String createHouse(String user, String house) {
        if (houses.containsKey(house)) {
            return "NOK";
        }
        houses.put(house, user);
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
        housePermissions.put(userAdd, section);
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
        
        Map<String, Integer> devices = device.get(house);
        int contagem = devices.get(section) + 1;
        devices.put(section, contagem);
        return "OK";
    }

    //Método EC (Enviar valor)
    //FALTA
    public String sendInt(){

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
        }

        return "ERRO: Comando desconhecido: " + comando;
    }
}
