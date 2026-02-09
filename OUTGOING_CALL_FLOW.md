# Outgoing Call Flow Verification

## Complete Flow Diagram

### 1. User Initiates Call
```
UserListScreen
  └─> User clicks on user card
      └─> onCallUser(calleeId, calleeName)
          └─> Navigation to CallScreen(calleeId, calleeName)
              └─> CallScreen LaunchedEffect
                  └─> CallViewModel.initiateOutgoingCall(calleeId, calleeName)
```

### 2. CallViewModel.initiateOutgoingCall() - Step by Step

#### Step 2.1: Authentication Check
- ✅ Gets current user from `authRepository.getCurrentUser()`
- ✅ If no user → Sets state to FAILED, returns

#### Step 2.2: UI State Update
- ✅ Sets UI state to `OUTGOING`
- ✅ Saves call state to `SavedStateHandle`
- ✅ Log: `[OUTGOING] Starting call to [calleeName] ([calleeId])`

#### Step 2.3: Create Peer Connection
- ✅ Calls `webRTCClient.createPeerConnection()` with callbacks:
  - `onIceCandidate`: Adds caller ICE candidates to Firestore (`callerCandidates` collection)
  - `onConnectionStateChange`: Handles connection state changes
  - `onRemoteTrack`: Receives remote audio track
- ✅ Log: `[OUTGOING] Creating peer connection...`
- ✅ Log: `[WEBRTC] Peer connection created successfully`

#### Step 2.4: Create Offer
- ✅ Calls `webRTCClient.createOffer()`
- ✅ When offer is created:
  - Converts to `SessionDescription`
  - Log: `[OUTGOING] Creating call in Firestore with offer...`
  - Calls `callUseCase.initiateCall()` which:
    - Updates user status to `IN_CALL`
    - Calls `signalingRepository.createCall()` which:
      - Creates call document in Firestore with status `RINGING`
      - Includes offer SDP
      - Returns `callId`
  - Updates UI state with `callId`
  - Starts foreground service
  - **Starts observing:**
    - `observeCall(callId)` - Watches for status changes (RINGING → ACCEPTED)
    - `observeCalleeIceCandidates(callId)` - Watches for callee's ICE candidates

### 3. Waiting for Callee to Answer

#### Step 3.1: Callee Receives Call
- Callee's `observeIncomingCalls()` detects new call with status `RINGING`
- Callee shows incoming call overlay
- When callee accepts:
  - Callee creates peer connection
  - Callee sets remote description (offer)
  - Callee creates answer
  - Callee saves answer to Firestore (status → `ACCEPTED`)

#### Step 3.2: Caller Observes Answer
- Caller's `observeCall(callId)` detects status change to `ACCEPTED`
- Caller checks if answer exists and `isRemoteDescriptionSet == false`
- ✅ Log: `[OUTGOING] Answer received ([size] bytes)`
- ✅ Calls `webRTCClient.setRemoteDescription(answer)`
- ✅ Sets `isRemoteDescriptionSet = true`
- ✅ Processes pending ICE candidates
- ✅ Log: `[OUTGOING] Setting UI state to CONNECTING`
- ✅ Updates UI state to `CONNECTING`

### 4. ICE Candidate Exchange

#### Step 4.1: Caller ICE Candidates
- Caller's peer connection generates ICE candidates
- `onIceCandidate` callback fires
- ✅ Adds to Firestore: `calls/{callId}/callerCandidates/{candidateId}`
- Callee's `observeCallerIceCandidates(callId)` receives them
- Callee adds them to peer connection

#### Step 4.2: Callee ICE Candidates
- Callee's peer connection generates ICE candidates
- Callee adds to Firestore: `calls/{callId}/calleeCandidates/{candidateId}`
- Caller's `observeCalleeIceCandidates(callId)` receives them
- ✅ Caller adds them to peer connection

### 5. Connection Established

#### Step 5.1: WebRTC Connection State
- WebRTC `onConnectionChange` fires with states:
  - `NEW` → `CONNECTING` → `CONNECTED`
