# Debug Logging Guide

When testing calls, check Logcat for these logs in order. Filter by tag: `AudioCallApp`

## Expected Log Sequence for Outgoing Call

### 1. Call Initiation
```
AudioCallApp: Initiating call to [calleeId] ([calleeName])
AudioCallApp: Creating peer connection...
```

### 2. WebRTC Initialization (if not already done)
```
AudioCallApp: Initializing WebRTC
AudioCallApp: Initializing PeerConnectionFactory...
AudioCallApp: PeerConnectionFactory initialized
AudioCallApp: Creating EGL base...
AudioCallApp: Creating encoder/decoder factories...
AudioCallApp: Creating PeerConnectionFactory...
AudioCallApp: PeerConnectionFactory created successfully: true
AudioCallApp: Initializing audio manager...
AudioCallApp: WebRTC initialization complete
```

### 3. Peer Connection Creation
```
AudioCallApp: createPeerConnection called, factory is NOT NULL
AudioCallApp: Creating peer connection with [X] ICE servers
AudioCallApp: Calling peerConnectionFactory.createPeerConnection()...
AudioCallApp: Peer connection created successfully
AudioCallApp: Adding local audio track...
AudioCallApp: addLocalAudioTrack: factory=NOT NULL, peerConnection=NOT NULL
AudioCallApp: Creating audio source...
AudioCallApp: Creating audio track...
AudioCallApp: Audio track created, adding to peer connection...
AudioCallApp: Audio track added to peer connection, result: [result]
AudioCallApp: Local audio track added
AudioCallApp: Peer connection created successfully
```

### 4. Offer Creation
```
AudioCallApp: About to call webRTCClient.createOffer()...
AudioCallApp: Creating WebRTC offer...
AudioCallApp: createOffer called, peerConnection is NOT NULL
AudioCallApp: Calling peerConnection.createOffer()...
AudioCallApp: peerConnection.createOffer() call completed
AudioCallApp: createOffer onCreateSuccess, sdp is NOT NULL
AudioCallApp: Setting local description, type: [type], sdp length: [length]
AudioCallApp: setLocalDescription onCreateSuccess
AudioCallApp: setLocalDescription onSetSuccess - calling callback
AudioCallApp: createOffer callback invoked! sdp type: [type]
AudioCallApp: Offer created successfully, type: [type], sdp length: [length]
```

### 5. Firestore Call Creation
```
AudioCallApp: Creating call in Firestore with offer...
AudioCallApp: initiateCall: callerId=[id], calleeId=[id], offer type=[type]
AudioCallApp: createCall: callerId=[id], calleeId=[id], offer type=[type], sdp length=[length]
AudioCallApp: Generated callId: [callId]
AudioCallApp: Writing call to Firestore: [callId]
AudioCallApp: Call document written successfully: [callId]
AudioCallApp: createCall SUCCESS: callId=[callId]
AudioCallApp: initiateCall result: true, [callId]
AudioCallApp: Call created successfully in Firestore, callId: [callId]
```

### 6. Service and Observation
```
AudioCallApp: Foreground service started
AudioCallApp: Starting to observe call and ICE candidates...
AudioCallApp: Starting to observe call: [callId]
```

### 7. Call Status Updates
```
AudioCallApp: Call [callId] status update: RINGING, isCaller: true
```

## Common Issues and What to Check

### Issue: Stuck at "Calling..." - No logs after "Creating WebRTC offer..."

**Possible causes:**
- WebRTC offer creation is failing silently
- Peer connection is null
- Audio track not added properly

**Check for:**
- `AudioCallApp: createOffer called, peerConnection is NULL` → Peer connection not created
- `AudioCallApp: createOffer onCreateFailure` → Offer creation failed
- `AudioCallApp: setLocalDescription onSetFailure` → Setting local description failed

### Issue: Offer created but Firestore call not created

**Check for:**
- `AudioCallApp: createOffer callback invoked!` → Should appear
- `AudioCallApp: Creating call in Firestore with offer...` → Should appear
- `AudioCallApp: createCall FAILED` → Firestore error
- Check for Firebase permission errors

### Issue: Call created but not observed

**Check for:**
- `AudioCallApp: Starting to observe call: [callId]` → Should appear
- `AudioCallApp: Call [callId] status update` → Should appear
- Check Firestore listener errors

## Filter Logcat

Use this filter in Android Studio Logcat:

```
tag:AudioCallApp
```

Or simply type:
```
AudioCallApp
```

Or filter by package:
```
package:avinash.app.audiocallapp
```

## Error Patterns

### WebRTC Initialization Failure
```
AudioCallApp: Error initializing WebRTC
```
→ Check if WebRTC library is properly included

### Peer Connection Creation Failure
```
AudioCallApp: Failed to create peer connection!
```
→ Check ICE server configuration

### Audio Track Failure
```
AudioCallApp: Failed to create audio track!
```
→ Check microphone permissions

### Firestore Permission Error
```
AudioCallApp: createCall FAILED
FirebaseFirestoreException: PERMISSION_DENIED
```
→ Check Firebase security rules and authentication

## Next Steps

1. Run the app and initiate a call
2. Copy all logs from Logcat (filter by the tags above)
3. Compare with the expected sequence above
4. Identify where the flow stops
5. Share the logs for further diagnosis
