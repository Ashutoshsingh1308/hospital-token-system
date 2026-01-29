# OPD Token Allocation Engine

## What is this?

This is a token management system for hospital OPD (Out Patient Department). The basic idea is simple - when patients come to a hospital, they get tokens based on their priority. Emergency patients get seen first, then paid/VIP patients, then regular walk-ins, and so on.

The tricky part is handling real-world situations like:
- What if a slot is already full and an emergency patient comes?
- What if a doctor gets delayed?
- What if someone cancels or doesn't show up?

This system handles all of that.

---

## How it Works

### The Priority System

I kept this simple - just 5 types of patients with different priorities:

```
EMERGENCY (0) - highest priority, seen first
PAID (1)      - VIP/premium patients  
FOLLOWUP (2)  - patients coming back for checkup
WALKIN (3)    - normal OPD desk registration
ONLINE (4)    - lowest priority, booked online
```

Lower number = higher priority. So if slot is full and emergency comes, it kicks out the online booking patient.

### The Bumping Logic

This was the hardest part to figure out. Here's what happens:

```
When booking a token:
1. Check if slot has space → if yes, just add it
2. If slot is full:
   - Compare new patient priority with lowest priority in slot
   - If new patient is more important → kick out the lowest one
   - The kicked out patient goes to next slot
   - If next slot is also full → repeat the process
   - If no slots left → add to waiting list
```

I used recursion here because it made the code cleaner. The function calls itself for bumped patients.

---

## API Endpoints

Built a simple REST API using Java's built-in HTTP server (no Spring or anything fancy).

| Method | URL | What it does |
|--------|-----|--------------|
| POST | /doctors | Add new doctor |
| POST | /doctors/{name}/slots | Add time slot to doctor |
| POST | /tokens | Book a token |
| DELETE | /tokens/{id}?doctor=X | Cancel booking |
| PUT | /tokens/{id}/noshow?doctor=X | Mark patient as no-show |
| PUT | /doctors/{name}/delay/{slot} | Handle doctor delay |
| GET | /doctors | See all doctors |
| GET | /doctors/{name} | See specific doctor |

### Request/Response Format

**Booking a token:**
```json
POST /tokens
{
  "doctor": "Sharma",
  "slot": 0,
  "patient": "Priya",
  "type": "ONLINE"
}

Response:
{
  "success": true,
  "tokenId": "T001",
  "patient": "Priya",
  "type": "ONLINE"
}
```

---

## Edge Cases Handled

| Problem | How I solved it |
|---------|-----------------|
| All slots full | Goes to waiting list |
| Chain bumping (A bumps B bumps C...) | Recursion handles it automatically |
| Doctor doesn't exist | Returns "Doctor not found" error |
| Token doesn't exist | Returns "Token not found" error |
| No-show patient | Same as cancel, fills spot from waiting list |
| Doctor delayed | All tokens shift to next slots |

---

## How to Run

### Compile everything
```bash
cd hospital-token-system/src
javac *.java
```

### Run the simulation (3 doctors, full day)
```bash
java Main
```

### Run the API server
```bash
java ApiServer
# runs on http://localhost:8080
```

### Test with curl
```bash
# Add a doctor
curl -X POST http://localhost:8080/doctors -d '{"name":"Sharma"}'

# Add slot
curl -X POST http://localhost:8080/doctors/Sharma/slots \
  -d '{"start":"9:00 AM","end":"10:00 AM","capacity":5}'

# Book token
curl -X POST http://localhost:8080/tokens \
  -d '{"doctor":"Sharma","slot":0,"patient":"Priya","type":"ONLINE"}'

# Cancel
curl -X DELETE "http://localhost:8080/tokens/T001?doctor=Sharma"
```

---

## Files

```
src/
├── TokenType.java     - enum for 5 priority types
├── Token.java         - patient token (id, name, type, timestamps)
├── Slot.java          - time slot (9-10 AM etc) with capacity
├── Doctor.java        - doctor with multiple slots + waiting list
├── TokenManager.java  - main logic (booking, cancel, bump, delay)
├── ApiServer.java     - REST API endpoints
└── Main.java          - simulation with 3 doctors
```

---

## What I learned

1. **Recursion is useful** - The bumping logic would be messy with loops, recursion made it clean

2. **Priority queues** - Sorting tokens by priority after every insert keeps things organized

3. **Edge cases matter** - Half the code is handling what-ifs (no slots, no doctor, etc.)

4. **Keep it simple** - I didn't use any framework, just plain Java. Makes it easier to understand and run anywhere.

---

## Limitations / Future Work

- Data is lost on restart (would need database)
- No authentication on API
- Single threaded (would need synchronization for real production)
- No UI (could add a simple web interface)

---

## Sample Output

```
▶ TEST 2: EMERGENCY insertion (triggers bumping)

══════════════════════════════════════════════════
OPERATION: Booking EMERGENCY token
══════════════════════════════════════════════════
✓ Token T006 created
  Patient: Critical1
  Priority: 0 (EMERGENCY)

⚠ Slot 9:00 AM - 10:00 AM is FULL (5/5)
→ Bumping T005 (Kavita-ONLINE) to next slot
✓ T006 allocated to 9:00 AM - 10:00 AM
✓ T005 allocated to 10:00 AM - 11:00 AM

DR. SHARMA - 9:00 AM - 10:00 AM [5/5] ████████████ FULL
  1. T006 - Critical1    [EMERGENCY] ⚡
  2. T004 - Amit         [PAID     ] 💎
  3. T003 - Neha         [FOLLOWUP ] 🔄
  4. T002 - Raj          [WALKIN   ] 🚶
  5. T001 - Priya        [ONLINE   ] 💻
```

Notice how emergency patient (Critical1) is at top, and the online booking (Kavita) got pushed to next slot.
# hospital-token-system
