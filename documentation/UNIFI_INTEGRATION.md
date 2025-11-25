# UniFi Controller Integration with Home Assistant

This guide explains how to integrate the UniFi Network Controller with Home Assistant for device tracking and presence detection in the AlfaWall project.

## Overview

The UniFi integration allows Home Assistant to:
- Track devices connected to your UniFi network
- Detect employee presence based on MAC addresses
- Create `device_tracker` entities for each tracked device
- Trigger automations based on device presence

## Prerequisites

1. **UniFi Controller must be running:**
   ```bash
   cd Alfa1-wall-addon
   docker-compose --profile local-testing up -d unifi-controller unifi-db
   ```

2. **Access UniFi Controller Web Interface:**
   - URL: https://localhost:8443
   - Accept the self-signed certificate warning
   - Complete initial setup wizard (if first time)

## Step 1: Set Up UniFi Controller

### Initial Setup (First Time Only)

1. Navigate to https://localhost:8443
2. Accept certificate warning
3. Click "Start Setup Wizard"
4. Create admin account:
   - **Username:** admin (or your choice)
   - **Password:** (choose a secure password)
   - **Email:** your-email@example.com
5. Name your controller: "AlfaWall UniFi"
6. Skip device adoption (unless you have physical UniFi devices)
7. Complete setup wizard

### Create Local User for Home Assistant

