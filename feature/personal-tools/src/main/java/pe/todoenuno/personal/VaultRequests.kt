package pe.todoenuno.personal

/** Main-thread request generation: locking or changing sides invalidates old results. */
internal class VaultRequests {
    private var generation=0L
    fun next():Long = ++generation
    fun invalidate() { generation++ }
    fun isCurrent(ticket:Long)=ticket==generation
}
