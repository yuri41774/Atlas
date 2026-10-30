package com.example.atlas.data.model

data class HomeSummary(
    val familyName: String = "Família Silva",
    val pendingShoppingCount: Int = 5,
    val upcomingEvent: String = "Aniversário da Vovó (Amanhã - 19h)",
    val pendingTask: String = "Lavar a louça do jantar",
    val latestAnnouncement: String = "Lembrar de fechar os portões à noite!"
)
