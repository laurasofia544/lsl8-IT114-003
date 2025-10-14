package M3;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Challenge 3: Mad Libs Generator (Randomized Stories)
-----------------------------------------------------
- Load a **random** story from the "stories" folder
- Extract **each line** into a collection (i.e., ArrayList)
- Prompts user for each placeholder (i.e., <adjective>) 
    - Any word the user types is acceptable, no need to verify if it matches the placeholder type
    - Any placeholder with underscores should display with spaces instead
- Replace placeholders with user input (assign back to original slot in collection)
*/

public class MadLibsGenerator extends BaseClass {
    private static final String STORIES_FOLDER = "M3/stories";
    private static String ucid = "lsl8"; // <-- change to your ucid

    public static void main(String[] args) {
        printHeader(ucid, 3,
                "Objective: Implement a Mad Libs generator that replaces placeholders dynamically.");

        Scanner scanner = new Scanner(System.in);
        File folder = new File(STORIES_FOLDER);

        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length == 0) {
            System.out.println("Error: No stories found in the 'stories' folder.");
            printFooter(ucid, 3);
            scanner.close();
            return;
        }
        List<String> lines = new ArrayList<>();
        // Start edits

        // load a random story file
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));
        Random rand = new Random();
        File story = files[rand.nextInt(files.length)];

        // parse the story lines
        try (Scanner fileReader = new Scanner(story)) {
            while (fileReader.hasNextLine()) {
                lines.add(fileReader.nextLine());
            }
        } catch (Exception e) {
            System.out.println("Error reading story file.");
        }

        // iterate through the lines
        Pattern pattern = Pattern.compile("<([^>]+)>");
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // prompt the user for each placeholder (note: there may be more than one
            // placeholder in a line)
            Matcher matcher = pattern.matcher(line);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String placeholder = matcher.group(1).replace('_', ' ');
                System.out.print("Enter " + placeholder + ": ");
                String userWord = scanner.nextLine();

                // apply the update to the same collection slot
                matcher.appendReplacement(sb, userWord);
            }
            matcher.appendTail(sb);
            lines.set(i, sb.toString());
        }

        // End edits
        System.out.println("\nYour Completed Mad Libs Story:\n");
        StringBuilder finalStory = new StringBuilder();
        for (String line : lines) {
            finalStory.append(line).append("\n");
        }
        System.out.println(finalStory.toString());

        printFooter(ucid, 3);
        scanner.close();
    }
}
