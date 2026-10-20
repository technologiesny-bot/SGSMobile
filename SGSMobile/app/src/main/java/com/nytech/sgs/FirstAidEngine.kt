package com.nytech.sgs

object FirstAidEngine {
    fun getProtocol(detections: List<String>): String {
        val protocols = mutableListOf<String>()

        if (detections.contains("WOUND_TRAUMA")) {
            protocols.add(
                "⚠️ [POSSIBLE WOUND / TISSUE INJURY DETECTED]\n" +
                    "• Apply continuous, direct, firm pressure with a clean cloth.\n" +
                    "• Do not remove embedded objects.\n" +
                    "• Non-diagnostic flag: Contact 112 emergency services immediately if bleeding is heavy."
            )
        }
        if (detections.contains("PALM_SCAN")) {
            protocols.add(
                "✋ [PALM / EXTREMITY ANALYZED]\n" +
                    "• Capillary refill visual inspection: Verify peripheral warmth.\n" +
                    "• If thermal/chemical burn: Run cool tap water for 15 minutes. Never apply ice."
            )
        }
        if (detections.contains("VEHICLE")) {
            protocols.add(
                "🚗 [ROAD / VEHICLE SCENE DETECTED]\n" +
                    "• Ensure scene safety before approaching.\n" +
                    "• Do not move suspected spinal injury victims unless in direct fire/explosion risk."
            )
        }
        if (protocols.isEmpty()) {
            return "✅ [SGS STATUS: NORMAL]\nNo immediate hazardous trauma patterns detected. Continue visual monitoring."
        }
        return protocols.joinToString("\n\n")
    }
}
