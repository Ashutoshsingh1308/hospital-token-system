import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Represents a doctor with their time slots and waiting list
 */
public class Doctor {
    private final String name;
    private final List<Slot> slots;
    private final Queue<Token> waitingList;

    public Doctor(String name) {
        this.name = name;
        this.slots = new ArrayList<>();
        this.waitingList = new LinkedList<>();
    }

    public String getName() {
        return name;
    }

    public List<Slot> getSlots() {
        return slots;
    }

    public Queue<Token> getWaitingList() {
        return waitingList;
    }

    /**
     * Add a time slot for this doctor
     */
    public void addSlot(String startTime, String endTime, int capacity) {
        slots.add(new Slot(startTime, endTime, capacity));
    }

    /**
     * Get slot by index
     */
    public Slot getSlot(int index) {
        if (index >= 0 && index < slots.size()) {
            return slots.get(index);
        }
        return null;
    }

    /**
     * Find slot containing a specific token
     */
    public Slot findSlotWithToken(String tokenId) {
        for (Slot slot : slots) {
            if (slot.findTokenById(tokenId) != null) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Get index of a slot
     */
    public int getSlotIndex(Slot slot) {
        return slots.indexOf(slot);
    }

    /**
     * Get next slot after given index
     */
    public Slot getNextSlot(int currentIndex) {
        if (currentIndex + 1 < slots.size()) {
            return slots.get(currentIndex + 1);
        }
        return null;
    }

    /**
     * Add token to waiting list
     */
    public void addToWaitingList(Token token) {
        waitingList.add(token);
    }

    /**
     * Remove and return first from waiting list
     */
    public Token pollWaitingList() {
        return waitingList.poll();
    }

    /**
     * Check if token is in waiting list
     */
    public boolean isInWaitingList(String tokenId) {
        for (Token t : waitingList) {
            if (t.getId().equals(tokenId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove token from waiting list
     */
    public boolean removeFromWaitingList(String tokenId) {
        // have to use iterator to avoid ConcurrentModificationException
        java.util.Iterator<Token> it = waitingList.iterator();
        while (it.hasNext()) {
            if (it.next().getId().equals(tokenId)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public void displayStatus() {
        System.out.println("\n" + "═".repeat(50));
        System.out.println("DR. " + name.toUpperCase());
        System.out.println("═".repeat(50));
        
        for (Slot slot : slots) {
            System.out.println("\n" + slot);
            List<Token> tokens = slot.getTokens();
            for (int i = 0; i < tokens.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + tokens.get(i));
            }
            if (tokens.isEmpty()) {
                System.out.println("  (No tokens)");
            }
        }
        
        if (!waitingList.isEmpty()) {
            System.out.println("\n⏳ WAITING LIST: " + waitingList.size() + " patients");
            int i = 1;
            for (Token t : waitingList) {
                System.out.println("  " + i++ + ". " + t);
            }
        }
    }
}
