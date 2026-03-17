# Key Rotation Specification

This document details the step-by-step process of the KMS key rotation and device synchronization.

## Protocol Workflow

1. **KMS Reassignment**:
   The process begins by removing the alias from the current active key and assigning it to the new target key in the Key Management Service (KMS).

2. **Coordinator Notification**:
   An endpoint trigger forces a reload. If the key state is updated, the Coordinator submits a rotation message to the **Hedera Consensus Service (HCS)**.

3. **Activation Window**:
   The message includes an `activateWindow` parameter (default: 10,000ms).

4. **Device Synchronization**:
    - Devices listening to the HCS topic receive the rotation message.
    - The device fetches/applies the new key but **only activates it** after verifying the window against the **Network Consensus Timestamp**.
    - This prevents **Clock Drift** issues, as the source of truth for "time" is the Hedera network, not the local device clock.

5. **Coordinator Safety**:
   To ensure no commands are lost or signed with mismatched keys, the Coordinator:
    - Stops sending commands immediately when rotation starts.
    - Resumes only after the safety margin: `ConsensusTimestamp + (activateWindow * 1.1)`.