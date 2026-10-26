package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GestureType
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallRoomRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private fun uniqueRoomCode(): String {
    val suffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
    return "RM$suffix"
  }

  @Test
  fun createRoom_andObserveHostedRooms_succeedsForAuthenticatedHost() = runBlocking {
    val hostUid = signInTestUser(ALICE_EMAIL)
    val repo = CallRoomRepository(firestore)
    val code = uniqueRoomCode()

    val createdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createRoom(code, "AI Gesture Demo Call")
    }
    assertTrue("Expected room creation to succeed: ${createdResult.exceptionOrNull()}", createdResult.isSuccess)

    val hostedRooms = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMyHostedRooms(hostUid).first { list -> list.any { it.roomCode == code } }
    }
    assertTrue(hostedRooms.any { it.roomCode == code })
  }

  @Test
  fun joinRoom_andSendReaction_syncsAcrossHostAndGuest() = runBlocking {
    val hostUid = signInTestUser(ALICE_EMAIL)
    val repo = CallRoomRepository(firestore)
    val code = uniqueRoomCode()

    val room = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createRoom(code, "1-on-1 Heart Reaction Room").getOrThrow()
    }
    assertEquals(hostUid, room.hostUid)

    // Switch to Bob (Guest) and join
    val guestUid = signInTestUser(BOB_EMAIL)
    val joinedRoom = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.joinRoom(code).getOrThrow()
    }
    assertEquals("active", joinedRoom.status)
    assertEquals(guestUid, joinedRoom.guestUid)

    // Bob triggers a HEART reaction
    val reactionResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.sendReaction(
        room = joinedRoom,
        gestureType = GestureType.HEART,
        source = "AI_GESTURE",
        originX = 50,
        originY = 85,
        confidencePercent = 98,
      )
    }
    assertTrue("Expected reaction send to succeed: ${reactionResult.exceptionOrNull()}", reactionResult.isSuccess)
    val reactionId = reactionResult.getOrThrow()

    // Verify Alice can observe the synced reaction in real time
    signInTestUser(ALICE_EMAIL)
    val reactions = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeReactions(code, hostUid).first { list -> list.any { it.id == reactionId } }
    }
    assertTrue(reactions.any { it.id == reactionId && it.gestureType == "HEART" })
  }

  @Test
  fun getRoomByCode_crossUserNonParticipant_failsWithPermissionDenied() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = CallRoomRepository(firestore)
    val code = uniqueRoomCode()
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createRoom(code, "Private Call").getOrThrow()
    }

    // Switch to Mallory (non-participant) and attempt to read Alice's room
    signInTestUser(MALLORY_EMAIL)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.getRoomByCode(code)
    }
    assertTrue(result.isFailure)
    val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
  }

  @Test
  fun observeHostedRooms_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = CallRoomRepository(firestore)

    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeMyHostedRooms("unauthenticated_uid").first()
      }
    } catch (e: Throwable) {
      caught = e
    }
    assertNotNull("Expected unauthenticated query to throw", caught)
    val firestoreEx = caught?.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@pulsecall.test"
    const val BOB_EMAIL = "bob@pulsecall.test"
    const val MALLORY_EMAIL = "mallory@pulsecall.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
