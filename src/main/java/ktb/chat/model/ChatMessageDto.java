package ktb.chat.model;


import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    // 메시지  타입 : 입장, 채팅
    public enum MessageType{
        ENTER, TALK
    }

    private MessageType messageType; // 메시지 타입
    private Long chatRoomId; // 방 번호
    private String nickname; // 채팅을 보낸 사람
    private String message; // 메시지
    private String profileImage; // 프로필 이미지
    private String ageGroup; // 연령대
    private int participants; // 참가자 수
}
