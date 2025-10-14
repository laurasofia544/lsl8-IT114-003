package M2;

public class Problem4 extends BaseClass {
    private static String[] array1 = { "hello world!", "java programming", "special@#$%^&characters", "numbers 123 456",
            "mIxEd CaSe InPut!" };
    private static String[] array2 = { "hello world", "java programming", "this is a title case test",
            "capitalize every word", "mixEd CASE input" };
    private static String[] array3 = { "  hello   world  ", "java    programming  ",
            "  extra    spaces  between   words   ",
            "      leading and trailing spaces      ", "multiple      spaces" };
    private static String[] array4 = { "hello world", "java programming", "short", "a", "even" };

    private static void transformText(String[] arr, int arrayNumber) {
        // Only make edits between the designated "Start" and "End" comments
        printArrayInfoBasic(arr, arrayNumber);

        // Challenge 1: Remove non-alphanumeric characters except spaces
        // Challenge 2: Convert text to Title Case
        // Challenge 3: Trim leading/trailing spaces and remove duplicate spaces
        // Result 1-3: Assign final phrase to `placeholderForModifiedPhrase`
        // Challenge 4 (extra credit): Extract middle 3 characters (beginning starts at middle of phrase),
        // assign to 'placeholderForMiddleCharacters'
        // if not enough characters assign "Not enough characters"
 
        // Step 1: sketch out plan using comments (include ucid and date)
        // Step 2: Add/commit your outline of comments (required for full credit)
        // Step 3: Add code to solve the problem (add/commit as needed)
        String placeholderForModifiedPhrase = "";
        String placeholderForMiddleCharacters = "";
        
        for(int i = 0; i <arr.length; i++){
            // Start Solution Edits

            //lsl8 09/29/25
            String s = arr[i];
            s = s.replaceAll("[^A-Za-z0-9 ]", ""); // step 1: using to remove non-alphanumeric characters, using ... to
            // preserve spaces
            s = s.trim().replaceAll("\\s+", " ");
            String[] words = s.split(" ");
            StringBuilder title = new StringBuilder();
            for (int w = 0; w < words.length; w++) {
                String word = words[w];
                if (word.length() > 0) {
                    char first = Character.toUpperCase(word.charAt(0)); // step 2: using to make text title case
                    String rest = (word.length() > 1) ? word.substring(1).toLowerCase() : "";
                    title.append(first).append(rest);// step 3: using to remove spaces at beginning and end, and to remove
                                                    // duplicate spaces
                        if (w < words.length - 1) title.append(" ");
                }
            }
            placeholderForModifiedPhrase = title.toString(); // step 4: assigning result to placeholderForModifiedPhrase
            // step 6: using to get up to the middle 3 characters
            String phrase = placeholderForModifiedPhrase;
            if (phrase.length() < 3) {
                placeholderForMiddleCharacters = "Not enough characters";
            } else {
            int mid = phrase.length() / 2; // start at the middle
            if (mid + 3 <= phrase.length()) {
                placeholderForMiddleCharacters = phrase.substring(mid, mid + 3); // step 7: using ... to ensure the middle characters exclude first and last of 
                                                                                // word/phrase
            } else {
                placeholderForMiddleCharacters = "Not enough characters";
            }
        }
             // End Solution Edits
            System.out.println(String.format("Index[%d] \"%s\" | Middle: \"%s\"",i, placeholderForModifiedPhrase, placeholderForMiddleCharacters));
        }

       

        
        System.out.println("\n______________________________________");
    }

    public static void main(String[] args) {
        final String ucid = "lsl8"; // <-- change to your UCID
        // No edits below this line
        printHeader(ucid, 4);

        transformText(array1, 1);
        transformText(array2, 2);
        transformText(array3, 3);
        transformText(array4, 4);
        printFooter(ucid, 4);
    }

}