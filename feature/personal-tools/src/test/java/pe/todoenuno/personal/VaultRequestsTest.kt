package pe.todoenuno.personal

import org.junit.Assert.*
import org.junit.Test

class VaultRequestsTest {
    @Test fun lockingRejectsInFlightResult() {
        val requests=VaultRequests()
        val front=requests.next()
        requests.invalidate()
        assertFalse(requests.isCurrent(front))
    }
    @Test fun changingSidesAndUnlockingNeverReviveOldResult() {
        val requests=VaultRequests()
        val front=requests.next()
        val back=requests.next()
        assertFalse(requests.isCurrent(front))
        assertTrue(requests.isCurrent(back))
        requests.invalidate()
        val reopened=requests.next()
        assertFalse(requests.isCurrent(back))
        assertTrue(requests.isCurrent(reopened))
    }
}
