// Per-version symbol swaps for Stonecutter. The shared src/ holds the newest version's
// identifiers; each entry maps them to the name used by `sc.current.parsed >= version`.
extra["swaps"] = mapOf(
    // MC 26.3 renamed LevelEvent.PARTICLES_AND_SOUND_WAX_ON -> PARTICLES_WAX_ON, and turned
    // SoundEvents.AXE_WAX_OFF into a Holder.Reference<SoundEvent> (needs .value() to pass to
    // Entity.playSound). The trailing comma in the AXE_WAX_OFF key keeps the forward direction a
    // no-op on the shared src/, which already carries the 26.3 form.
    "26.3" to mapOf(
        "PARTICLES_AND_SOUND_WAX_ON" to "PARTICLES_WAX_ON",
        "SoundEvents.AXE_WAX_OFF," to "SoundEvents.AXE_WAX_OFF.value(),",
    ),
)
