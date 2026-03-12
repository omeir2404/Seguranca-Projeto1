// package main.client;

// import java.util.Arrays;
// import java.util.List;

// public class CommandHandler {

//     private static final String HELP_STRING = "• CREATE <hm> # Criar casa <hm> - utilizador é Owner \n • ADD <user1> <hm> <s> # Adicionar utilizador <user1> à casa <hm>, seção <s> \n • RD <hm> <s> # Registar um Dispositivo na casa <hm>, na seção <s> \n • EC <hm> <d> <int> # Enviar valor <int> de estado/temporização, do dispositivo <d> da casa <hm>, para o servidor. \n • RT <hm># Receber a informação sobre o último comando (estados/temporizações) enviado a cada dispositivo da casa <hm>, desde que o utilizador tenha permissões. \n • RH <hm> <d># Receber o Histórico (ficheiro de log .csv) de comandos enviados ao dispositivo <d> da casa <hm>, desde que o utilizador tenha permissões. ";

//     public static void handleCommand(String cmd) {
//         // maybe verify cmd is a valid string
//         List<String> args = Arrays.asList(cmd.split(" "));
//         if (!verifyArgs(cmd)) return;
//         cmd = args.getFirst().toUpperCase();
//         switch (cmd) {
//             case "CREATE":
//                 break;
//             case "ADD":
//                 break;
//             case "RD":
//                 break;
//             case "EC":
//                 break;
//             case "RT":
//                 break;
//             case "RH":
//                 break;
//             case "help":
//                 System.out.printf(HELP_STRING);
//                 break;
//                 default:
//                     System.out.println("use \"help\" to see the commands");
//         }
//     }

//     public static boolean verifyArgs(String cmd){
//         List<String> args = Arrays.asList(cmd.split(" "));
//         if(args.size() < 2){
//             return false;
//         }
//         args.stream().forEach(System.out::println);
//         return true;
//     }
// }
