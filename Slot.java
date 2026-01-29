import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a time slot for a doctor with token capacity
 */
public class Slot {
    private final String startTime;
    private final String endTime;
    private final int capacity;
    private final List<Token> tokens;

    public Slot(String startTime, String endTime, int capacity) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.tokens = new ArrayList<>();
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public List<Token> getTokens() {
        return new ArrayList<>(tokens);
    }

    public int getCurrentCount() {
        return tokens.size();
    }

    public boolean isFull() {
        return tokens.size() >= capacity;
    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    /**
     * Add token and maintain priority order (lower priority number = higher priority)
     */
    public void addToken(Token token) {
        tokens.add(token);
        // Sort by priority (ascending), then by creation time
        tokens.sort(Comparator
            .comparingInt(Token::getPriority)
            .thenComparing(Token::getCreatedAt));
    }

    /**
     * Remove a specific token
     */
    public boolean removeToken(Token token) {
        return tokens.remove(token);
    }

    /**
     * Remove token by ID
     */
    public Token removeTokenById(String tokenId) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getId().equals(tokenId)) {
                return tokens.remove(i);
            }
        }
        return null;
    }

    /**
     * Get the lowest priority token in this slot (highest priority number)
     */
    public Token getLowestPriorityToken() {
        if (tokens.isEmpty()) return null;
        return tokens.get(tokens.size() - 1);
    }

    /**
     * Find token by ID - simple loop through all tokens
     */
    public Token findTokenById(String tokenId) {
        for (Token t : tokens) {
            if (t.getId().equals(tokenId)) {
                return t;
            }
        }
        return null;
    }

    public String getTimeRange() {
        return startTime + " - " + endTime;
    }

    /**
     * Get visual capacity bar
     */
    public String getCapacityBar() {
        int filled = (int) ((double) tokens.size() / capacity * 12);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        return bar.toString();
    }

    public String getStatus() {
        if (isFull()) return "FULL";
        if (isEmpty()) return "EMPTY";
        return tokens.size() + "/" + capacity;
    }

    @Override
    public String toString() {
        return String.format("%s [%d/%d] %s %s", 
            getTimeRange(), tokens.size(), capacity, getCapacityBar(), 
            isFull() ? "FULL" : "");
    }
}
