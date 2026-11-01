/**
 * Item.java — Model class representing a lost or found item.
 *
 * Each Item object maps to one row in the 'item' table.
 * Uses encapsulation: all fields are private with public getters/setters.
 */
public class Item {

    // ── Fields (match the database columns) ──────────────────────────

    private int    itemId;       // auto-generated primary key
    private String name;         // item name, e.g. "Blue Backpack"
    private String category;     // e.g. Electronics, Documents, Keys …
    private String description;  // optional extra details
    private String location;     // where the item was lost/found
    private String itemDate;     // date in yyyy-MM-dd format
    private String type;         // "LOST" or "FOUND"
    private String status;       // "OPEN" or "CLAIMED"
    private String ownerName;    // name of the person reporting
    private String ownerPhone;   // 10-digit phone number
    private String ownerEmail;   // optional email address

    // ── Constructors ─────────────────────────────────────────────────

    /** No-arg constructor (needed when building an Item from a ResultSet). */
    public Item() {
    }

    /** Full constructor (used when creating a new item to insert). */
    public Item(int itemId, String name, String category, String description,
                String location, String itemDate, String type, String status,
                String ownerName, String ownerPhone, String ownerEmail) {
        this.itemId      = itemId;
        this.name        = name;
        this.category    = category;
        this.description = description;
        this.location    = location;
        this.itemDate    = itemDate;
        this.type        = type;
        this.status      = status;
        this.ownerName   = ownerName;
        this.ownerPhone  = ownerPhone;
        this.ownerEmail  = ownerEmail;
    }

    // ── Getters & Setters ────────────────────────────────────────────

    public int    getItemId()      { return itemId; }
    public void   setItemId(int itemId) { this.itemId = itemId; }

    public String getName()        { return name; }
    public void   setName(String name) { this.name = name; }

    public String getCategory()    { return category; }
    public void   setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void   setDescription(String description) { this.description = description; }

    public String getLocation()    { return location; }
    public void   setLocation(String location) { this.location = location; }

    public String getItemDate()    { return itemDate; }
    public void   setItemDate(String itemDate) { this.itemDate = itemDate; }

    public String getType()        { return type; }
    public void   setType(String type) { this.type = type; }

    public String getStatus()      { return status; }
    public void   setStatus(String status) { this.status = status; }

    public String getOwnerName()   { return ownerName; }
    public void   setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerPhone()  { return ownerPhone; }
    public void   setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }

    public String getOwnerEmail()  { return ownerEmail; }
    public void   setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    // ── toString (for quick debugging) ───────────────────────────────

    @Override
    public String toString() {
        return "Item{" +
                "id=" + itemId +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", location='" + location + '\'' +
                ", date='" + itemDate + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", owner='" + ownerName + '\'' +
                ", phone='" + ownerPhone + '\'' +
                '}';
    }
}
