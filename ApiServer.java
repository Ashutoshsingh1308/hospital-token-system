import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * REST API Server for OPD Token Allocation Engine
 * 
 * Endpoints:
 *   POST /doctors              - Add a doctor
 *   POST /doctors/{name}/slots - Add slot to doctor
 *   POST /tokens               - Book a token
 *   DELETE /tokens/{id}        - Cancel a token
 *   PUT /tokens/{id}/noshow    - Mark token as no-show
 *   PUT /doctors/{name}/delay/{slotIndex} - Delay a slot
 *   GET /doctors               - Get all doctors status
 *   GET /doctors/{name}        - Get specific doctor status
 */
public class ApiServer {
    private final TokenManager manager;
    private HttpServer server;

    public ApiServer(TokenManager manager) {
        this.manager = manager;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Register endpoints
        server.createContext("/doctors", this::handleDoctors);
        server.createContext("/tokens", this::handleTokens);
        
        server.setExecutor(null);
        server.start();
        System.out.println("API Server started on http://localhost:" + port);
        System.out.println("\nAvailable Endpoints:");
        System.out.println("  POST   /doctors              - Add doctor {\"name\": \"...\"}");
        System.out.println("  POST   /doctors/{name}/slots - Add slot {\"start\": \"9:00 AM\", \"end\": \"10:00 AM\", \"capacity\": 5}");
        System.out.println("  POST   /tokens               - Book token {\"doctor\": \"...\", \"slot\": 0, \"patient\": \"...\", \"type\": \"ONLINE\"}");
        System.out.println("  DELETE /tokens/{id}?doctor=X - Cancel token");
        System.out.println("  PUT    /tokens/{id}/noshow?doctor=X - Mark no-show");
        System.out.println("  PUT    /doctors/{name}/delay/{slot} - Delay slot");
        System.out.println("  GET    /doctors              - Get all doctors");
        System.out.println("  GET    /doctors/{name}       - Get specific doctor");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleDoctors(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        String response = "";
        int statusCode = 200;

        try {
            if (path.equals("/doctors")) {
                if ("GET".equals(method)) {
                    response = getAllDoctorsJson();
                } else if ("POST".equals(method)) {
                    Map<String, String> body = parseJsonBody(exchange);
                    String name = body.get("name");
                    if (name != null) {
                        manager.addDoctor(name);
                        response = "{\"success\": true, \"message\": \"Doctor " + name + " added\"}";
                    } else {
                        statusCode = 400;
                        response = "{\"error\": \"name is required\"}";
                    }
                }
            } else if (path.matches("/doctors/[^/]+/slots")) {
                String doctorName = path.split("/")[2];
                if ("POST".equals(method)) {
                    Map<String, String> body = parseJsonBody(exchange);
                    Doctor doctor = manager.getDoctor(doctorName);
                    if (doctor != null) {
                        doctor.addSlot(body.get("start"), body.get("end"), 
                            Integer.parseInt(body.getOrDefault("capacity", "5")));
                        response = "{\"success\": true, \"message\": \"Slot added\"}";
                    } else {
                        statusCode = 404;
                        response = "{\"error\": \"Doctor not found\"}";
                    }
                }
            } else if (path.matches("/doctors/[^/]+/delay/\\d+")) {
                String[] parts = path.split("/");
                String doctorName = parts[2];
                int slotIndex = Integer.parseInt(parts[4]);
                if ("PUT".equals(method)) {
                    manager.delaySlot(doctorName, slotIndex);
                    response = "{\"success\": true, \"message\": \"Slot delayed\"}";
                }
            } else if (path.matches("/doctors/[^/]+")) {
                String doctorName = path.split("/")[2];
                if ("GET".equals(method)) {
                    Doctor doctor = manager.getDoctor(doctorName);
                    if (doctor != null) {
                        response = getDoctorJson(doctor);
                    } else {
                        statusCode = 404;
                        response = "{\"error\": \"Doctor not found\"}";
                    }
                }
            }
        } catch (Exception e) {
            statusCode = 500;
            response = "{\"error\": \"" + e.getMessage() + "\"}";
        }

        sendResponse(exchange, statusCode, response);
    }

    private void handleTokens(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        String method = exchange.getRequestMethod();
        String response = "";
        int statusCode = 200;

        try {
            if (path.equals("/tokens") && "POST".equals(method)) {
                Map<String, String> body = parseJsonBody(exchange);
                String doctor = body.get("doctor");
                int slot = Integer.parseInt(body.getOrDefault("slot", "0"));
                String patient = body.get("patient");
                String type = body.getOrDefault("type", "ONLINE");
                
                Token token = manager.bookToken(doctor, slot, patient, TokenType.valueOf(type));
                if (token != null) {
                    response = "{\"success\": true, \"tokenId\": \"" + token.getId() + "\", \"patient\": \"" + 
                        token.getPatientName() + "\", \"type\": \"" + token.getType() + "\"}";
                } else {
                    statusCode = 400;
                    response = "{\"error\": \"Failed to book token\"}";
                }
            } else if (path.matches("/tokens/T\\d+/noshow")) {
                String tokenId = path.split("/")[2];
                String doctorName = getQueryParam(query, "doctor");
                if ("PUT".equals(method)) {
                    boolean success = manager.markNoShow(doctorName, tokenId);
                    response = "{\"success\": " + success + "}";
                }
            } else if (path.matches("/tokens/T\\d+")) {
                String tokenId = path.split("/")[2];
                String doctorName = getQueryParam(query, "doctor");
                if ("DELETE".equals(method)) {
                    boolean success = manager.cancelToken(doctorName, tokenId);
                    response = "{\"success\": " + success + "}";
                }
            }
        } catch (Exception e) {
            statusCode = 500;
            response = "{\"error\": \"" + e.getMessage() + "\"}";
        }

        sendResponse(exchange, statusCode, response);
    }

    private String getAllDoctorsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"doctors\": [");
        boolean first = true;
        for (Doctor doctor : getDoctors()) {
            if (!first) sb.append(",");
            sb.append(getDoctorJson(doctor));
            first = false;
        }
        sb.append("]}");
        return sb.toString();
    }

    private String getDoctorJson(Doctor doctor) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"name\": \"").append(doctor.getName()).append("\", \"slots\": [");
        List<Slot> slots = doctor.getSlots();
        for (int i = 0; i < slots.size(); i++) {
            if (i > 0) sb.append(",");
            Slot slot = slots.get(i);
            sb.append("{\"time\": \"").append(slot.getTimeRange())
              .append("\", \"capacity\": ").append(slot.getCapacity())
              .append(", \"current\": ").append(slot.getCurrentCount())
              .append(", \"tokens\": [");
            List<Token> tokens = slot.getTokens();
            for (int j = 0; j < tokens.size(); j++) {
                if (j > 0) sb.append(",");
                Token t = tokens.get(j);
                sb.append("{\"id\": \"").append(t.getId())
                  .append("\", \"patient\": \"").append(t.getPatientName())
                  .append("\", \"type\": \"").append(t.getType()).append("\"}");
            }
            sb.append("]}");
        }
        sb.append("], \"waitingList\": ").append(doctor.getWaitingList().size()).append("}");
        return sb.toString();
    }

    private Collection<Doctor> getDoctors() {
        return manager.getAllDoctors();
    }

    private Map<String, String> parseJsonBody(HttpExchange exchange) throws IOException {
        Map<String, String> result = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        // Simple JSON parsing (no external libraries)
        String json = body.toString().replaceAll("[{}\"]", "");
        for (String pair : json.split(",")) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                result.put(kv[0].trim(), kv[1].trim());
            }
        }
        return result;
    }

    private String getQueryParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals(param)) {
                return kv[1];
            }
        }
        return null;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
