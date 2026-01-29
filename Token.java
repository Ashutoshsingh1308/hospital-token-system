import java.time.LocalDateTime;

/**
 * Represents a patient token in the hospital queue system
 */
public class Token {
    private static int counter = 0;
    
    private final String id;
    private final String patientName;
    private final TokenType type;
    private final LocalDateTime createdAt;
    private LocalDateTime allocatedAt;

    public Token(String patientName, TokenType type) {
        this.id = String.format("T%03d", ++counter);
        this.patientName = patientName;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getPatientName() {
        return patientName;
    }

    public TokenType getType() {
        return type;
    }

    public int getPriority() {
        return type.getPriority();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    public void setAllocatedAt(LocalDateTime allocatedAt) {
        this.allocatedAt = allocatedAt;
    }

    @Override
    public String toString() {
        return String.format("%s - %-12s [%-9s] %s", id, patientName, type, type.getIcon());
    }

    // Reset counter for testing
    public static void resetCounter() {
        counter = 0;
    }
}
