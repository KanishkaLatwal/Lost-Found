# Lost & Found Management System

A simple, menu-driven Java console application that records lost and found items along with owner/reporter details, stores everything in an SQLite database, and lets the user view, search, update, and delete records.

> **College Minor Project** — built with clean, well-commented code that is easy to explain in a viva.

---

## Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Report LOST item | Record an item that someone has lost |
| 2 | Report FOUND item | Record an item that someone has found |
| 3 | View all items | Display every record in a formatted table |
| 4 | Search by name | Case-insensitive keyword search |
| 5 | Search by category | Filter by Electronics, Documents, Keys, etc. |
| 6 | Filter by type | Show only LOST or only FOUND items |
| 7 | Update item details | Edit any field of an existing record |
| 8 | Mark as CLAIMED | Change status to CLAIMED (or re-open) |
| 9 | Delete item | Remove a record (with confirmation) |
| 10 | Summary | Dashboard with counts of lost, found, open, claimed |

---

## Tech Stack

- **Language:** Java 17 (core Java only)
- **UI:** Console-based, menu-driven (Scanner)
- **Database:** SQLite via `sqlite-jdbc` (single file: `lostfound.db`)
- **Build:** Plain `javac` / `java` — no Maven, no Gradle
- **Pattern:** DAO (Data Access Object) for clean separation of SQL from UI

---

## Folder Structure

```
LostAndFound/
 ├── lib/
 │    └── sqlite-jdbc-3.36.0.3.jar     ← SQLite JDBC driver
 ├── src/
 │    ├── Item.java                     ← Model (POJO with getters/setters)
 │    ├── DBConnection.java             ← Connection helper + table creation
 │    ├── ItemDAO.java                  ← All SQL operations (CRUD)
 │    ├── InputHelper.java              ← Input validation utility
 │    └── Main.java                     ← Menu loop and console UI
 ├── compile.bat                        ← Compile script (Windows)
 ├── run.bat                            ← Run script (Windows)
 ├── README.md
 ├── VIVA_NOTES.md
 └── .gitignore
```

---

## Database Schema

```sql
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
```

On the first run, 6 sample records (mix of LOST/FOUND, various categories) are auto-inserted so the demo works immediately.

---

## Setup & Run (Windows)

### Prerequisites
1. **Install JDK 17** (or newer).  
   Download from [https://adoptium.net](https://adoptium.net) or [https://jdk.java.net](https://jdk.java.net).  
   Make sure `java` and `javac` are on your PATH:
   ```
   java -version
   javac -version
   ```

2. **SQLite JDBC driver** — already included in the `lib/` folder.  
   If missing, download `sqlite-jdbc-3.36.0.3.jar` from [Maven Central](https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.36.0.3/) and place it in `lib/`.

### Compile
```
compile.bat
```
This compiles all `.java` files in `src/` into the `out/` folder.

### Run
```
run.bat
```
The application starts and shows the main menu. The `lostfound.db` file is created automatically in the project root.

### Run from Command Line (manual)
```
javac -d out -cp "lib/*" src/*.java
java -cp "out;lib/*" Main
```

---

## Running in an IDE

### VS Code
1. Open the `LostAndFound/` folder.
2. Install the "Extension Pack for Java" extension.
3. Add `lib/sqlite-jdbc-3.36.0.3.jar` to `java.project.referencedLibraries` in `.vscode/settings.json`:
   ```json
   {
     "java.project.referencedLibraries": ["lib/*.jar"]
   }
   ```
4. Open `Main.java` → click **Run** above `main()`.

### IntelliJ IDEA
1. Open the `LostAndFound/` folder as a project.
2. Go to **File → Project Structure → Libraries → + → Java**.
3. Select `lib/sqlite-jdbc-3.36.0.3.jar`.
4. Mark `src/` as **Sources Root**.
5. Right-click `Main.java` → **Run 'Main'**.

---

## Sample Console Output

```
==========================================================
        LOST  &  FOUND  MANAGEMENT  SYSTEM
==========================================================
[INFO] Database created and 6 sample records inserted.
----------------------------------------------------------
                      MAIN  MENU
----------------------------------------------------------
  1.  Report LOST item
  2.  Report FOUND item
  3.  View all items
  ...
  0.  Exit
----------------------------------------------------------
Enter your choice: 3

--- All Items ---

+------+--------------------+--------------+-----------------+------------+-------+---------+-----------------+------------+
| ID   | Name               | Category     | Location        | Date       | Type  | Status  | Owner           | Phone      |
+------+--------------------+--------------+-----------------+------------+-------+---------+-----------------+------------+
| 6    | Black Wallet       | Wallet       | Lecture Hall 3  | 2026-10-22 | FOUND | OPEN    | Ananya Gupta    | 8877665544 |
| 5    | Gold Ring          | Jewellery    | Sports Ground   | 2026-10-21 | LOST  | OPEN    | Rahul Singh     | 9988776655 |
| 1    | Blue Backpack      | Bags         | Library Block A | 2026-10-20 | LOST  | OPEN    | Aarav Sharma    | 9876543210 |
| 3    | Aadhar Card        | Documents    | Main Gate       | 2026-10-19 | FOUND | OPEN    | Rohan Verma     | 7654321098 |
| 2    | iPhone 14          | Electronics  | Canteen         | 2026-10-18 | LOST  | OPEN    | Priya Mehta     | 8765432109 |
| 4    | Car Keys (Honda)   | Keys         | Parking Lot B   | 2026-10-15 | FOUND | CLAIMED | Sneha Patel     | 6543210987 |
+------+--------------------+--------------+-----------------+------------+-------+---------+-----------------+------------+
  Total: 6 record(s)
```

---

## Future Scope

- **GUI version** using JavaFX or Swing
- **Auto-matching** — suggest matches between LOST and FOUND items with similar names/categories
- **Image upload** — attach a photo of the item
- **Admin login** — role-based access with username/password
- **Email notifications** — notify the owner when their item is found
- **Web version** using Spring Boot + Thymeleaf
- **Export to CSV/PDF** — generate downloadable reports

---

## License

This project is created for educational purposes as a college minor project.
