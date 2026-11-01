import java.util.List;
import java.util.Scanner;

/**
 * Main.java — Entry point and console menu for the Lost & Found Management System.
 *
 * This class handles ONLY the user interface (menu display, input prompts,
 * formatted output). All database work is delegated to ItemDAO, and all
 * input validation is handled by InputHelper.
 */
public class Main {

    // Shared objects used throughout the menu loop
    private static final Scanner     scanner = new Scanner(System.in);
    private static final InputHelper input   = new InputHelper(scanner);
    private static final ItemDAO     dao     = new ItemDAO();

    // Categories offered when reporting an item
    private static final String[] CATEGORIES = {
        "Electronics", "Documents", "Bags", "Keys",
        "Jewellery", "Clothing", "Wallet", "Others"
    };

    // ─── MAIN METHOD ─────────────────────────────────────────────────

    public static void main(String[] args) {
        // Load the SQLite JDBC driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("[FATAL] SQLite JDBC driver not found in classpath.");
            System.out.println("Make sure sqlite-jdbc JAR is in the lib/ folder.");
            return;
        }

        // Create table and seed sample data (first run only)
        DBConnection.initializeDatabase();

        printBanner();

        // Menu loop — runs until the user picks 0 (Exit)
        boolean running = true;
        while (running) {
            printMenu();
            int choice = input.readInt("Enter your choice: ");

            switch (choice) {
                case 1  -> reportItem("LOST");
                case 2  -> reportItem("FOUND");
                case 3  -> viewAllItems();
                case 4  -> searchByName();
                case 5  -> searchByCategory();
                case 6  -> filterByType();
                case 7  -> updateItemDetails();
                case 8  -> markAsClaimed();
                case 9  -> deleteItem();
                case 10 -> showSummary();
                case 0  -> {
                    running = false;
                    System.out.println("\nThank you for using the Lost & Found System. Goodbye!\n");
                }
                default -> System.out.println("\n[!] Invalid choice. Please pick 0-10.\n");
            }
        }
        scanner.close();
    }

    // ─── MENU DISPLAY ────────────────────────────────────────────────

    /** Prints the application banner once at startup. */
    private static void printBanner() {
        System.out.println("==========================================================");
        System.out.println("        LOST  &  FOUND  MANAGEMENT  SYSTEM");
        System.out.println("==========================================================");
    }

    /** Prints the numbered menu options. */
    private static void printMenu() {
        System.out.println("----------------------------------------------------------");
        System.out.println("                      MAIN  MENU");
        System.out.println("----------------------------------------------------------");
        System.out.println("  1.  Report LOST item");
        System.out.println("  2.  Report FOUND item");
        System.out.println("  3.  View all items");
        System.out.println("  4.  Search by item name");
        System.out.println("  5.  Search by category");
        System.out.println("  6.  Filter by type (LOST / FOUND)");
        System.out.println("  7.  Update item details");
        System.out.println("  8.  Mark item as CLAIMED");
        System.out.println("  9.  Delete item");
        System.out.println("  10. Summary");
        System.out.println("  0.  Exit");
        System.out.println("----------------------------------------------------------");
    }

    // ─── 1 & 2: REPORT ITEM ─────────────────────────────────────────

    /**
     * Collects details from the user and inserts a new LOST or FOUND item.
     * @param type "LOST" or "FOUND"
     */
    private static void reportItem(String type) {
        System.out.println("\n--- Report " + type + " Item ---\n");

        Item item = new Item();
        item.setType(type);
        item.setStatus("OPEN");

        item.setName(input.readNonEmptyString("Item Name        : "));
        item.setCategory(input.readChoice("Select Category:", CATEGORIES));
        item.setDescription(input.readOptionalString("Description      : (optional, press Enter to skip) "));
        item.setLocation(input.readNonEmptyString("Location         : "));
        item.setItemDate(input.readDate("Date (" + type.toLowerCase() + ") [yyyy-MM-dd]: "));

        System.out.println("\n-- Reporter / Owner Details --");
        item.setOwnerName(input.readNonEmptyString("Your Name        : "));
        item.setOwnerPhone(input.readPhone("Phone (10 digits): "));
        item.setOwnerEmail(input.readOptionalString("Email            : (optional, press Enter to skip) "));

        int id = dao.addItem(item);
        if (id > 0) {
            System.out.println("\n>> Item added successfully with ID " + id + ".\n");
        } else {
            System.out.println("\n>> Failed to add item. Please try again.\n");
        }
    }

    // ─── 3: VIEW ALL ─────────────────────────────────────────────────

    /** Fetches and displays every item as an aligned table. */
    private static void viewAllItems() {
        System.out.println("\n--- All Items ---\n");
        List<Item> items = dao.getAllItems();
        printTable(items);
    }

    // ─── 4: SEARCH BY NAME ──────────────────────────────────────────

    /** Searches items whose name contains the user's keyword. */
    private static void searchByName() {
        System.out.println("\n--- Search by Item Name ---\n");
        String keyword = input.readNonEmptyString("Enter keyword: ");
        List<Item> items = dao.searchByName(keyword);
        System.out.println("\nResults for \"" + keyword + "\":\n");
        printTable(items);
    }

    // ─── 5: SEARCH BY CATEGORY ──────────────────────────────────────

    /** Searches items by an exact category match. */
    private static void searchByCategory() {
        System.out.println("\n--- Search by Category ---\n");
        String category = input.readChoice("Select Category:", CATEGORIES);
        List<Item> items = dao.searchByCategory(category);
        System.out.println("\nResults for category \"" + category + "\":\n");
        printTable(items);
    }

    // ─── 6: FILTER BY TYPE ──────────────────────────────────────────

    /** Shows only LOST or only FOUND items. */
    private static void filterByType() {
        System.out.println("\n--- Filter by Type ---\n");
        String type = input.readChoice("Select Type:", new String[]{"LOST", "FOUND"});
        List<Item> items = dao.filterByType(type);
        System.out.println("\nAll " + type + " items:\n");
        printTable(items);
    }

    // ─── 7: UPDATE ITEM DETAILS ─────────────────────────────────────

    /** Lets the user edit the details of an existing item. */
    private static void updateItemDetails() {
        System.out.println("\n--- Update Item Details ---\n");
        int id = input.readInt("Enter Item ID to update: ");

        Item item = dao.getItemById(id);
        if (item == null) {
            System.out.println("\n>> No item found with ID " + id + ".\n");
            return;
        }

        // Show current values so the user knows what they're changing
        System.out.println("\nCurrent details:");
        printSingleItem(item);
        System.out.println("\n(Press Enter to keep the current value)\n");

        // For each field: read input; if blank, keep the old value
        String val;

        val = input.readOptionalString("Item Name        [" + item.getName() + "]: ");
        if (val != null) item.setName(val);

        System.out.println("Current category: " + item.getCategory());
        if (input.readYesNo("Change category?")) {
            item.setCategory(input.readChoice("Select new Category:", CATEGORIES));
        }

        val = input.readOptionalString("Description      [" + orBlank(item.getDescription()) + "]: ");
        if (val != null) item.setDescription(val);

        val = input.readOptionalString("Location         [" + item.getLocation() + "]: ");
        if (val != null) item.setLocation(val);

        System.out.println("Current date: " + item.getItemDate());
        if (input.readYesNo("Change date?")) {
            item.setItemDate(input.readDate("New date [yyyy-MM-dd]: "));
        }

        System.out.println("Current type: " + item.getType());
        if (input.readYesNo("Change type (LOST/FOUND)?")) {
            item.setType(input.readChoice("Select Type:", new String[]{"LOST", "FOUND"}));
        }

        val = input.readOptionalString("Owner Name       [" + item.getOwnerName() + "]: ");
        if (val != null) item.setOwnerName(val);

        System.out.println("Current phone: " + item.getOwnerPhone());
        if (input.readYesNo("Change phone?")) {
            item.setOwnerPhone(input.readPhone("New phone (10 digits): "));
        }

        val = input.readOptionalString("Email            [" + orBlank(item.getOwnerEmail()) + "]: ");
        if (val != null) item.setOwnerEmail(val);

        if (dao.updateItem(item)) {
            System.out.println("\n>> Item ID " + id + " updated successfully.\n");
        } else {
            System.out.println("\n>> Failed to update item.\n");
        }
    }

    // ─── 8: MARK AS CLAIMED ─────────────────────────────────────────

    /** Changes an item's status to CLAIMED (or back to OPEN). */
    private static void markAsClaimed() {
        System.out.println("\n--- Mark Item as CLAIMED ---\n");
        int id = input.readInt("Enter Item ID: ");

        Item item = dao.getItemById(id);
        if (item == null) {
            System.out.println("\n>> No item found with ID " + id + ".\n");
            return;
        }

        System.out.println("Current status: " + item.getStatus());
        if (item.getStatus().equals("CLAIMED")) {
            if (input.readYesNo("This item is already CLAIMED. Re-open it?")) {
                if (dao.updateStatus(id, "OPEN")) {
                    System.out.println("\n>> Item ID " + id + " status changed to OPEN.\n");
                } else {
                    System.out.println("\n>> Failed to update status.\n");
                }
            }
        } else {
            if (input.readYesNo("Mark this item as CLAIMED?")) {
                if (dao.updateStatus(id, "CLAIMED")) {
                    System.out.println("\n>> Item ID " + id + " marked as CLAIMED.\n");
                } else {
                    System.out.println("\n>> Failed to update status.\n");
                }
            }
        }
    }

    // ─── 9: DELETE ITEM ──────────────────────────────────────────────

    /** Deletes an item after user confirmation. */
    private static void deleteItem() {
        System.out.println("\n--- Delete Item ---\n");
        int id = input.readInt("Enter Item ID to delete: ");

        Item item = dao.getItemById(id);
        if (item == null) {
            System.out.println("\n>> No item found with ID " + id + ".\n");
            return;
        }

        printSingleItem(item);

        if (input.readYesNo("\nAre you sure you want to delete this item?")) {
            if (dao.deleteItem(id)) {
                System.out.println("\n>> Item ID " + id + " deleted successfully.\n");
            } else {
                System.out.println("\n>> Failed to delete item.\n");
            }
        } else {
            System.out.println("\n>> Delete cancelled.\n");
        }
    }

    // ─── 10: SUMMARY ────────────────────────────────────────────────

    /** Shows a quick statistical summary of items in the database. */
    private static void showSummary() {
        System.out.println("\n--- Summary ---\n");

        int total      = dao.countItems(null,    null);
        int totalLost  = dao.countItems("LOST",  null);
        int totalFound = dao.countItems("FOUND", null);
        int openItems  = dao.countItems(null,    "OPEN");
        int claimed    = dao.countItems(null,    "CLAIMED");
        int lostOpen   = dao.countItems("LOST",  "OPEN");
        int foundOpen  = dao.countItems("FOUND", "OPEN");

        System.out.println("+-------------------------+-------+");
        System.out.println("| Metric                  | Count |");
        System.out.println("+-------------------------+-------+");
        System.out.printf( "| Total Items             | %5d |%n", total);
        System.out.printf( "| Total LOST              | %5d |%n", totalLost);
        System.out.printf( "| Total FOUND             | %5d |%n", totalFound);
        System.out.printf( "| Open (unclaimed)        | %5d |%n", openItems);
        System.out.printf( "| Claimed                 | %5d |%n", claimed);
        System.out.printf( "| LOST & still OPEN       | %5d |%n", lostOpen);
        System.out.printf( "| FOUND & still OPEN      | %5d |%n", foundOpen);
        System.out.println("+-------------------------+-------+");
        System.out.println();
    }

    // ─── TABLE PRINTING ─────────────────────────────────────────────

    /**
     * Prints a list of items as a neatly aligned text table.
     * If the list is empty, prints a friendly "No records found" message.
     */
    private static void printTable(List<Item> items) {
        if (items.isEmpty()) {
            System.out.println("  (No records found)\n");
            return;
        }

        // Header and data format strings (ID is int in data, String in header)
        String hdrFmt  = "| %-4s | %-18s | %-12s | %-15s | %-10s | %-5s | %-7s | %-15s | %-10s |%n";
        String dataFmt = "| %-4d | %-18s | %-12s | %-15s | %-10s | %-5s | %-7s | %-15s | %-10s |%n";
        String line = "+------+--------------------+--------------+-----------------+------------+-------+---------+-----------------+------------+";

        System.out.println(line);
        System.out.printf(hdrFmt, "ID", "Name", "Category", "Location", "Date", "Type", "Status", "Owner", "Phone");
        System.out.println(line);

        for (Item item : items) {
            System.out.printf(dataFmt,
                    item.getItemId(),
                    truncate(item.getName(), 18),
                    truncate(item.getCategory(), 12),
                    truncate(item.getLocation(), 15),
                    item.getItemDate(),
                    item.getType(),
                    item.getStatus(),
                    truncate(item.getOwnerName(), 15),
                    item.getOwnerPhone()
            );
        }
        System.out.println(line);
        System.out.println("  Total: " + items.size() + " record(s)\n");
    }

    /**
     * Prints a single item in a vertical key-value format.
     * Used before update/delete so the user can see what they're changing.
     */
    private static void printSingleItem(Item item) {
        System.out.println("+-------------------+--------------------------------------+");
        System.out.printf( "| %-17s | %-36s |%n", "Item ID",     item.getItemId());
        System.out.printf( "| %-17s | %-36s |%n", "Name",        item.getName());
        System.out.printf( "| %-17s | %-36s |%n", "Category",    item.getCategory());
        System.out.printf( "| %-17s | %-36s |%n", "Description", orBlank(item.getDescription()));
        System.out.printf( "| %-17s | %-36s |%n", "Location",    item.getLocation());
        System.out.printf( "| %-17s | %-36s |%n", "Date",        item.getItemDate());
        System.out.printf( "| %-17s | %-36s |%n", "Type",        item.getType());
        System.out.printf( "| %-17s | %-36s |%n", "Status",      item.getStatus());
        System.out.printf( "| %-17s | %-36s |%n", "Owner Name",  item.getOwnerName());
        System.out.printf( "| %-17s | %-36s |%n", "Phone",       item.getOwnerPhone());
        System.out.printf( "| %-17s | %-36s |%n", "Email",       orBlank(item.getOwnerEmail()));
        System.out.println("+-------------------+--------------------------------------+");
    }

    // ─── UTILITY METHODS ─────────────────────────────────────────────

    /** Truncates a string to maxLen characters, adding ".." if cut off. */
    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 2) + "..";
    }

    /** Returns the string, or "(none)" if it's null or blank. */
    private static String orBlank(String s) {
        return (s == null || s.isBlank()) ? "(none)" : s;
    }
}
