import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemDAO.java — Data Access Object for the 'item' table.
 *
 * This is the ONLY class that contains SQL statements.
 * Every method uses PreparedStatement to prevent SQL injection
 * and try-with-resources to ensure connections are always closed.
 */
public class ItemDAO {

    // ─── CREATE ──────────────────────────────────────────────────────

    /**
     * Inserts a new item into the database.
     * @return the auto-generated item_id, or -1 on failure.
     */
    public int addItem(Item item) {
        String sql = "INSERT INTO item (name, category, description, location, item_date, "
                   + "type, status, owner_name, owner_phone, owner_email) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1,  item.getName());
            ps.setString(2,  item.getCategory());
            ps.setString(3,  item.getDescription());
            ps.setString(4,  item.getLocation());
            ps.setString(5,  item.getItemDate());
            ps.setString(6,  item.getType());
            ps.setString(7,  item.getStatus());
            ps.setString(8,  item.getOwnerName());
            ps.setString(9,  item.getOwnerPhone());
            ps.setString(10, item.getOwnerEmail());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Could not add item: " + e.getMessage());
        }
        return -1;
    }

    // ─── READ ────────────────────────────────────────────────────────

    /** Returns every item in the table, ordered by most recent date first. */
    public List<Item> getAllItems() {
        String sql = "SELECT * FROM item ORDER BY item_date DESC, item_id DESC";
        List<Item> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Could not fetch items: " + e.getMessage());
        }
        return list;
    }

    /** Searches items whose name contains the keyword (case-insensitive). */
    public List<Item> searchByName(String keyword) {
        String sql = "SELECT * FROM item WHERE LOWER(name) LIKE LOWER(?) ORDER BY item_date DESC";
        List<Item> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Search failed: " + e.getMessage());
        }
        return list;
    }

    /** Returns all items in a given category (exact match, case-insensitive). */
    public List<Item> searchByCategory(String category) {
        String sql = "SELECT * FROM item WHERE LOWER(category) = LOWER(?) ORDER BY item_date DESC";
        List<Item> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Category search failed: " + e.getMessage());
        }
        return list;
    }

    /** Returns all items of a given type — LOST or FOUND. */
    public List<Item> filterByType(String type) {
        String sql = "SELECT * FROM item WHERE type = ? ORDER BY item_date DESC";
        List<Item> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, type.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Filter failed: " + e.getMessage());
        }
        return list;
    }

    /** Fetches a single item by its ID, or null if not found. */
    public Item getItemById(int id) {
        String sql = "SELECT * FROM item WHERE item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Could not fetch item: " + e.getMessage());
        }
        return null;
    }

    // ─── UPDATE ──────────────────────────────────────────────────────

    /** Updates all editable fields of an existing item. */
    public boolean updateItem(Item item) {
        String sql = "UPDATE item SET name = ?, category = ?, description = ?, location = ?, "
                   + "item_date = ?, type = ?, owner_name = ?, owner_phone = ?, owner_email = ? "
                   + "WHERE item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1,  item.getName());
            ps.setString(2,  item.getCategory());
            ps.setString(3,  item.getDescription());
            ps.setString(4,  item.getLocation());
            ps.setString(5,  item.getItemDate());
            ps.setString(6,  item.getType());
            ps.setString(7,  item.getOwnerName());
            ps.setString(8,  item.getOwnerPhone());
            ps.setString(9,  item.getOwnerEmail());
            ps.setInt(10, item.getItemId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] Could not update item: " + e.getMessage());
        }
        return false;
    }

    /** Marks an item as CLAIMED or re-opens it to OPEN. */
    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE item SET status = ? WHERE item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] Could not update status: " + e.getMessage());
        }
        return false;
    }

    // ─── DELETE ──────────────────────────────────────────────────────

    /** Deletes an item by its ID. */
    public boolean deleteItem(int id) {
        String sql = "DELETE FROM item WHERE item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] Could not delete item: " + e.getMessage());
        }
        return false;
    }

    // ─── SUMMARY HELPER ─────────────────────────────────────────────

    /**
     * Counts items matching a given type and status.
     * Pass null for a parameter to skip that filter.
     *   countItems("LOST", "OPEN")    → open lost items
     *   countItems("FOUND", null)     → all found items
     *   countItems(null, null)        → total items
     */
    public int countItems(String type, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM item WHERE 1=1");
        List<String> params = new ArrayList<>();

        if (type != null) {
            sql.append(" AND type = ?");
            params.add(type);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Could not count items: " + e.getMessage());
        }
        return 0;
    }

    // ─── PRIVATE HELPER ─────────────────────────────────────────────

    /**
     * Maps the current row of a ResultSet to an Item object.
     * Called by every read method to avoid duplicating mapping code.
     */
    private Item mapRow(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setItemId(rs.getInt("item_id"));
        item.setName(rs.getString("name"));
        item.setCategory(rs.getString("category"));
        item.setDescription(rs.getString("description"));
        item.setLocation(rs.getString("location"));
        item.setItemDate(rs.getString("item_date"));
        item.setType(rs.getString("type"));
        item.setStatus(rs.getString("status"));
        item.setOwnerName(rs.getString("owner_name"));
        item.setOwnerPhone(rs.getString("owner_phone"));
        item.setOwnerEmail(rs.getString("owner_email"));
        return item;
    }
}
