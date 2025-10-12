package com.okcanvas.rnnative.model

/* ---------- 모델 ---------- */
data class Participant(
    val id: String,
    val name: String,
    val avatarUrl: String? = null
)

data class ChatRoom(
    val id: String,
    val title: String,
    val lastMessage: String,
    val unread: Int = 0,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val participants: List<Participant> = emptyList(),
    val timestamp: Long? = null // ✅ 단일 소스(정렬/표시 모두 이 값 기반)
)

/* ---------- 정렬 유틸: pinned 우선 → 최신(timestamp) ---------- */
fun List<ChatRoom>.sortedForList(): List<ChatRoom> =
    this.sortedWith(
        compareByDescending<ChatRoom> { it.pinned }
            .thenByDescending { it.timestamp ?: Long.MIN_VALUE }
            .thenByDescending { it.id } // tie-breaker
    )

/* ---------- 시간 헬퍼 ---------- */
private fun minutesAgo(m: Int): Long = System.currentTimeMillis() - m * 60_000L
private fun hoursAgo(h: Int): Long = System.currentTimeMillis() - h * 3_600_000L

/* ---------- 데모 데이터 (timestamp 기반) ---------- */
fun demoRooms(): List<ChatRoom> = listOf(
    ChatRoom(
        id = "1",
        title = "가족방",
        lastMessage = "저녁 뭐 먹지? 🍲",
        unread = 3,
        pinned = true,
        participants = listOf(
            Participant("p1","엄마","https://randomuser.me/api/portraits/women/65.jpg"),
            Participant("p2","아빠","https://randomuser.me/api/portraits/men/41.jpg"),
            Participant("p3","나","https://randomuser.me/api/portraits/men/22.jpg"),
            Participant("p4","Alice","https://randomuser.me/api/portraits/women/12.jpg")
        ),
        timestamp = minutesAgo(5)        // 최근
    ),
    ChatRoom(
        id = "2",
        title = "프로젝트 A",
        lastMessage = "내일 배포 일정 공유",
        muted = true,
        pinned = true,
        participants = listOf(
            Participant("p1","Alice","https://randomuser.me/api/portraits/women/12.jpg"),
            Participant("p2","Bob","https://randomuser.me/api/portraits/men/45.jpg")
        ),
        timestamp = minutesAgo(20)
    ),
    ChatRoom(
        id = "3",
        title = "친구들",
        lastMessage = "주말에 등산 어때",
        unread = 2,
        participants = listOf(
            Participant("p1","J","https://randomuser.me/api/portraits/men/33.jpg")
        ),
        timestamp = minutesAgo(70)
    ),
    ChatRoom(
        id = "4",
        title = "동호회 사진",
        lastMessage = "사진 공유했어요",
        participants = listOf(
            Participant("p1","A","https://randomuser.me/api/portraits/men/10.jpg"),
            Participant("p2","B","https://randomuser.me/api/portraits/women/11.jpg"),
            Participant("p3","C","https://randomuser.me/api/portraits/men/12.jpg")
        ),
        timestamp = minutesAgo(180)
    ),
    ChatRoom(
        id = "5",
        title = "개발 스터디",
        lastMessage = "코틀린 코루틴 주제로 진행",
        participants = listOf(
            Participant("p1","A","https://randomuser.me/api/portraits/men/10.jpg"),
            Participant("p2","B","https://randomuser.me/api/portraits/women/11.jpg"),
            Participant("p3","C","https://randomuser.me/api/portraits/men/12.jpg"),
            Participant("p4","D","https://randomuser.me/api/portraits/women/13.jpg"),
            Participant("p5","E","https://randomuser.me/api/portraits/men/14.jpg")
        ),
        timestamp = minutesAgo(190)
    ),
    ChatRoom(
        id = "6",
        title = "회사 공지",
        lastMessage = "연휴 일정 공지",
        unread = 10,
        participants = emptyList(),
        timestamp = hoursAgo(6)
    ),
    ChatRoom(
        id = "7",
        title = "디자인팀",
        lastMessage = "새 컴포넌트 가이드 초안",
        participants = listOf(
            Participant("p1","Mina","https://randomuser.me/api/portraits/women/31.jpg"),
            Participant("p2","Ethan","https://randomuser.me/api/portraits/men/32.jpg"),
            Participant("p3","Yuri","https://randomuser.me/api/portraits/women/33.jpg"),
            Participant("p4","Ken","https://randomuser.me/api/portraits/men/34.jpg")
        ),
        timestamp = hoursAgo(5)
    ),
    ChatRoom(
        id = "8",
        title = "런치 모임",
        lastMessage = "내일 파스타? 🍝",
        unread = 1,
        participants = listOf(
            Participant("p1","Paul","https://randomuser.me/api/portraits/men/40.jpg"),
            Participant("p2","Soo","https://randomuser.me/api/portraits/women/41.jpg")
        ),
        timestamp = hoursAgo(4)
    ),
    ChatRoom(
        id = "9",
        title = "운동 메이트",
        lastMessage = "오늘 8시 러닝 고?",
        participants = listOf(
            Participant("p1","Leo","https://randomuser.me/api/portraits/men/50.jpg"),
            Participant("p2","Jade","https://randomuser.me/api/portraits/women/51.jpg"),
            Participant("p3","Noah","https://randomuser.me/api/portraits/men/52.jpg"),
            Participant("p4","Emma","https://randomuser.me/api/portraits/women/53.jpg"),
            Participant("p5","Mason","https://randomuser.me/api/portraits/men/54.jpg")
        ),
        timestamp = hoursAgo(8)
    ),
    ChatRoom(
        id = "10",
        title = "여행 계획",
        lastMessage = "항공권 확인 완료 ✈️",
        participants = listOf(
            Participant("p1","Han","https://randomuser.me/api/portraits/men/61.jpg"),
            Participant("p2","Jin","https://randomuser.me/api/portraits/men/62.jpg"),
            Participant("p3","Yuna","https://randomuser.me/api/portraits/women/63.jpg")
        ),
        timestamp = hoursAgo(3)
    ),
    ChatRoom(
        id = "11",
        title = "북클럽",
        lastMessage = "이번 주는 3장까지 읽기",
        muted = true,
        participants = listOf(
            Participant("p1","Olivia","https://randomuser.me/api/portraits/women/70.jpg"),
            Participant("p2","James","https://randomuser.me/api/portraits/men/71.jpg")
        ),
        timestamp = hoursAgo(10)
    ),
    ChatRoom(
        id = "12",
        title = "반려견 모임",
        lastMessage = "주말 산책 코스 추천해요 🐶",
        participants = listOf(
            Participant("p1","Coco","https://randomuser.me/api/portraits/women/80.jpg"),
            Participant("p2","Max","https://randomuser.me/api/portraits/men/81.jpg"),
            Participant("p3","Luna","https://randomuser.me/api/portraits/women/82.jpg"),
            Participant("p4","Rocky","https://randomuser.me/api/portraits/men/83.jpg")
        ),
        timestamp = hoursAgo(9)
    ),
    ChatRoom(
        id = "13",
        title = "사진 동아리",
        lastMessage = "RAW 파일 공유 완료",
        unread = 5,
        participants = listOf(
            Participant("p1","Ian","https://randomuser.me/api/portraits/men/91.jpg")
        ),
        timestamp = hoursAgo(1)
    ),
    ChatRoom(
        id = "14",
        title = "축구팀",
        lastMessage = "일요일 5시 경기 ⚽️",
        participants = listOf(
            Participant("p1","Tom","https://randomuser.me/api/portraits/men/95.jpg"),
            Participant("p2","Ray","https://randomuser.me/api/portraits/men/96.jpg"),
            Participant("p3","Ben","https://randomuser.me/api/portraits/men/97.jpg"),
            Participant("p4","Kai","https://randomuser.me/api/portraits/men/98.jpg"),
            Participant("p5","Zed","https://randomuser.me/api/portraits/men/99.jpg")
        ),
        timestamp = hoursAgo(7)
    ),
    ChatRoom(
        id = "15",
        title = "오픈채팅-테크",
        lastMessage = "Compose Multiplatform 얘기 나왔어요",
        participants = emptyList(),
        timestamp = hoursAgo(12)
    )
).sortedForList()
