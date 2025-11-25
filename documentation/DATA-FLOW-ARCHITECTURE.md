# WLED Simulator Frontend - Data Flow Architecture

## 📊 Answer: Where Do Values Come From?

### **TL;DR**
- **On Page Load**: Backend (Thymeleaf server-side rendering)
- **When User Changes Value**: Frontend immediately updates, sends to backend
- **When Backend Changes (API/WebSocket)**: Frontend controls now auto-sync via WebSocket listener

---

## 🔄 Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                   INITIAL PAGE LOAD                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Browser GET http://localhost:8081                              │
│         ↓                                                         │
│  Spring Boot ViewController.getIndex()                           │
│         ↓                                                         │
│  WledService.getCurrentState() & getDeviceInfo()                │
│         ↓                                                         │
│  Thymeleaf renders index.html                                    │
│    • th:value="${state.brightness}" → "128"                     │
│    • th:value="${segment.brightness}" → "255"                   │
│    • th:selected="${segment.effect == 0}" → selected            │
│         ↓                                                         │
│  HTML sent to browser with SERVER-RENDERED VALUES               │
│         ↓                                                         │
│  JavaScript initializes: initializeControls()                    │
│         ↓                                                         │
│  WebSocket connection established                               │
│                                                                   │
│  RESULT: Frontend shows values FROM BACKEND                      │
│          (Thymeleaf template rendering)                          │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                  USER ADJUSTS CONTROL                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  User: Moves brightness slider to 180                            │
│         ↓                                                         │
│  Browser: 'input' event fired on slider                          │
│         ↓                                                         │
│  JavaScript: setMasterBrightness(180) called                     │
│    Step 1: Immediately update DOM                               │
│             document.getElementById('masterBrightnessValue')     │
│               .textContent = 180                                 │
│    Step 2: POST /json/state with {"bri": 180}                   │
│             (Asynchronous - doesn't block UI)                    │
│         ↓                                                         │
│  Backend: WledService.updateState()                              │
│    Step 1: Update currentState.brightness = 180                 │
│    Step 2: broadcastUpdate() via WebSocket                      │
│         ↓                                                         │
│  Frontend: WebSocket onmessage received                          │
│    Step 1: updateLEDColors(state) called                         │
│    Step 2: Animation loop renders new colors                    │
│         ↓                                                         │
│  User sees: Instant value change + visual feedback               │
│                                                                   │
│  RESULT: Frontend value IS THE SOURCE                            │
│          (Optimistic update for responsiveness)                  │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│            BACKEND STATE CHANGES (from another client)           │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Scenario: Another API client calls:                             │
│    POST /json/state {"bri": 50}                                  │
│         ↓                                                         │
│  Backend: WledService.updateState()                              │
│    Step 1: Update currentState.brightness = 50                  │
│    Step 2: broadcastUpdate() via WebSocket                      │
│         ↓                                                         │
│  All Connected Clients: WebSocket onmessage                      │
│         ↓                                                         │
│  updateLEDColors(state) Function:                                │
│    NOW INCLUDES:                                                 │
│    • Sync Master Brightness Slider:                              │
│      masterBrightnessInput.value = 50                            │
│      masterBrightnessValue.textContent = 50                      │
│                                                                   │
│    • Sync Segment Controls:                                      │
│      brightnessInput.value = segment.bri                         │
│      speedInput.value = segment.sx                               │
│      intensityInput.value = segment.ix                           │
│      effectSelect.value = segment.fx                             │
│      paletteSelect.value = segment.pal                           │
│      toggleBtn.textContent = isOn ? '✓ ON' : '✗ OFF'            │
│         ↓                                                         │
│  Result: Frontend controls now show CORRECT backend values       │
│          (Received via WebSocket sync)                           │
│                                                                   │
│  RESULT: Frontend IS SYNCED TO BACKEND                           │
│          (Single source of truth maintained)                     │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📍 Data Source Timeline

### **Phase 1: Page Load (Initial Values)**
```
Backend WledService       │  Thymeleaf Template     │  Browser
─────────────────────────┼─────────────────────────┼──────────
brightness = 128 ────────→ th:value="${state.bri}" → <input value="128">
segment[0].bri = 255  ───→ th:value="..." ─────────→ <input value="255">
segment[0].fx = 0 ────────→ th:selected="..." ─────→ <option selected>

Data Source: ✅ BACKEND (server-side rendering)
```

### **Phase 2: User Interaction**
```
Browser Input            │  JavaScript              │  Backend
─────────────────────────┼─────────────────────────┼──────────
User slides to 180 ──────→ setMasterBrightness() ──→ POST /json/state
                         → DOM update: 180
                           (Immediate)
                                                   ← updateLEDColors()
                                                   ← WebSocket sync

Data Source: ✅ FRONTEND (optimistic update for UX)
Truth Source: ✅ BACKEND (via POST request)
```

### **Phase 3: External State Change (NEW!)**
```
External API Call        │  Backend                │  WebSocket Clients
─────────────────────────┼─────────────────────────┼──────────────────
POST /json/state ────────→ Update state = 100
                        ├────────────────────────→ WebSocket broadcast
                                                  ├→ updateLEDColors()
                                                  ├→ NEW SYNC CODE:
                                                  │  • slider.value = 100
                                                  │  • display = "100"
                                                  │  • button.text = "ON"
                                                  ├→ LED animation
                                                  └→ UI fully synced

Data Source: ✅ BACKEND (source of truth)
Sync Method: ✅ WEBSOCKET (real-time)
Result: ✅ Frontend controls NOW MATCH backend
```

---

## 🏗️ Architecture Layers

### **1. Data Source Layer (Backend)**
**Location**: `WledService.java`
- Single source of truth
- Maintains state in memory
- Broadcasts changes via WebSocket

### **2. Transport Layer (WebSocket)**
**Location**: `WledWebSocketHandler.java`
- Real-time bidirectional communication
- Sends state updates to all connected clients
- Broadcasts every state change

### **3. Control Layer (Frontend JavaScript)**
**Location**: `index.html` script section
```javascript
// Optimistic update (immediate feedback)
function setMasterBrightness(value) {
    document.getElementById('masterBrightnessValue').textContent = value;  // ← FRONTEND
    fetch('/json/state', {body: JSON.stringify({bri: value})});           // ← BACKEND
}

// Sync from WebSocket (external changes)
function updateLEDColors(state) {
    // NEW SYNC CODE:
    masterBrightnessInput.value = state.bri;  // ← SYNCS SLIDER
    masterBrightnessValue.textContent = state.bri;  // ← SYNCS DISPLAY
    // ... all other controls ...
}
```

### **4. Presentation Layer (HTML)**
**Location**: `index.html` template
- Server-side rendered initial values (from Thymeleaf)
- Client-side updated values (from JavaScript)
- Always reflects current backend state

---

## 🔄 Scenarios & Data Sources

### **Scenario 1: First Time User Loads Page**
| Step | Component | Data Source | Value |
|------|-----------|-------------|-------|
| 1 | Backend | WledService | bri = 128 |
| 2 | Template | Thymeleaf | `value="${state.bri}"` |
| 3 | Browser | HTML | `<input value="128">` |
| 4 | Display | Frontend | "128/255" |

✅ **Data Source**: BACKEND → TEMPLATE → FRONTEND

---

### **Scenario 2: User Adjusts Slider**
| Step | Component | Data Source | Value |
|------|-----------|-------------|-------|
| 1 | User Action | Browser | Slider moved to 180 |
| 2 | Frontend Update | JavaScript | DOM updated immediately |
| 3 | API Call | JavaScript | POST `{"bri": 180}` |
| 4 | Backend Update | WledService | state.bri = 180 |
| 5 | WebSocket Broadcast | WledService | Send to all clients |
| 6 | Frontend Sync | JavaScript | New sync code updates slider |

✅ **Data Source**: FRONTEND (optimistic) → BACKEND (truth) → FRONTEND (sync)

---

### **Scenario 3: Another Client Changes State**
| Step | Component | Data Source | Value |
|------|-----------|-------------|-------|
| 1 | External API | Another client | POST `{"bri": 50}` |
| 2 | Backend Update | WledService | state.bri = 50 |
| 3 | WebSocket Broadcast | WledService | Send new state to all |
| 4 | **Frontend Sync** (NEW) | updateLEDColors | Slider updated to 50 |
| 5 | **Frontend Display** (NEW) | updateLEDColors | Display shows 50 |
| 6 | LED Animation | effectSimulator | Renders new brightness |

✅ **Data Source**: BACKEND (truth) → WEBSOCKET → FRONTEND (synced)

---

## 🎯 Key Improvements Made

### **Before (Problematic)**
- Frontend values were **static** after page load
- External API changes wouldn't update form controls
- User could get **out of sync** with backend state
- LED visualization updated but sliders/buttons didn't

### **After (Fixed)**
- WebSocket listener now **syncs all form controls**
- Master brightness slider synced
- Segment brightness sliders synced
- Segment power buttons synced
- Segment speed/intensity synced
- Segment effect/palette dropdowns synced
- **Single source of truth**: Backend state always correct

---

## 🔧 Implementation Details

### **The Sync Code**
```javascript
function updateLEDColors(state) {
    // MASTER CONTROLS SYNC
    masterBrightnessInput.value = state.bri;
    masterBrightnessValue.textContent = state.bri;

    // SEGMENT CONTROLS SYNC
    state.seg.forEach(segment => {
        const brightnessInput = document.querySelector(
            `.segment-brightness[data-segment="${segment.id}"]`
        );
        brightnessInput.value = segment.bri;

        const speedInput = document.querySelector(
            `.segment-speed[data-segment="${segment.id}"]`
        );
        speedInput.value = segment.sx;

        // ... sync all other controls ...
    });
}
```

### **When It Runs**
- Every time WebSocket receives a state update
- Called from: `ws.onmessage`
- Guarantees frontend always reflects backend state

---

## 📝 Summary Table

| Scenario | Data From | Updated To | Method | Sync |
|----------|-----------|-----------|--------|------|
| Page Load | Backend (DB) | Frontend HTML | Thymeleaf | N/A |
| User Changes | Frontend Input | Frontend DOM | JavaScript | Async HTTP POST |
| User Changes | Frontend Input | Backend | HTTP POST | Yes |
| Backend Changes | Backend State | Frontend | WebSocket | ✅ NEW |
| Backend Changes | Backend State | LED Display | WebSocket | Yes |

---

## 🎓 Best Practices Implemented

1. **Optimistic Updates**: Frontend updates immediately for responsiveness
2. **Async Operations**: HTTP calls don't block UI
3. **Single Source of Truth**: Backend always authoritative
4. **Real-time Sync**: WebSocket keeps all clients in sync
5. **Graceful Degradation**: Works even if network is slow
6. **State Consistency**: Form controls reflect actual backend state

---

## ✅ Verification

All data flow scenarios verified with automated tests:
- ✓ Initial values loaded from backend
- ✓ User changes sent to backend
- ✓ Backend changes broadcast via WebSocket
- ✓ Frontend controls synced to backend state
- ✓ LED visualization always shows correct state

---

**Last Updated**: November 22, 2025
**Version**: 1.1 - Data Synchronization Fixed
**Status**: ✅ All features verified and working correctly
