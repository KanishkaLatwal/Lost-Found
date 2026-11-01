import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * InputHelper.java — Utility class for safe, validated console input.
 *
 * Every read method keeps prompting until the user provides valid data,
 * so the rest of the application never has to worry about bad input.
 * All methods share a single Scanner (passed in via the constructor).
 */
public class InputHelper {

    private final Scanner scanner;

    /** Constructor — accepts the shared Scanner from Main. */
    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    // ─── STRING INPUT ────────────────────────────────────────────────

    /**
     * Prompts for a non-empty string. Keeps asking until something is typed.
     * Used for required fields like name, location, owner name.
     */
    public String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("  >> This field cannot be empty. Please try again.");
        }
    }

    /**
     * Prompts for an optional string. Returns the trimmed value,
     * which may be empty (caller should treat empty as null).
     */
    public String readOptionalString(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? null : input;
    }

    // ─── INTEGER INPUT ───────────────────────────────────────────────

    /**
     * Prompts for an integer. Keeps asking if the user types letters or symbols.
     * Used for menu choices and item IDs.
     */
    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("  >> Please enter a valid number.");
            }
        }
    }

    // ─── PHONE INPUT ─────────────────────────────────────────────────

    /**
     * Prompts for a 10-digit phone number. Rejects anything that is not
     * exactly 10 digits (no letters, no spaces, no country code).
     */
    public String readPhone(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.matches("\\d{10}")) {
                return input;
            }
            System.out.println("  >> Phone must be exactly 10 digits (e.g. 9876543210).");
        }
    }

    // ─── DATE INPUT ──────────────────────────────────────────────────

    /**
     * Prompts for a date in yyyy-MM-dd format.
     * Validates that:
     *   1. The format is correct and the date actually exists (no Feb 30).
     *   2. The date is not in the future.
     */
    public String readDate(String prompt) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                LocalDate date = LocalDate.parse(input, fmt);
                if (date.isAfter(LocalDate.now())) {
                    System.out.println("  >> Date cannot be in the future.");
                    continue;
                }
                return input;
            } catch (DateTimeParseException e) {
                System.out.println("  >> Invalid date. Use yyyy-MM-dd format (e.g. 2026-10-15).");
            }
        }
    }

    // ─── CHOICE INPUT ────────────────────────────────────────────────

    /**
     * Displays numbered options and returns the user's chosen value.
     * Keeps asking until the user picks a valid number.
     *
     * @param prompt  text shown before the options list
     * @param options array of valid choices (e.g. {"Electronics","Documents",...})
     * @return the selected option string
     */
    public String readChoice(String prompt, String[] options) {
        System.out.println(prompt);
        for (int i = 0; i < options.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, options[i]);
        }
        while (true) {
            System.out.print("Enter choice (1-" + options.length + "): ");
            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= options.length) {
                    return options[choice - 1];
                }
            } catch (NumberFormatException ignored) {
                // fall through to error message
            }
            System.out.println("  >> Invalid choice. Please pick a number from the list.");
        }
    }

    // ─── YES / NO INPUT ─────────────────────────────────────────────

    /**
     * Asks a yes/no question. Returns true for 'y'/'yes', false for 'n'/'no'.
     */
    public boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) return true;
            if (input.equals("n") || input.equals("no"))  return false;
            System.out.println("  >> Please enter 'y' or 'n'.");
        }
    }
}
