package main.client;

import java.util.Arrays;
import java.util.List;

public class CommandHandler {
    public static void handleCommand(String cmd) {
        // maybe verify cmd is a valid string
        List<String> args = Arrays.asList(cmd.split(" "));
        if (!verifyArgs(cmd)) return;
        cmd = args.getFirst().toUpperCase();
        switch (cmd) {
            case "CREATE":
                break;
            case "ADD":
                break;
            case "RD":
                break;
            case "EC":
                break;
            case "RT":
                break;
            case "RH":
                break;
            case "help":
                break;
                default:
                    System.out.println("use \"help\" to see the commands");
        }
    }

    public static boolean verifyArgs(String cmd){
        List<String> args = Arrays.asList(cmd.split(" "));
        if(args.size() < 2){
            return false;
        }
        args.stream().forEach(System.out::println);
        return true;
    }
}
