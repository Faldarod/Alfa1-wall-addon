# Simulating Employee Devices for Testing

Since you don't have physical UniFi devices, this guide shows how to simulate employee presence using Home Assistant's device_tracker entities.

## Quick Start

### Method 1: Using Developer Tools (Immediate)

1. **Access Home Assistant Developer Tools:**
   - Go to http://localhost:8123
   - Click **Developer Tools** (hammer icon in sidebar)
   - Go to **Services** tab

2. **Set a device as "home" (present):**
   ```yaml
   Service: device_tracker.see
   Service Data:
   dev_id: john_phone
   location_name: home
   ```
   Click **CALL SERVICE**

3. **Set a device as "away" (not present):**
   ```yaml
   Service: device_tracker.see
   Service Data:
   dev_id: john_phone
   location_name: not_home
   ```

4. **Test with AlfaWall:**
   ```bash
   curl -X POST http://localhost:8080/api/conversation/process \
     -H "Content-Type: application/json" \
     -d '{"text":"Who is here now?"}'
   ```

### Method 2: Using Input Booleans (UI Toggle Switches)

1. **Go to Settings → Devices & Services → Helpers**
2. **You'll see toggle switches for each employee's devices:**
   - John's Phone Present
   - John's Laptop Present
   - Jane's Phone Present
   - etc.

3. **Toggle ON to simulate presence, OFF for away**

4. **These automatically update the device_tracker entities**

## Available Mock Devices

The following `device_tracker` entities are pre-configured:

| Entity ID | Employee | Device Type |
|-----------|----------|-------------|
| `device_tracker.john_phone` | John Doe | Phone |
| `device_tracker.john_laptop` | John Doe | Laptop |
| `device_tracker.jane_phone` | Jane Doe | Phone |
| `device_tracker.alice_phone` | Alice | Phone |
| `device_tracker.alice_laptop` | Alice | Laptop |
| `device_tracker.bob_phone` | Bob | Phone |
| `device_tracker.charlie_phone` | Charlie | Phone |

## Testing Scenarios

### Scenario 1: Single Employee Present

**Set John's phone as home:**
```yaml
Service: device_tracker.see
Service Data:
  dev_id: john_phone
  location_name: home
```

**Query:**
```bash
curl -X POST http://localhost:8080/api/conversation/process \
  -H "Content-Type: application/json" \
  -d '{"text":"Who is here now?"}'
```

**Expected:** LEDs light up for John Doe (Segment 0, Green)

### Scenario 2: Multiple Employees Present

**Set multiple devices as home:**
```yaml
# John's phone
Service: device_tracker.see
Service Data:
  dev_id: john_phone
  location_name: home
```

```yaml
# Jane's phone
Service: device_tracker.see
Service Data:
  dev_id: jane_phone
  location_name: home
```

**Query:** "Who is here now?"

**Expected:** LEDs light up for both John and Jane

### Scenario 3: Skills-Based Query

**Set John as present (has Java skill):**
```yaml
Service: device_tracker.see
Service Data:
  dev_id: john_phone
  location_name: home
```

**Query:** "Who knows Java?"

**Expected:** LEDs light up purple for John (skills-based query color)

## Checking Device States

### Via UI

1. Go to **Developer Tools** → **States**
2. Filter for `device_tracker`
3. You'll see all entities and their current states:
   - `home` = Present
   - `not_home` = Away

### Via REST API

```bash
# Get all device_tracker states
curl http://localhost:8123/api/states | grep device_tracker

# Get specific device state
curl http://localhost:8123/api/states/device_tracker.john_phone \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Automating Presence Simulation

You can create automations to simulate realistic presence patterns:

**Example: Simulate John arriving at 9 AM:**

```yaml
automation:
  - alias: "John Arrives at Office"
    trigger:
      platform: time
      at: "09:00:00"
    action:
      service: device_tracker.see
      data:
        dev_id: john_phone
        location_name: home

  - alias: "John Leaves Office"
    trigger:
      platform: time
      at: "17:00:00"
    action:
      service: device_tracker.see
      data:
        dev_id: john_phone
        location_name: not_home
```

## Creating a Testing Dashboard

Create a dedicated dashboard for testing:

1. Go to **Settings** → **Dashboards**
2. Click **+ ADD DASHBOARD**
3. Name it "AlfaWall Testing"
4. Add cards:
   - **Entities Card** with all input_boolean helpers
   - **Entity Card** for each device_tracker to see states
   - **Button Card** to trigger test queries

## Troubleshooting

### Device tracker not showing up

**Check configuration:**
```bash
# Restart Home Assistant to load known_devices.yaml
docker restart alfa1-wall-addon-home-assistant-1
```

**Verify entity exists:**
- Go to Developer Tools → States
- Search for `device_tracker.john_phone`
- If not found, check `known_devices.yaml` syntax

### AlfaWall not detecting presence

**Check entity state:**
```bash
curl http://localhost:8123/api/states/device_tracker.john_phone \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Verify application.yaml mapping:**
```yaml
employee-data:
  employees:
    - id: john-doe
      name: John Doe
      device-trackers:
        - device_tracker.john_phone  # Must match exactly
        - device_tracker.john_laptop
```

### Input booleans not working

**Manual fallback:**
Use `device_tracker.see` service directly in Developer Tools as shown in Method 1 above.

## Advanced: Bulk Device Updates

**Set multiple devices at once using a script:**

```yaml
script:
  simulate_morning_arrival:
    sequence:
      - service: device_tracker.see
        data:
          dev_id: john_phone
          location_name: home
      - service: device_tracker.see
        data:
          dev_id: jane_phone
          location_name: home
      - service: device_tracker.see
        data:
          dev_id: alice_phone
          location_name: home
```

Call it via Developer Tools → Services:
```yaml
Service: script.simulate_morning_arrival
```

## Next Steps

1. Start Home Assistant with the new configuration
2. Use Developer Tools to set device states
3. Test queries with AlfaWall
4. Watch LEDs light up in WLED Simulator (http://localhost:8081)
5. Create automations for realistic testing scenarios

## Related Documentation

- `UNIFI_INTEGRATION.md` - For when you have real UniFi devices
- `CLAUDE.md` - Main project documentation
- Home Assistant device_tracker docs: https://www.home-assistant.io/integrations/device_tracker/
