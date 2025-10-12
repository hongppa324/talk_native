package com.okcanvas.rnnative.model

/** 메시지 이모지 리액션(집계) */
data class Reaction(
    val emoji: String,   // 예: "😀", "❤️"
    val count: Int = 0   // 총 리액션 수
)

/** 채팅 메시지 타입 */
enum class MessageType {
    TEXT, IMAGE, FILE, STICKER, SYSTEM,
    AUDIO, VIDEO
}

/**
 * 채팅 메시지 모델
 */
data class ChatMessage(
    val id: String,
    val sender: String,                 // "me" or "other"
    val text: String,
    val time: String,
    val date: String,
    val displayName: String? = null,
    val type: MessageType = MessageType.TEXT,

    // ✅ 프로필 아바타 URL (발신자 이미지)
    val avatarUrl: String? = null,

    // ==== 미디어 관련 ====
    val mediaUrl: String? = null,
    val mediaUrls: List<String>? = null,
    val fileSize: String? = null,
    val mediaDuration: String? = null,

    // ✅ 비디오/파일/링크 등 썸네일 전용 URL
    val thumbnailUrl: String? = null,

    // ==== 읽음 관련 ====
    val totalRecipients: Int? = null,
    val readCount: Int? = null,

    // ==== 이모지 리액션 ====
    val reactions: List<Reaction> = emptyList()
) {
    /** 아직 읽지 않은 인원 수 */
    val unreadCount: Int?
        get() = if (totalRecipients != null && readCount != null) {
            (totalRecipients - readCount).coerceAtLeast(0)
        } else null

    /** 총 리액션 개수(모든 이모지 합) */
    val totalReactions: Int get() = reactions.sumOf { it.count }
}

/* ===== Reaction 유틸 ===== */

/** 특정 이모지의 카운트를 1 증가시키거나 없으면 새로 추가 */
fun ChatMessage.addReaction(emoji: String): ChatMessage {
    val current = reactions.toMutableList()
    val idx = current.indexOfFirst { it.emoji == emoji }
    if (idx >= 0) {
        val r = current[idx]
        current[idx] = r.copy(count = r.count + 1)
    } else {
        current += Reaction(emoji, 1)
    }
    val sorted = current.sortedWith(compareByDescending<Reaction> { it.count }.thenBy { it.emoji })
    return copy(reactions = sorted)
}

/** 특정 이모지의 카운트를 감소시키거나 0 이하일 시 제거 */
fun ChatMessage.removeReaction(emoji: String): ChatMessage {
    val current = reactions.toMutableList()
    val idx = current.indexOfFirst { it.emoji == emoji }
    if (idx >= 0) {
        val r = current[idx]
        val newCount = (r.count - 1).coerceAtLeast(0)
        if (newCount == 0) current.removeAt(idx)
        else current[idx] = r.copy(count = newCount)
    }
    val sorted = current.sortedWith(compareByDescending<Reaction> { it.count }.thenBy { it.emoji })
    return copy(reactions = sorted)
}

/** 특정 이모지의 카운트를 외부에서 지정 */
fun ChatMessage.withReactionCount(emoji: String, count: Int): ChatMessage {
    val filtered = reactions.filterNot { it.emoji == emoji }.toMutableList()
    if (count > 0) filtered += Reaction(emoji, count)
    val sorted = filtered.sortedWith(compareByDescending<Reaction> { it.count }.thenBy { it.emoji })
    return copy(reactions = sorted)
}

/* ===== 읽음 관련 유틸 ===== */

/** 읽은 인원 수 변경 */
fun ChatMessage.withReadCount(count: Int): ChatMessage {
    val safeCount = count.coerceAtLeast(0)
    return copy(readCount = safeCount)
}

/** 전체 수신자 수 변경 */
fun ChatMessage.withTotalRecipients(count: Int): ChatMessage {
    val safeCount = count.coerceAtLeast(0)
    return copy(totalRecipients = safeCount)
}

/** 읽은 수와 전체 수신자 수를 한 번에 변경 */
fun ChatMessage.withReadStatus(read: Int, total: Int): ChatMessage {
    val safeRead = read.coerceAtLeast(0)
    val safeTotal = total.coerceAtLeast(0)
    return copy(readCount = safeRead, totalRecipients = safeTotal)
}

/* ===== 데모 데이터 (현실에서 자주 나오는 30케이스) ===== */

