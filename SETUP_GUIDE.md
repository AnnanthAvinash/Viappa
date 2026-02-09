# Audio Call App - Setup Guide

## Quick Start

This guide will help you set up and test the Audio Call App.

## 1. Firebase Setup

### 1.1 Enable Anonymous Authentication

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **Authentication** → **Sign-in method**
4. Enable **Anonymous** authentication
5. Click **Save**

### 1.2 Create Firestore Database

1. In Firebase Console, go to **Firestore Database**
2. Click **Create database**
3. Start in **Test mode** (for initial testing)
4. Choose a location (preferably close to your users)
5. Click **Enable**

### 1.3 Set Firestore Security Rules

1. In Firestore Database, go to **Rules** tab
2. Replace the rules with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
    match /uid_mapping/{uid} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
    match /calls/{callId} {
      allow read, write: if request.auth != null;
      match /callerCandidates/{candidateId} {
        allow read, write: if request.auth != null;
      }
      match /calleeCandidates/{candidateId} {
        allow read, write: if request.auth != null;
      }
    }
  }
}
```

3. Click **Publish**

### 1.4 Verify google-services.json

- Ensure `app/google-services.json` is present and matches your Firebase project
- If missing, download it from Firebase Console → Project Settings → Your apps

## 2. TURN Server Configuration (Optional but Recommended)

### 2.1 Choose a TURN Provider

**Option A: Metered.ca (Free for Testing)**
- Sign up at https://metered.ca
- Get your TURN credentials from the dashboard
- Free tier available with rate limits

**Option B: Twilio TURN (Production)**
- Create account at https://www.twilio.com
- Get TURN credentials from Twilio Console
- Pay-as-you-go pricing (~$0.40 per GB)

**Option C: Self-hosted coturn**
- Deploy coturn on your server
- Configure credentials

### 2.2 Configure TURN Servers

1. Open `local.properties` in the project root (create if it doesn't exist)
2. Add your TURN server credentials:

```properties
TURN_SERVER=your-turn-server.com
TURN_USERNAME=your_username
TURN_PASSWORD=your_password
```

**Example for Metered.ca:**
```properties
TURN_SERVER=a.relay.metered.ca
TURN_USERNAME=your_metered_username
TURN_PASSWORD=your_metered_password
```

3. Rebuild the app:
```bash
./gradlew clean assembleDebug
```

**Note:** If TURN servers are not configured, the app will work on the same network (Wi-Fi) but may fail on mobile data or different networks.

## 3. Firestore Composite Index

The app requires a composite index for incoming calls. This will be created automatically when you first test incoming calls:

1. Run the app on two devices
2. Make a call from Device 1 to Device 2
3. Check Logcat for an index creation link
4. Click the link to create the index automatically

**OR** create manually in Firebase Console:
- Go to Firestore Database → Indexes
- Create composite index:
  - Collection: `calls`
  - Fields: `calleeId` (Ascending), `status` (Ascending), `createdAt` (Descending)

## 4. Testing Checklist

### Basic Functionality

- [ ] **Registration**: Register two users with unique IDs
- [ ] **User List**: Verify both users appear in each other's list
- [ ] **Outgoing Call**: Initiate call, verify connection, test audio
- [ ] **Incoming Call**: Receive call, accept, verify connection
- [ ] **Call Controls**: Test mute, speaker, end call

### Network Resilience

- [ ] **Wi-Fi to Mobile Data**: Switch networks during call, verify reconnection
- [ ] **Mobile Data to Wi-Fi**: Switch networks during call, verify reconnection
- [ ] **TURN on Mobile Data**: Test call on mobile data with TURN configured

### Lifecycle

- [ ] **Background**: Put app in background, verify call continues
- [ ] **Screen Lock**: Lock screen during call, verify call continues
- [ ] **Process Death**: Force stop app during call, reopen, verify recovery

## 5. Troubleshooting

### Calls Not Connecting

1. **Check Firebase**: Verify Anonymous Auth is enabled
2. **Check Firestore**: Verify database is created and rules are published
3. **Check Network**: Ensure both devices have internet
4. **Check TURN**: If on mobile data, ensure TURN servers are configured
5. **Check Logs**: Review Logcat for error messages

### Incoming Calls Not Received

1. **Check Firestore Index**: Verify composite index is created
2. **Check Logs**: Look for Firestore query errors
3. **Check Permissions**: Ensure app has all required permissions

### Audio Issues

1. **Check Permissions**: Verify microphone permission is granted
2. **Check Audio Focus**: Ensure no other app is using microphone
3. **Test on Different Devices**: Rule out device-specific issues

## 6. Build Configuration

The app uses BuildConfig for TURN server configuration. To verify:

1. Build the app: `./gradlew assembleDebug`
2. Check `app/build/generated/source/buildConfig/.../BuildConfig.java`
3. Verify TURN_SERVER, TURN_USERNAME, TURN_PASSWORD are set correctly

## 7. Production Checklist

Before releasing to production:

- [ ] Update Firestore security rules (remove test mode)
- [ ] Configure production TURN servers (Twilio recommended)
- [ ] Move TURN credentials to secure storage (not BuildConfig)
- [ ] Enable ProGuard/R8 with proper rules
- [ ] Test on multiple devices and networks
- [ ] Monitor Firebase usage and costs
- [ ] Set up Firebase Analytics for monitoring

## Support

For issues or questions:
1. Check Logcat for detailed error messages
2. Verify Firebase Console for authentication and database status
3. Review network connectivity on both devices
