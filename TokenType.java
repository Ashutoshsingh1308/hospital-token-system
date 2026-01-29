/**
 * Priority levels for tokens (lower number = higher priority)
 */
public enum TokenType {
    EMERGENCY(0, "⚡"),
    PAID(1, "💎"),
    FOLLOWUP(2, "🔄"),
    WALKIN(3, "🚶"),
    ONLINE(4, "💻");

    private final int priority;
    private final String icon;

    TokenType(int priority, String icon) {
        this.priority = priority;
        this.icon = icon;
    }

    public int getPriority() {
        return priority;
    }

    public String getIcon() {
        return icon;
    }
}
