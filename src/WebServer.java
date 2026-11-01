import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.List;

/**
 * WebServer.java — Lightweight HTTP server for the Lost & Found web frontend.
 *
 * Uses Java's built-in com.sun.net.httpserver (no external frameworks needed).
 * Serves the static HTML/CSS from the frontend/ folder and exposes a REST API
 * that reuses the existing ItemDAO and DBConnection classes.
 *
 * Endpoints:
 *   GET    /                          → serves index.html
 *   GET    /style.css                 → serves style.css
 *   GET    /api/items                 → all items (JSON array)
 *   GET    /api/items/search?...      → search/filter items
 *   GET    /api/items/{id}            → single item by ID
 *   POST   /api/items                 → add new item (JSON body)
 *   PUT    /api/items/{id}            → update item (JSON body)
 *   PUT    /api/items/{id}/status     → update status (JSON body)
 *   DELETE /api/items/{id}            → delete item
 *   GET    /api/summary               → summary counts
 */
public class WebServer {

    private static final int PORT = 8080;
    private static final ItemDAO dao = new ItemDAO();

    public static void main(String[] args) throws Exception {
        // Load SQLite driver and initialize database
        Class.forName("org.sqlite.JDBC");
        DBConnection.initializeDatabase();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Static file serving (frontend/)
        server.createContext("/", new StaticFileHandler());

        // API endpoints
        server.createContext("/api/summary",      WebServer::handleSummary);
        server.createContext("/api/items/search",  WebServer::handleSearch);
        server.createContext("/api/items",         WebServer::handleItems);

        server.setExecutor(null); // default single-threaded executor
        server.start();

        System.out.println("==========================================================");
        System.out.println("  Lost & Found Web Server running!");
        System.out.println("  Open in browser:  http://localhost:" + PORT);
        System.out.println("  Press Ctrl+C to stop.");
        System.out.println("==========================================================");
    }

    // ─── STATIC FILE HANDLER ─────────────────────────────────────────

