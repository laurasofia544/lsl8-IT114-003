package M3;

import java.util.Scanner;
import java.util.Random;

/*
Challenge 2: Simple Slash Command Handler
-----------------------------------------
- Accept user input as slash commands
  - "/greet <name>" → Prints "Hello, <name>!"
  - "/roll <num>d<sides>" → Roll <num> dice with <sides> and returns a single outcome as "Rolled <num>d<sides> and got <result>!"
  - "/echo <message>" → Prints the message back
  - "/quit" → Exits the program
- Commands are case-insensitive
- Print an error for unrecognized commands
- Print errors for invalid command formats (when applicable)
- Capture 3 variations of each command except "/quit"
*/

public class SlashCommandHandler extends BaseClass {
    private static String ucid = "lsl8"; // <-- change to your UCID

    public static void main(String[] args) {
        printHeader(ucid, 2, "Objective: Implement a simple slash command parser.");

        Scanner scanner = new Scanner(System.in);
        Random rand = new Random();
//lsl8 10/13/25
        while (true) {
            System.out.print("Enter command: ");
            String input = scanner.nextLine().trim();

            // check if greet
            if (input.equalsIgnoreCase("/greet")) {
                System.out.println("Error: Use /greet <name>");
            } else if (input.toLowerCase().startsWith("/greet ")) {
                String name = input.substring(7).trim();
                if (name.isEmpty()) {
                    System.out.println("Error: Use /greet <name>");
                } else {
                    System.out.println("Hello, " + name + "!");
                }
            }

            // check if roll
            ///process roll
            /// handle invalid formats
            else if (input.equalsIgnoreCase("/roll")) {
                System.out.println("Error: Use /roll <num>d<sides>");
            } else if (input.toLowerCase().startsWith("/roll ")) {
                String roll = input.substring(6).trim();
                int d = roll.toLowerCase().indexOf('d');
                if (d <= 0 || d == roll.length() - 1) {
                    System.out.println("Error: Use /roll <num>d<sides>");
                } else {
                    try {
                        int num = Integer.parseInt(roll.substring(0, d).trim());
                        int sides = Integer.parseInt(roll.substring(d + 1).trim());
                        if (num < 1 || sides < 2) {
                            System.out.println("Error: num must be >= 1 and sides >= 2");
                        } else {
                            int total = 0;
                            for (int i = 0; i < num; i++) total += rand.nextInt(sides) + 1;
                            System.out.println("Rolled " + num + "d" + sides + " and got " + total + "!");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Error: Numbers must be valid integers");
                    }
                }
            }

            // check if echo
            /// process echo
            else if (input.equalsIgnoreCase("/echo")) {
                System.out.println("Error: Use /echo <message>");
            } else if (input.toLowerCase().startsWith("/echo ")) {
                String msg = input.substring(6).trim();
                if (msg.isEmpty()) System.out.println("Error: Use /echo <message>");
                else System.out.println(msg);
            }

            // check if quit
            /// process quit
            else if (input.equalsIgnoreCase("/quit")) {
                System.out.println("Goodbye!");
                break;
            }

            // handle invalid commands
            else {
                System.out.println("Error: Unknown command.");
            }
        }

        scanner.close();
        printFooter(ucid, 2);
    }
}