fun demoMessages(): MutableList<ChatMessage> = mutableListOf(
    ChatMessage(
        id = "1",
        sender = "other",
        text = "어제 얘기한 일정 다시 한 번만 확인해줘!",
        time = "오전 9:02",
        date = "2025-10-03",
        displayName = "상대",
        avatarUrl = "https://randomuser.me/api/portraits/women/65.jpg",
        totalRecipients = 3, readCount = 1,
        reactions = listOf(Reaction("👀", 1))
    ),
    ChatMessage(
        id = "2",
        sender = "me",
        text = "ㅇㅋ 오늘 오후 3시에 리마인드 걸어둘게.",
        time = "오전 9:05",
        date = "2025-10-03",
        avatarUrl = "https://randomuser.me/api/portraits/men/41.jpg",
        totalRecipients = 3, readCount = 2,
        reactions = listOf(Reaction("👍", 2))
    ),
    ChatMessage(
        id = "3",
        sender = "other",
        text = "참고 링크: https://kakaostyle.example.com/docs/plan",
        time = "오전 9:17",
        date = "2025-10-03",
        displayName = "박지민",
        avatarUrl = "https://randomuser.me/api/portraits/men/14.jpg",
        totalRecipients = 3, readCount = 2
    ),
    ChatMessage(
        id = "4",
        sender = "other",
        text = "이미지 몇 장 공유할게!",
        time = "오전 9:18",
        date = "2025-10-03",
        displayName = "박지민",
        type = MessageType.IMAGE,
        mediaUrls = listOf(
            "https://picsum.photos/seed/p1/900/600",
            "https://picsum.photos/seed/p2/900/600",
            "https://picsum.photos/seed/p3/900/600"
        ),
        totalRecipients = 3, readCount = 2,
        reactions = listOf(Reaction("😍", 3))
    ),
    ChatMessage(
        id = "5",
        sender = "me",
        text = "첫 번째가 제일 괜찮다!",
        time = "오전 9:22",
        date = "2025-10-03",
        avatarUrl = "https://randomuser.me/api/portraits/men/41.jpg",
        reactions = listOf(Reaction("❤️", 2), Reaction("👍", 1)),
        totalRecipients = 3, readCount = 2
    ),
    ChatMessage(
        id = "6",
        sender = "other",
        text = "회의록 첨부합니다.",
        time = "오전 10:01",
        date = "2025-10-03",
        displayName = "최유진",
        type = MessageType.FILE,
        mediaUrl = "https://example.com/meeting_notes_2025-10-03.pdf",
        fileSize = "2.1MB",
        totalRecipients = 3, readCount = 3
    ),
    ChatMessage(
        id = "7",
        sender = "me",
        text = "스티커!",
        time = "오전 10:05",
        date = "2025-10-03",
        type = MessageType.STICKER,
        mediaUrl = "sticker://cheer",
        totalRecipients = 3, readCount = 3,
        reactions = listOf(Reaction("🎉", 4))
    ),
    ChatMessage(
        id = "8",
        sender = "other",
        text = "새 멤버가 초대되었습니다.",
        time = "오전 10:10",
        date = "2025-10-03",
        type = MessageType.SYSTEM
    ),
    ChatMessage(
        id = "9",
        sender = "other",
        text = "회의 때 쓸 음성 코멘트 남겨둠(12초)",
        time = "오전 10:30",
        date = "2025-10-03",
        displayName = "상대",
        type = MessageType.AUDIO,
        mediaUrl = DemoAssets.AUDIO_SAMPLE_URL,
        mediaDuration = "00:12",
        totalRecipients = 4, readCount = 1,
        reactions = listOf(Reaction("👂", 2))
    ),
    ChatMessage(
        id = "10",
        sender = "me",
        text = "굿. 오후에는 비디오도 참고해줘!",
        time = "오전 10:31",
        date = "2025-10-03",
        totalRecipients = 4, readCount = 3
    ),
    ChatMessage(
        id = "11",
        sender = "me",
        text = "샘플 영상 공유",
        time = "오전 10:32",
        date = "2025-10-03",
        type = MessageType.VIDEO,
        mediaUrl = DemoAssets.VIDEO_SAMPLE_1,
        mediaDuration = "00:30",
        thumbnailUrl = "https://picsum.photos/seed/video_thumb_1/1280/720", // ✅ 썸네일
        totalRecipients = 4, readCount = 3,
        reactions = listOf(Reaction("🔥", 1), Reaction("😂", 2))
    ),
    ChatMessage(
        id = "12",
        sender = "other",
        text = "지금은 바빠서 저녁에 볼게요!",
        time = "오전 11:05",
        date = "2025-10-03",
        displayName = "최유진",
        totalRecipients = 4, readCount = 2
    ),
    ChatMessage(
        id = "13",
        sender = "me",
        text = "넵! 🙌",
        time = "오전 11:06",
        date = "2025-10-03",
        totalRecipients = 4, readCount = 4,
        reactions = listOf(Reaction("🙌", 3))
    ),
    ChatMessage(
        id = "14",
        sender = "other",
        text = "점심 뭐 먹을까요?",
        time = "오후 12:01",
        date = "2025-10-03",
        displayName = "박지민",
        totalRecipients = 4, readCount = 2
    ),
    ChatMessage(
        id = "15",
        sender = "me",
        text = "근처에 새로 생긴 쌀국수집 어때? 🍜",
        time = "오후 12:03",
        date = "2025-10-03",
        totalRecipients = 4, readCount = 3,
        reactions = listOf(Reaction("👍", 2))
    ),
    ChatMessage(
        id = "16",
        sender = "other",
        text = "좋아요 ㅎㅎ 1시에 봬요.",
        time = "오후 12:05",
        date = "2025-10-03",
        displayName = "상대",
        totalRecipients = 4, readCount = 3
    ),
    ChatMessage(
        id = "17",
        sender = "other",
        text = "방 제목이 변경되었습니다.",
        time = "오후 12:06",
        date = "2025-10-03",
        type = MessageType.SYSTEM
    ),
    ChatMessage(
        id = "18",
        sender = "me",
        text = "방금 배포 끝났어요. 버전 v2.1.0!",
        time = "오후 2:10",
        date = "2025-10-03",
        totalRecipients = 4, readCount = 2,
        reactions = listOf(Reaction("🎉", 6), Reaction("💯", 1))
    ),
    ChatMessage(
        id = "19",
        sender = "other",
        text = "릴리즈 노트 파일 첨부합니다.",
        time = "오후 2:12",
        date = "2025-10-03",
        displayName = "최유진",
        type = MessageType.FILE,
        mediaUrl = "https://example.com/release_v2.1.0.txt",
        fileSize = "14KB",
        totalRecipients = 4, readCount = 3
    ),
    ChatMessage(
        id = "20",
        sender = "me",
        text = "디자인 시안은 이거로 가죠!",
        time = "오후 3:01",
        date = "2025-10-04",
        type = MessageType.IMAGE,
        mediaUrls = listOf("https://picsum.photos/seed/ui1/900/600"),
        totalRecipients = 4, readCount = 3,
        reactions = listOf(Reaction("❤️", 2), Reaction("👍", 1))
    ),
    ChatMessage(
        id = "21",
        sender = "other",
        text = "좋습니다. 폰트는 Pretendard로.",
        time = "오후 3:05",
        date = "2025-10-04",
        displayName = "박지민",
        totalRecipients = 4, readCount = 2
    ),
    ChatMessage(
        id = "22",
        sender = "me",
        text = "참여자 분들 모두 확인 부탁드려요 🙏",
        time = "오후 3:10",
        date = "2025-10-04",
        totalRecipients = 6, readCount = 2,
        reactions = listOf(Reaction("🙏", 2))
    ),
    ChatMessage(
        id = "23",
        sender = "other",
        text = "스티커로 답장 😀",
        time = "오후 3:11",
        date = "2025-10-04",
        displayName = "상대",
        type = MessageType.STICKER,
        mediaUrl = "sticker://smile",
        totalRecipients = 6, readCount = 2
    ),
    ChatMessage(
        id = "24",
        sender = "other",
        text = "동영상도 하나 더 첨부할게요",
        time = "오후 3:40",
        date = "2025-10-04",
        displayName = "최유진",
        type = MessageType.VIDEO,
        mediaUrl = DemoAssets.VIDEO_SAMPLE_2,
        mediaDuration = "00:42",
        thumbnailUrl = "https://picsum.photos/seed/video_thumb_2/1280/720", // ✅ 썸네일
        totalRecipients = 6, readCount = 4,
        reactions = listOf(Reaction("👏", 2))
    ),
    ChatMessage(
        id = "25",
        sender = "me",
        text = "오디오로 설명 더함(8초)",
        time = "오후 3:41",
        date = "2025-10-04",
        type = MessageType.AUDIO,
        mediaUrl = DemoAssets.AUDIO_SAMPLE_URL,
        mediaDuration = "00:08",
        totalRecipients = 6, readCount = 5
    ),
    ChatMessage(
        id = "26",
        sender = "other",
        text = "오늘 저녁 일정 확정: 오후 6시 회의실 B",
        time = "오후 4:05",
        date = "2025-10-04",
        displayName = "박지민",
        totalRecipients = 6, readCount = 3,
        reactions = listOf(Reaction("✅", 2))
    ),
    ChatMessage(
        id = "27",
        sender = "me",
        text = "확인! 회의 끝나고 바로 정리해서 공유할게요.",
        time = "오후 4:06",
        date = "2025-10-04",
        totalRecipients = 6, readCount = 6
    ),
    ChatMessage(
        id = "28",
        sender = "other",
        text = "사진 하나만 더!",
        time = "오전 9:31",
        date = "2025-10-05",
        displayName = "상대",
        type = MessageType.IMAGE,
        mediaUrls = listOf("https://picsum.photos/seed/p4/900/600"),
        totalRecipients = 6, readCount = 2
    ),
    ChatMessage(
        id = "29",
        sender = "me",
        text = "좋네요. 이 버전으로 진행합시다.",
        time = "오전 9:35",
        date = "2025-10-05",
        totalRecipients = 6, readCount = 4,
        reactions = listOf(Reaction("👍", 3), Reaction("💪", 1))
    ),
    ChatMessage(
        id = "30",
        sender = "other",
        text = "공지: 오후 2시에 서비스 점검이 예정되어 있습니다.",
        time = "오전 10:00",
        date = "2025-10-06",
        type = MessageType.SYSTEM
    )
)
