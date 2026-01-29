/**
 * Hospital OPD Token Management System - Simulation
 * 
 * Simulates one OPD day with 3 doctors demonstrating:
 * - All token types (EMERGENCY, PAID, FOLLOWUP, WALKIN, ONLINE)
 * - Priority-based allocation and bumping
 * - Cancellations and no-shows
 * - Slot delays
 * - Waiting list management
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║      OPD TOKEN ALLOCATION ENGINE - DAY SIMULATION        ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        Token.resetCounter();
        TokenManager manager = new TokenManager();

        // ═══════════════════════════════════════════════════════════
        // SETUP: Create 3 doctors with slots
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n▶ SETUP: Creating 3 doctors with time slots...\n");
        
        Doctor drSharma = manager.addDoctor("Sharma");
        drSharma.addSlot("9:00 AM", "10:00 AM", 5);
        drSharma.addSlot("10:00 AM", "11:00 AM", 5);
        drSharma.addSlot("11:00 AM", "12:00 PM", 5);
        System.out.println("✓ Dr. Sharma (General Medicine) - 3 slots, capacity 5 each");

        Doctor drPatel = manager.addDoctor("Patel");
        drPatel.addSlot("9:00 AM", "10:00 AM", 4);
        drPatel.addSlot("10:00 AM", "11:00 AM", 4);
        drPatel.addSlot("11:00 AM", "12:00 PM", 4);
        System.out.println("✓ Dr. Patel (Cardiology) - 3 slots, capacity 4 each");

        Doctor drGupta = manager.addDoctor("Gupta");
        drGupta.addSlot("9:00 AM", "10:00 AM", 5);
        drGupta.addSlot("10:00 AM", "11:00 AM", 5);
        System.out.println("✓ Dr. Gupta (Orthopedics) - 2 slots, capacity 5 each");

        // ═══════════════════════════════════════════════════════════
        // TEST 1: Book tokens from all sources for Dr. Sharma
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n\n▶ TEST 1: Booking tokens from ALL sources (Dr. Sharma, 9-10 AM)");
        
        manager.bookToken("Sharma", 0, "Priya", TokenType.ONLINE);
        manager.bookToken("Sharma", 0, "Raj", TokenType.WALKIN);
        manager.bookToken("Sharma", 0, "Neha", TokenType.FOLLOWUP);  // Follow-up patient
        manager.bookToken("Sharma", 0, "Amit", TokenType.PAID);
        manager.bookToken("Sharma", 0, "Kavita", TokenType.ONLINE);

        // ═══════════════════════════════════════════════════════════
        // TEST 2: Emergency insertion with bumping
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n\n▶ TEST 2: EMERGENCY insertion (triggers bumping)");
        
        manager.bookToken("Sharma", 0, "Critical1", TokenType.EMERGENCY);

        // ═══════════════════════════════════════════════════════════
        // TEST 3: Fill Dr. Patel's slots and create waiting list
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n\n▶ TEST 3: Filling Dr. Patel's slots + waiting list");
        
        manager.bookToken("Patel", 0, "Ramesh", TokenType.PAID);
        manager.bookToken("Patel", 0, "Suresh", TokenType.FOLLOWUP);
        manager.bookToken("Patel", 0, "Dinesh", TokenType.WALKIN);
        manager.bookToken("Patel", 0, "Mahesh", TokenType.ONLINE);
        // Slot 0 now full (4/4), next ones will overflow
        manager.bookToken("Patel", 0, "Ganesh", TokenType.ONLINE);
        manager.bookToken("Patel", 0, "Lokesh", TokenType.ONLINE);

        // ═══════════════════════════════════════════════════════════
        // TEST 4: Cancellation
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n\n▶ TEST 4: Patient cancellation");
        
        manager.cancelToken("Sharma", "T003");

        // ═══════════════════════════════════════════════════════════
        // TEST 5: No-show handling
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n\n▶ TEST 5: Marking NO-SHOW");
        
        manager.markNoShow("Patel", "T008");

        // ═══════════════════════════════════════════════════════════
        // TEST 6: Dr. Gupta's slot delay
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n\n▶ TEST 6: Dr. Gupta setup and slot delay");
        
        manager.bookToken("Gupta", 0, "Anita", TokenType.ONLINE);
        manager.bookToken("Gupta", 0, "Vijay", TokenType.PAID);
        manager.bookToken("Gupta", 0, "Sunita", TokenType.FOLLOWUP);
        
        // Doctor is delayed - shift all tokens
        manager.delaySlot("Gupta", 0);

        // ═══════════════════════════════════════════════════════════
        // TEST 7: Edge cases
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n\n▶ TEST 7: Edge cases");
        
        System.out.println("\n--- Cancel non-existent token ---");
        manager.cancelToken("Sharma", "T999");
        
        System.out.println("\n--- Book for non-existent doctor ---");
        manager.bookToken("NonExistent", 0, "Test", TokenType.ONLINE);
        
        System.out.println("\n--- Multiple emergencies ---");
        manager.bookToken("Gupta", 0, "Emergency2", TokenType.EMERGENCY);
        manager.bookToken("Gupta", 0, "Emergency3", TokenType.EMERGENCY);

        // ═══════════════════════════════════════════════════════════
        // FINAL STATE
        // ═══════════════════════════════════════════════════════════
        System.out.println("\n\n▶ FINAL STATE OF ALL DOCTORS");
        manager.displayAll();
        
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              DAY SIMULATION COMPLETED                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
}