- ✅ `handleConnectionStateChange()` is called
- ✅ Log: `[OUTGOING] Connection state changed to: CONNECTED`
- ✅ Updates UI state to `CONNECTED`
- ✅ Starts call duration timer
- ✅ Saves call state

#### Step 5.2: Audio Stream
- Remote audio track received
- ✅ Log: `[OUTGOING] Remote audio track received: audio`
- Audio playback starts

## Potential Issues Found

### ✅ Issue 1: ICE Candidate Timing
**Status: VERIFIED CORRECT**
- Caller observes `calleeCandidates` collection ✅
- Callee observes `callerCandidates` collection ✅
- This is correct - each side observes the other's candidates

### ✅ Issue 2: Remote Description Timing
**Status: VERIFIED CORRECT**
- Caller sets remote description (answer) only when status is `ACCEPTED` ✅
- `isRemoteDescriptionSet` flag prevents duplicate setting ✅
- Pending ICE candidates are processed after remote description is set ✅

### ✅ Issue 3: Connection State Updates
**Status: VERIFIED CORRECT**
- `handleConnectionStateChange()` updates UI state when WebRTC state changes ✅
- UI state is set to `CONNECTING` when answer is received ✅
- UI state is set to `CONNECTED` when WebRTC connection is established ✅

### ⚠️ Issue 4: Missing Logs
**Status: FIXED**
- Added comprehensive logs for all connection flow steps ✅
- All state transitions are logged ✅
- ICE candidate exchange is logged ✅

## Expected Log Sequence for Outgoing Call

```
[OUTGOING] Starting call to [calleeName] ([calleeId])
[OUTGOING] Creating peer connection...
[WEBRTC] Creating peer connection...
[WEBRTC] ICE servers: [count]
[WEBRTC] Peer connection created successfully
[OUTGOING] Creating WebRTC offer...
[OUTGOING] Offer created successfully
[OUTGOING] Creating call in Firestore with offer...
[OUTGOING] Call created successfully in Firestore, callId: [callId]
[OUTGOING] Observing call: [callId]
[OUTGOING] Listening for callee ICE candidates
[OUTGOING] Answer received ([size] bytes)
[OUTGOING] Setting remote ANSWER...
[WEBRTC] Remote ANSWER set
[OUTGOING] Answer set
[OUTGOING] Setting UI state to CONNECTING
[WEBRTC] Connection state changed: CONNECTING
[OUTGOING] Connection state changed to: CONNECTING
[OUTGOING] State: CONNECTING - Establishing connection
[WEBRTC] ICE connection: CHECKING
[WEBRTC] ICE connection: CONNECTED
[WEBRTC] Connection state changed: CONNECTED
[OUTGOING] Connection state changed to: CONNECTED
[OUTGOING] State: CONNECTED - Call active
[OUTGOING] Remote audio track received: audio
```

## Verification Checklist

- [x] Peer connection created before offer
- [x] Offer created and saved to Firestore
- [x] Call observation started
- [x] Callee ICE candidates observation started
- [x] Answer received and remote description set
- [x] UI state updated to CONNECTING when answer received
- [x] ICE candidates exchanged correctly
- [x] Connection state changes handled
- [x] UI state updated to CONNECTED when WebRTC connects
- [x] All steps have proper logging

## Conclusion

The outgoing call flow is **CORRECTLY IMPLEMENTED**. All steps are in the right order, and the logic handles:
- ✅ Proper sequencing of peer connection, offer, and answer
- ✅ Correct ICE candidate exchange (caller → calleeCandidates, callee → callerCandidates)
- ✅ State management (OUTGOING → CONNECTING → CONNECTED)
- ✅ Error handling and timeouts
- ✅ Comprehensive logging

The flow should work correctly. If calls are stuck at "Connecting", check:
1. Are ICE candidates being exchanged? (Check logs for ICE candidate messages)
2. Is the WebRTC connection state changing? (Check for `[WEBRTC] Connection state changed` logs)
3. Are both devices on the same network or using TURN servers?
