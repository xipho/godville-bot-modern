package ru.xipho.godvillebotmodern.bot

object Constants {
    const val BOT_EVENT_HEAL_FAILED_TEXT: String = "\uD83D\uDE33 Не удалось вылечить героя! Проверь, что происходит!"
    const val BOT_EVENT_FAILED_RESURRECT_HERO_TEXT: String = "❗️ Воскресить героя не удалось! Требуется вмешательство!"
    const val BOT_EVENT_HERO_DEAD_TEXT: String = "\uD83D\uDE35 Герой всё. Пытаемся воскресить!"
    const val BOT_EVENT_PET_BAD_CAN_HEAL_TEXT: String = "\uD83E\uDD11 Есть бабло на починку питомца! Действуй!"
    const val BOT_EVENT_PET_BAD_TEXT: String = "\uD83D\uDE31 БЕДА!!! Питомца контузило!!!"
    const val BOT_EVENT_PRANA_ACCUM_EMPTY_TEXT: String = "\uD83D\uDED1 В аккумуляторе закончилась прана! Пополни запасы как можно скорее!"
    const val BOT_EVENT_LOW_PRANA_LEVEL_TEXT: String = "\uD83D\uDE4F Маловато праны, распаковываем из аккумулятора!"
}