    /**
     * Serves index.html and style.css from the frontend/ folder.
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            // Only serve known frontend files
            String fileName = path.substring(1); // remove leading /

            // Security: block path traversal attempts (e.g. ../../etc/passwd)
            if (fileName.contains("..") || fileName.contains("\\")) {
                String msg = "403 Forbidden";
                exchange.sendResponseHeaders(403, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
                exchange.getResponseBody().close();
                return;
            }

            Path frontendDir = Paths.get("frontend").toAbsolutePath().normalize();
            Path filePath = frontendDir.resolve(fileName).normalize();

            // Ensure resolved path is still inside the frontend/ directory
            if (!filePath.startsWith(frontendDir)) {
                String msg = "403 Forbidden";
                exchange.sendResponseHeaders(403, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
                exchange.getResponseBody().close();
                return;
            }

            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                byte[] bytes = Files.readAllBytes(filePath);
                String contentType = fileName.endsWith(".css") ? "text/css" :
                                     fileName.endsWith(".js")  ? "application/javascript" :
                                     "text/html";
                exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } else {
                String msg = "404 Not Found";
                exchange.sendResponseHeaders(404, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
            }
            exchange.getResponseBody().close();
        }
    }

    // ─── API: /api/summary ───────────────────────────────────────────

    private static void handleSummary(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        // Handle CORS preflight
        if (exchange.getRequestMethod().equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            exchange.getResponseBody().close();
            return;
        }
        if (!exchange.getRequestMethod().equals("GET")) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        int totalLost  = dao.countItems("LOST", null);
        int totalFound = dao.countItems("FOUND", null);
        int totalOpen  = dao.countItems(null, "OPEN");
        int totalClaimed = dao.countItems(null, "CLAIMED");

        String json = String.format(
            "{\"totalLost\":%d,\"totalFound\":%d,\"totalOpen\":%d,\"totalClaimed\":%d}",
            totalLost, totalFound, totalOpen, totalClaimed
        );
        sendJson(exchange, 200, json);
    }

    // ─── API: /api/items/search ──────────────────────────────────────

    private static void handleSearch(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        // Handle CORS preflight
        if (exchange.getRequestMethod().equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            exchange.getResponseBody().close();
            return;
        }
        if (!exchange.getRequestMethod().equals("GET")) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String keyword = getParam(query, "keyword");
        String category = getParam(query, "category");
        String type = getParam(query, "type");

        List<Item> items;

        // Priority: keyword > category > type > all
        if (keyword != null && !keyword.isEmpty()) {
            items = dao.searchByName(keyword);
            // Further filter by category and type in-memory if specified
            if (category != null && !category.isEmpty()) {
                items = items.stream()
                    .filter(i -> i.getCategory().equalsIgnoreCase(category))
                    .collect(java.util.stream.Collectors.toList());
            }
            if (type != null && !type.isEmpty()) {
                items = items.stream()
                    .filter(i -> i.getType().equalsIgnoreCase(type))
                    .collect(java.util.stream.Collectors.toList());
            }
        } else if (category != null && !category.isEmpty()) {
            items = dao.searchByCategory(category);
            if (type != null && !type.isEmpty()) {
                items = items.stream()
                    .filter(i -> i.getType().equalsIgnoreCase(type))
                    .collect(java.util.stream.Collectors.toList());
            }
        } else if (type != null && !type.isEmpty()) {
            items = dao.filterByType(type);
        } else {
            items = dao.getAllItems();
        }

        sendJson(exchange, 200, itemsToJson(items));
    }

    // ─── API: /api/items (+ /api/items/{id} + /api/items/{id}/status)

    private static void handleItems(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();

        // Handle CORS preflight
        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            exchange.getResponseBody().close();
            return;
        }

        // Parse item ID from path: /api/items/{id} or /api/items/{id}/status
        String remaining = path.substring("/api/items".length()); // e.g. "" or "/5" or "/5/status"
        int itemId = -1;
        boolean isStatusEndpoint = false;

        if (!remaining.isEmpty() && remaining.startsWith("/")) {
            String[] parts = remaining.substring(1).split("/");
            try {
                itemId = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, "{\"error\":\"Invalid item ID\"}");
                return;
            }
            if (parts.length > 1 && parts[1].equals("status")) {
                isStatusEndpoint = true;
            }
        }

        switch (method) {
            case "GET" -> {
                if (itemId > 0) {
                    // GET /api/items/{id}
                    Item item = dao.getItemById(itemId);
                    if (item == null) {
                        sendJson(exchange, 404, "{\"error\":\"Item not found\"}");
                    } else {
                        sendJson(exchange, 200, itemToJson(item));
                    }
                } else {
                    // GET /api/items
                    sendJson(exchange, 200, itemsToJson(dao.getAllItems()));
                }
            }
            case "POST" -> {
                // POST /api/items — add new item
                String body = readBody(exchange);
                Item item = jsonToItem(body);
                if (item.getStatus() == null) item.setStatus("OPEN");

                int newId = dao.addItem(item);
                if (newId > 0) {
                    sendJson(exchange, 201, "{\"success\":true,\"id\":" + newId + "}");
                } else {
                    sendJson(exchange, 500, "{\"success\":false,\"error\":\"Failed to add item\"}");
                }
            }
            case "PUT" -> {
                if (itemId <= 0) {
                    sendJson(exchange, 400, "{\"error\":\"Item ID required\"}");
                    return;
                }
                String body = readBody(exchange);

                if (isStatusEndpoint) {
                    // PUT /api/items/{id}/status
                    String status = extractJsonField(body, "status");
                    if (status != null && dao.updateStatus(itemId, status)) {
                        sendJson(exchange, 200, "{\"success\":true}");
                    } else {
                        sendJson(exchange, 500, "{\"success\":false}");
                    }
                } else {
                    // PUT /api/items/{id} — full update
                    Item item = jsonToItem(body);
                    item.setItemId(itemId);
                    // Keep existing status
                    Item existing = dao.getItemById(itemId);
                    if (existing == null) {
                        sendJson(exchange, 404, "{\"error\":\"Item not found\"}");
                        return;
                    }
                    if (item.getStatus() == null) item.setStatus(existing.getStatus());

                    if (dao.updateItem(item)) {
                        sendJson(exchange, 200, "{\"success\":true}");
                    } else {
                        sendJson(exchange, 500, "{\"success\":false}");
                    }
                }
            }
            case "DELETE" -> {
                if (itemId <= 0) {
                    sendJson(exchange, 400, "{\"error\":\"Item ID required\"}");
                    return;
                }
                if (dao.deleteItem(itemId)) {
                    sendJson(exchange, 200, "{\"success\":true}");
                } else {
                    sendJson(exchange, 500, "{\"success\":false}");
                }
            }
            default -> sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    // ─── JSON HELPERS ────────────────────────────────────────────────

    /** Converts a list of items to a JSON array string. */
    private static String itemsToJson(List<Item> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(itemToJson(items.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    /** Converts a single Item to a JSON object string. */
    private static String itemToJson(Item item) {
        return String.format(
            "{\"itemId\":%d,\"name\":%s,\"category\":%s,\"description\":%s," +
            "\"location\":%s,\"itemDate\":%s,\"type\":%s,\"status\":%s," +
            "\"ownerName\":%s,\"ownerPhone\":%s,\"ownerEmail\":%s}",
            item.getItemId(),
            jsonStr(item.getName()),
            jsonStr(item.getCategory()),
            jsonStr(item.getDescription()),
            jsonStr(item.getLocation()),
            jsonStr(item.getItemDate()),
            jsonStr(item.getType()),
            jsonStr(item.getStatus()),
            jsonStr(item.getOwnerName()),
            jsonStr(item.getOwnerPhone()),
            jsonStr(item.getOwnerEmail())
        );
    }

    /** Wraps a string in quotes for JSON, escaping special characters. Returns "null" for null. */
    private static String jsonStr(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t")
                    + "\"";
    }

    /**
     * Very simple JSON parser — extracts Item fields from a JSON object string.
     * Suitable for this project's simple flat JSON; does not handle nested objects.
     */
    private static Item jsonToItem(String json) {
        Item item = new Item();
        item.setName(extractJsonField(json, "name"));
        item.setCategory(extractJsonField(json, "category"));
        item.setDescription(extractJsonField(json, "description"));
        item.setLocation(extractJsonField(json, "location"));
        item.setItemDate(extractJsonField(json, "itemDate"));
        item.setType(extractJsonField(json, "type"));
        item.setStatus(extractJsonField(json, "status"));
        item.setOwnerName(extractJsonField(json, "ownerName"));
        item.setOwnerPhone(extractJsonField(json, "ownerPhone"));
        item.setOwnerEmail(extractJsonField(json, "ownerEmail"));

        // Parse itemId if present
        String idStr = extractJsonField(json, "itemId");
        if (idStr != null) {
            try { item.setItemId(Integer.parseInt(idStr)); } catch (NumberFormatException ignored) {}
        }
        return item;
    }

    /**
     * Extracts the value of a field from a simple JSON string.
     * Handles string values (in quotes) and numeric/null values.
     */
    private static String extractJsonField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;

        int colonIdx = json.indexOf(':', idx + key.length());
        if (colonIdx == -1) return null;

        // Skip whitespace after colon
        int start = colonIdx + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;

        if (start >= json.length()) return null;

        if (json.charAt(start) == '"') {
            // String value — find closing quote (handle escaped quotes)
            int end = start + 1;
            while (end < json.length()) {
                if (json.charAt(end) == '\\') { end += 2; continue; }
                if (json.charAt(end) == '"') break;
                end++;
            }
            String value = json.substring(start + 1, end);
            // Unescape
            value = value.replace("\\\"", "\"").replace("\\\\", "\\")
                         .replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
            return value;
        } else if (json.substring(start).startsWith("null")) {
            return null;
        } else {
            // Number or boolean — read until comma, }, or whitespace
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ' ') {
                end++;
            }
            return json.substring(start, end);
        }
    }

    // ─── HTTP HELPERS ────────────────────────────────────────────────

    /** Sends a JSON response with the given status code. */
    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        addCorsHeaders(exchange);
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    /** Reads the full request body as a string. */
    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    /** Adds CORS headers so the browser allows API calls. */
    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    /** Extracts a query parameter value from a URL query string. */
    private static String getParam(String query, String name) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}

