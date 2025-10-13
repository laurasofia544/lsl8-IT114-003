package M3;

/*
Challenge 1: Command-Line Calculator
------------------------------------
- Accept two numbers and an operator as command-line arguments
- Supports addition (+) and subtraction (-)
- Allow integer and floating-point numbers
- Ensures correct decimal places in output based on input (e.g., 0.1 + 0.2 → 1 decimal place)
- Display an error for invalid inputs or unsupported operators
- Capture 5 variations of tests
*/

public class CommandLineCalculator extends BaseClass {
    private static String ucid = "lsl8"; // <-- change to your ucid

    // Helper method to count how many decimal places are in a number string
    private static int getDecimalPlaces(String numStr) {
        if (numStr.contains(".")) {
            return numStr.length() - numStr.indexOf('.') - 1;
        }
        return 0;
    }

    public static void main(String[] args) {
        printHeader(ucid, 1, "Objective: Implement a calculator using command-line arguments.");

        if (args.length != 3) {
            System.out.println("Usage: java M3.CommandLineCalculator <num1> <operator> <num2>");
            printFooter(ucid, 1);
            return;
        }

        try {
            System.out.println("Calculating result...");
            //lsl8 10/13/25
            // extract the equation (format is <num1> <operator> <num2>)
            String num1Str = args[0];
            String operator = args[1];
            String num2Str = args[2];

            // check if operator is addition or subtraction
            if (!operator.equals("+") && !operator.equals("-")) {
                System.out.println("Error: Unsupported operator. Use + or - only.");
                printFooter(ucid, 1);
                return;
            }

            // check the type of each number and choose appropriate parsing
            double num1 = Double.parseDouble(num1Str);
            double num2 = Double.parseDouble(num2Str);

            // generate the equation result (Important: ensure decimals display as the
            // longest decimal passed)
            // i.e., 0.1 + 0.2 would show as one decimal place (0.3), 0.11 + 0.2 would show
            // as two (0.31), etc
            int decimals1 = getDecimalPlaces(num1Str);
            int decimals2 = getDecimalPlaces(num2Str);
            int maxDecimals = Math.max(decimals1, decimals2);

            double result;
            if (operator.equals("+")) {
                result = num1 + num2;
            } else {
                result = num1 - num2;
            }

            // Format result with correct decimal places
            String format = "%." + maxDecimals + "f";
            System.out.println("Result: " + String.format(format, result));

        } catch (Exception e) {
            System.out.println("Invalid input. Please ensure correct format and valid numbers.");
        }

        printFooter(ucid, 1);
    }
}

