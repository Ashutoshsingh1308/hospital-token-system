import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages token booking, cancellation, and slot operations with priority bumping
 */
public class TokenManager {
    private final Map<String, Doctor> doctors;

    public TokenManager() {
        this.doctors = new HashMap<>();
    }

    /**
     * Register a new doctor
     */
    public Doctor addDoctor(String name) {
        Doctor doctor = new Doctor(name);
        doctors.put(name, doctor);
        return doctor;
    }

    public Doctor getDoctor(String name) {
        return doctors.get(name);
    }

    public Collection<Doctor> getAllDoctors() {
        return doctors.values();
    }

    /**
     * Book a token for a patient with the specified priority
     * Implements bumping algorithm when slot is full
     */
    public Token bookToken(String doctorName, int slotIndex, String patientName, TokenType type) {
        printOperationHeader("Booking " + type + " token");
        
        Doctor doctor = doctors.get(doctorName);
        if (doctor == null) {
            System.out.println("✗ Doctor not found: " + doctorName);
            return null;
        }

        Slot targetSlot = doctor.getSlot(slotIndex);
        if (targetSlot == null) {
            System.out.println("✗ Invalid slot index: " + slotIndex);
            return null;
        }

        // Create the new token
        Token newToken = new Token(patientName, type);
        System.out.println("✓ Token " + newToken.getId() + " created");
        System.out.println("  Patient: " + patientName);
        System.out.println("  Priority: " + type.getPriority() + " (" + type + ")");

        // Try to allocate token to the requested slot
        allocateToken(doctor, slotIndex, newToken);
        
        // Display current slot status
        displaySlotStatus(doctor, targetSlot);
        
        return newToken;
    }

    /**
     * Core allocation logic with bumping
     * When slot is full:
     *   - If new token has higher priority than lowest in slot: bump lowest
     *   - Otherwise: try next slot or add to waiting list
     */
    private void allocateToken(Doctor doctor, int slotIndex, Token token) {
        Slot slot = doctor.getSlot(slotIndex);
        
        if (slot == null) {
            // No more slots available, add to waiting list
            System.out.println("→ No slots available, adding to waiting list");
            doctor.addToWaitingList(token);
            return;
        }

        if (!slot.isFull()) {
            // Slot has space, add directly
            slot.addToken(token);
            token.setAllocatedAt(LocalDateTime.now());
            System.out.println("✓ " + token.getId() + " allocated to " + slot.getTimeRange());
            return;
        }

        // Slot is full - apply bumping logic
        System.out.println("\n⚠ Slot " + slot.getTimeRange() + " is FULL (" + 
            slot.getCurrentCount() + "/" + slot.getCapacity() + ")");

        Token lowestInSlot = slot.getLowestPriorityToken();
        
        // Check if new token has higher priority (lower number) than lowest in slot
        if (token.getPriority() < lowestInSlot.getPriority()) {
            // Bump the lowest priority token
            System.out.println("→ Bumping " + lowestInSlot.getId() + " (" + 
                lowestInSlot.getPatientName() + "-" + lowestInSlot.getType() + 
                ") to next slot");
            
            slot.removeToken(lowestInSlot);
            slot.addToken(token);
            token.setAllocatedAt(LocalDateTime.now());
            System.out.println("✓ " + token.getId() + " allocated to " + slot.getTimeRange());
            
            // Recursively try to place bumped token in next slot
            allocateToken(doctor, slotIndex + 1, lowestInSlot);
        } else {
            // New token doesn't have higher priority, move it to next slot
            System.out.println("→ Moving " + token.getId() + " to next slot (lower priority)");
            allocateToken(doctor, slotIndex + 1, token);
        }
    }

    /**
     * Cancel a token and potentially fill from waiting list
     */
    public boolean cancelToken(String doctorName, String tokenId) {
        printOperationHeader("Cancelling token " + tokenId);
        
        Doctor doctor = doctors.get(doctorName);
        if (doctor == null) {
            System.out.println("✗ Doctor not found: " + doctorName);
            return false;
        }

        // Check if token is in waiting list
        if (doctor.removeFromWaitingList(tokenId)) {
            System.out.println("✓ Token " + tokenId + " removed from waiting list");
            return true;
        }

        // Find and remove from slot
        Slot slot = doctor.findSlotWithToken(tokenId);
        if (slot == null) {
            System.out.println("✗ Token " + tokenId + " not found");
            return false;
        }

        Token removed = slot.removeTokenById(tokenId);
        System.out.println("✓ Token " + tokenId + " (" + removed.getPatientName() + 
            ") cancelled from " + slot.getTimeRange());

        // Try to fill vacancy from waiting list
        fillFromWaitingList(doctor, slot);
        
        displaySlotStatus(doctor, slot);
        return true;
    }