1. Log into UniFi Controller (https://localhost:8443)
2. Go to **Settings** (gear icon)
3. Navigate to **System** → **Admins**
4. Click **Add Admin**
5. Create new admin:
   - **Name:** homeassistant
   - **Email:** homeassistant@localhost
   - **Role:** Limited Admin or View Only
   - **Password:** (choose a secure password - you'll need this for HA)
6. Click **Add**

**Important:** Save these credentials - you'll need them for Home Assistant!

## Step 2: Configure Home Assistant Integration

### Option A: Via Home Assistant UI (Recommended)

1. Start Home Assistant:
   ```bash
   cd Alfa1-wall-addon
   docker-compose --profile local-testing up -d
   ```

2. Access Home Assistant: http://localhost:8123

3. Complete Home Assistant onboarding (if first time):
   - Create your account
   - Set location and time zone
   - Complete setup

4. Add UniFi Integration:
   - Go to **Settings** → **Devices & Services**
   - Click **+ ADD INTEGRATION**
   - Search for "UniFi Network"
   - Click on "UniFi Network"

5. Configure integration:
   - **Host:** `unifi-controller` (Docker hostname)
   - **Port:** `8443`
   - **Username:** `homeassistant` (or the admin username you created)
   - **Password:** (the password you set)
   - **Verify SSL certificate:** Uncheck (we're using self-signed cert)
   - Click **Submit**

6. Choose options:
   - **Track network clients:** Check this
   - **Track network devices:** Optional (for UniFi APs, switches)
   - **Detection time:** 300 seconds (adjust as needed)
   - Click **Submit**

7. Select area: Choose or create "Office" area
   - Click **Finish**

### Option B: Via Configuration YAML (Manual)

Add to `home-assistant/config/configuration.yaml`:

```yaml
# UniFi Network Integration
unifi:
  controllers:
    - host: unifi-controller
      port: 8443
      username: !secret unifi_username
      password: !secret unifi_password
      verify_ssl: false
      site: default
      detection_time: 300
      track_clients: true
      track_devices: false
```

Add to `home-assistant/config/secrets.yaml`:

```yaml
unifi_username: homeassistant
unifi_password: your-password-here
```

Then restart Home Assistant.

## Step 3: Configure Device Tracking for Employees

### Add Employee Devices

Once the integration is set up, you need to map employee devices to their MAC addresses.

#### Get Device MAC Addresses

**Method 1: From Home Assistant**
1. Go to **Settings** → **Devices & Services**
2. Click on "UniFi Network" integration
3. Click on "Devices" tab
4. You'll see all detected devices with their MAC addresses

**Method 2: From UniFi Controller**
1. Log into https://localhost:8443
2. Go to **Clients** tab
3. Find connected devices and note their MAC addresses

#### Map to Employees in Application Config

Edit `alfa-wall-addon/src/main/resources/application.yaml`:

```yaml
employee-device:
  mappings:
    "John Doe": "device_tracker.johns_iphone"          # Maps to UniFi device
    "Sarah Chen": "device_tracker.sarahs_laptop"
    "Mike Johnson": "device_tracker.mikes_phone"
    "Emma Wilson": "device_tracker.emmas_macbook"
    "Alex Rodriguez": "device_tracker.alexs_phone"
```

The `device_tracker` entity IDs come from Home Assistant after devices are detected.

### Finding Entity IDs

1. In Home Assistant, go to **Developer Tools** → **States**
2. Filter for `device_tracker`
3. You'll see entities like:
   - `device_tracker.johns_iphone`
   - `device_tracker.sarahs_laptop`
   - etc.
4. Copy these exact entity IDs to your application.yaml

### Customize Device Names

To make device names more readable in Home Assistant:

1. Go to **Settings** → **Devices & Services** → **UniFi Network**
2. Click on a device
3. Click the gear icon (settings)
4. Change **Name** to something friendly (e.g., "John's iPhone")
5. The entity ID will update automatically

## Step 4: Test Presence Detection

### Test Current Presence Query

1. Ensure devices are connected to the network (or simulate presence)

2. Test via curl:
   ```bash
   curl -X POST http://localhost:8080/api/conversation/process \
     -H "Content-Type: application/json" \
     -d '{"text":"Who is here now?"}'
   ```

3. Expected response:
   ```json
   {
     "response": {
       "speech": {
         "plain": {
           "speech": "2 employees are currently present: John Doe, Sarah Chen."
         }
       }
     }
   }
   ```

### Check Home Assistant Device States

1. Go to **Developer Tools** → **States**
2. Find your device_tracker entities
3. States should show:
   - `home` - Device is connected/present
   - `not_home` - Device is disconnected/absent

## Troubleshooting

### Issue: Can't connect to UniFi Controller

**Solution:**
1. Check controller is running: `docker ps | grep unifi`
2. Check logs: `docker logs unifi-controller`
3. Verify hostname resolution: `docker exec alfa1-wall-addon-home-assistant-1 ping unifi-controller`

### Issue: SSL Certificate Error

**Solution:**
- Make sure "Verify SSL certificate" is **unchecked** in integration config
- We're using self-signed certificates in Docker

### Issue: No devices showing up

**Solutions:**
1. Ensure devices are actually connected to UniFi network
2. Wait 5-10 minutes for initial device detection
3. Check UniFi Controller clients list: https://localhost:8443
4. Reload integration: Settings → Devices & Services → UniFi → ⋮ → Reload

### Issue: Device tracking not working

**Solutions:**
1. Verify entity IDs match in application.yaml
2. Check device state in Developer Tools → States
3. Ensure detection_time isn't too short (minimum 300 seconds recommended)
4. Check alfa-wall-addon logs: `docker logs alfa1-wall-addon-alfa-wall-addon-1`

## Architecture

```
Employee Device (Phone/Laptop)
          ↓
    WiFi Connection
          ↓
   UniFi Network Controller (Docker)
          ↓
  Home Assistant UniFi Integration
          ↓
   device_tracker entities
          ↓
  AlfaWall EmployeeCollectorAgent
          ↓
   getCurrentPresence() method
          ↓
    LED Visualization
```

## Configuration Reference

### UniFi Controller Docker Settings

From `docker-compose.yml`:
- **Web Interface:** https://localhost:8443
- **Device Communication:** Port 8082 (mapped from 8080)
- **Database:** MongoDB (unifi-db container)
- **Persistence:** `./unifi/unifi-data` directory

### Detection Time

The `detection_time` parameter (default 300 seconds) controls how long Home Assistant waits before marking a device as "away" after it disconnects.

- **Lower values (120-180s):** Faster detection but more false negatives
- **Higher values (300-600s):** More stable but slower updates
- **Recommended:** 300 seconds (5 minutes)

## Security Notes

⚠️ **Important for Production:**
- Change default UniFi admin password
- Use strong passwords for Home Assistant user
- Consider using proper SSL certificates
- Restrict UniFi Controller network access
- Current setup uses `verify_ssl: false` - acceptable for local Docker environment

## Next Steps

After integration is working:
1. Test presence detection with real devices
2. Configure employee device mappings in application.yaml
3. Test voice queries: "Who is here now?"
4. Monitor logs for presence detection accuracy
5. Adjust detection_time if needed

## Related Documentation

- `CLAUDE.md` - Main project documentation
- `IMPLEMENTATION.md` - Agent implementation details
- `application.yaml` - Employee device mappings configuration
