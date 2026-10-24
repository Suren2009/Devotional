package com.pradarshana.app.count

/** Set by the counting screen while volume-button counting is on. One press counts one name or round. */
object VolumeCount {
    @Volatile
    var handler: (() -> Boolean)? = null
}
