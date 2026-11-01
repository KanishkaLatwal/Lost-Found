import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DBConnection.java — Manages the SQLite database connection and initialization.
 *
 * Responsibilities:
 *   1. Provide a reusable getConnection() method so other classes can get a Connection.
 *   2. Create the 'item' table if it does not exist (initializeDatabase).
 *   3. Seed 6 sample records the very first time the table is empty.
 */
public class DBConnection {

    // SQLite database file — created in the project's working directory.
    private static final String DB_URL = "jdbc:sqlite:lostfound.db";

    /**
     * Returns a new Connection to the SQLite database.
     * Each caller is responsible for closing it (use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Creates the item table (if it doesn't exist) and seeds sample data
     * when the table is empty. Call this once at application startup.
     */
    public static void initializeDatabase() {
        // SQL to create the table
        String createTable = """
                CREATE TABLE IF NOT EXISTS item (
                    item_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    name        TEXT NOT NULL,
                    category    TEXT NOT NULL,
                    description TEXT,
                    location    TEXT NOT NULL,
                    item_date   TEXT NOT NULL,
                    type        TEXT NOT NULL CHECK(type IN ('LOST','FOUND')),
                    status      TEXT NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN','CLAIMED')),
                    owner_name  TEXT NOT NULL,
                    owner_phone TEXT NOT NULL,
                    owner_email TEXT
                );
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Step 1: Create the table
            stmt.execute(createTable);

            // Step 2: Check if any rows exist
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM item");
            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();

            // Step 3: Seed sample data only when the table is empty
            if (count == 0) {
                seedSampleData(conn);
                System.out.println("[INFO] Database created and 6 sample records inserted.");
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to initialize database: " + e.getMessage());
        }
    }

    /**
     * Inserts 6 diverse sample records so the demo works immediately.
     */
    private static void seedSampleData(Connection conn) throws SQLException {
        String sql = "INSERT INTO item (name, category, description, location, item_date, "
                   + "type, status, owner_name, owner_phone, owner_email) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // Each row: name, category, description, location, date, type, status,
        //           ownerName, ownerPhone, ownerEmail
        String[][] samples = {
            {"Blue Backpack",   "Bags",        "Navy blue with laptop compartment",
             "Library Block A", "2026-10-20", "LOST",  "OPEN",
             "Aarav Sharma",    "9876543210",  "aarav@email.com"},

            {"iPhone 14",       "Electronics", "Silver, cracked screen protector",
             "Canteen",         "2026-10-18", "LOST",  "OPEN",
             "Priya Mehta",     "8765432109",  "priya@email.com"},

            {"Aadhar Card",     "Documents",   "Found near main gate",
             "Main Gate",       "2026-10-19", "FOUND", "OPEN",
             "Rohan Verma",     "7654321098",  null},

            {"Car Keys (Honda)","Keys",        "Honda key with red keychain",
             "Parking Lot B",   "2026-10-15", "FOUND", "CLAIMED",
             "Sneha Patel",     "6543210987",  "sneha@email.com"},

            {"Gold Ring",       "Jewellery",   "Small gold ring with initials 'RS'",
             "Sports Ground",   "2026-10-21", "LOST",  "OPEN",
             "Rahul Singh",     "9988776655",  null},

            {"Black Wallet",    "Wallet",      "Leather wallet with college ID inside",
             "Lecture Hall 3",  "2026-10-22", "FOUND", "OPEN",
             "Ananya Gupta",    "8877665544",  "ananya@email.com"}
        };

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] row : samples) {
                for (int i = 0; i < row.length; i++) {
                    ps.setString(i + 1, row[i]);   // null values are handled by setString
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