    /**
     * Fill slot vacancy from waiting list
     */
    private void fillFromWaitingList(Doctor doctor, Slot slot) {
        if (!slot.isFull() && !doctor.getWaitingList().isEmpty()) {
            Token waitingToken = doctor.pollWaitingList();
            slot.addToken(waitingToken);
            waitingToken.setAllocatedAt(LocalDateTime.now());
            System.out.println("→ " + waitingToken.getId() + " (" + waitingToken.getPatientName() + 
                ") moved from waiting list to " + slot.getTimeRange());
        }
    }

    /**
     * Delay a slot - shifts all tokens to subsequent slots
     */
    public void delaySlot(String doctorName, int slotIndex) {
        printOperationHeader("Delaying slot");
        
        Doctor doctor = doctors.get(doctorName);
        if (doctor == null) {
            System.out.println("✗ Doctor not found: " + doctorName);
            return;
        }

        Slot slot = doctor.getSlot(slotIndex);
        if (slot == null) {
            System.out.println("✗ Invalid slot index: " + slotIndex);
            return;
        }

        System.out.println("⚠ Delaying " + slot.getTimeRange() + " for Dr. " + doctorName);
        System.out.println("→ Shifting all tokens to subsequent slots...\n");

        // Get all tokens from this slot
        java.util.List<Token> tokensToMove = slot.getTokens();
        
        // Clear the slot
        for (Token t : tokensToMove) {
            slot.removeToken(t);
        }

        // Move each token to next available slot (cascade)
        for (Token token : tokensToMove) {
            System.out.println("  Moving " + token.getId() + " (" + token.getPatientName() + ")...");
            allocateToken(doctor, slotIndex + 1, token);
        }

        System.out.println("\n✓ Slot delay completed");
        doctor.displayStatus();
    }

    /**
     * Mark a token as no-show and fill from waiting list
     */
    public boolean markNoShow(String doctorName, String tokenId) {
        printOperationHeader("Marking NO-SHOW: " + tokenId);
        
        Doctor doctor = doctors.get(doctorName);
        if (doctor == null) {
            System.out.println("✗ Doctor not found: " + doctorName);
            return false;
        }

        Slot slot = doctor.findSlotWithToken(tokenId);
        if (slot == null) {
            System.out.println("✗ Token " + tokenId + " not found");
            return false;
        }

        Token removed = slot.removeTokenById(tokenId);
        System.out.println("✗ Token " + tokenId + " (" + removed.getPatientName() + 
            ") marked as NO-SHOW from " + slot.getTimeRange());

        // Fill vacancy from waiting list
        fillFromWaitingList(doctor, slot);
        
        displaySlotStatus(doctor, slot);
        return true;
    }

    /**
     * Display all doctors and their status
     */
    public void displayAll() {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("           HOSPITAL TOKEN MANAGEMENT SYSTEM");
        System.out.println("═".repeat(60));
        
        for (Doctor doctor : doctors.values()) {
            doctor.displayStatus();
        }
        
        System.out.println("\n" + "═".repeat(60));
    }

    private void printOperationHeader(String operation) {
        System.out.println("\n" + "═".repeat(50));
        System.out.println("OPERATION: " + operation);
        System.out.println("═".repeat(50));
    }

    private void displaySlotStatus(Doctor doctor, Slot slot) {
        System.out.println("\nDR. " + doctor.getName().toUpperCase() + " - " + slot);
        java.util.List<Token> tokens = slot.getTokens();
        for (int i = 0; i < tokens.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + tokens.get(i));
        }
        
        if (!doctor.getWaitingList().isEmpty()) {
            System.out.println("\nWAITING LIST: " + doctor.getWaitingList().size() + " patients");
        }
    }
}
