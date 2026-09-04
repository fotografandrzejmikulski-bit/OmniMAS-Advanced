package pl.omnimas.advanced

class MemoryAgent {
    private val actions = ArrayDeque<Action>()
    private val states = ArrayDeque<String>()

    fun record(action: Action, state: String) {
        actions.addLast(action)
        states.addLast(state)
        while (actions.size > 30) actions.removeFirst()
        while (states.size > 30) states.removeFirst()
    }

    fun actions(): List<Action> = actions.toList()
    fun repeatedState(): Boolean = states.size >= 3 && states.takeLast(3).distinct().size == 1
    fun repeatedAction(): Boolean = actions.size >= 3 && actions.takeLast(3).distinct().size == 1
}

class SecurityAgent {
    fun risk(action: Action): String {
        if (action.type.equals("DONE", ignoreCase = true)) return "LOW"
        val text = (action.text ?: "").lowercase()
        if (listOf(
                "kup", "buy", "zapłać", "pay", "usuń konto", "delete account",
                "wyślij", "send", "przelew", "transfer", "publikuj", "publish"
            ).any(text::contains)
        ) return "HIGH"
        return action.risk.uppercase()
    }

    fun requiresConfirmation(action: Action): Boolean = risk(action) == "HIGH"
}
