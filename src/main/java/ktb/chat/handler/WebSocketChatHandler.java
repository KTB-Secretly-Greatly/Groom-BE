package ktb.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import ktb.chat.model.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChatHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper;

    private final Set<WebSocketSession> sessions = new HashSet<>();
    private final Map<Long, Set<WebSocketSession>> chatRoomSessionMap = new HashMap<>();
    private final Map<WebSocketSession, ChatMessageDto> sessionUserMap = new HashMap<>(); // 세션과 사용자 정보 매핑

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("{} 연결됨", session.getId());
        sessions.add(session);
    }

    // 소켓 통신 시 메세지의 전송을 다루는 부분
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("payload {}", payload);

        // 페이로드 -> chatMessageDto로 변환
        ChatMessageDto chatMessageDto = mapper.readValue(payload, ChatMessageDto.class);
        log.info("session {}", chatMessageDto.toString());

        Long chatRoomId = chatMessageDto.getChatRoomId();
        log.info("roomId {}", chatMessageDto.getChatRoomId());

        // 세션과 사용자 정보를 맵에 저장
        sessionUserMap.put(session, chatMessageDto);

        // 메모리 상에 채팅방에 대한 세션 없으면 만들어줌
        if (!chatRoomSessionMap.containsKey(chatRoomId)) {
            chatRoomSessionMap.put(chatRoomId, new HashSet<>());
        }
        Set<WebSocketSession> chatRoomSession = chatRoomSessionMap.get(chatRoomId);

        // 새로운 사용자가 들어왔을 때, 기존 참가자들에게 정보 전송
        if (chatMessageDto.getMessageType().equals(ChatMessageDto.MessageType.ENTER)) {
            chatRoomSession.add(session);
            // 기존 참가자들에게 새 참가자의 정보를 전송
            chatRoomSession.forEach(sess -> {
                if (sess != session && sess.isOpen()) {
                    ChatMessageDto newUserInfoMessage = new ChatMessageDto();
                    newUserInfoMessage.setMessageType(ChatMessageDto.MessageType.TALK);

                    sendMessage(sess, newUserInfoMessage);
                }
            });

            // 새로 들어온 참가자에게 기존 참가자들의 정보를 전송
            chatRoomSession.forEach(sess -> {
                if (sess != session && sess.isOpen()) {
                    ChatMessageDto existingUserInfo = sessionUserMap.get(sess);
                    if (existingUserInfo != null) {
                        ChatMessageDto existingUserInfoMessage = new ChatMessageDto();
                        existingUserInfoMessage.setMessageType(ChatMessageDto.MessageType.TALK);
                        existingUserInfoMessage.setMessage("나이 그룹은 " + existingUserInfo.getAgeGroup());

                        sendMessage(session, existingUserInfoMessage);
                    }
                }
            });
        }

        removeClosedSession(chatRoomSession);

        // 참가자 수 갱신
        int participantsCount = chatRoomSession.size();
        chatMessageDto.setParticipants(participantsCount);
        sendMessageToChatRoom(chatMessageDto, chatRoomSession);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("{} 연결 끊김", session.getId());
        sessions.remove(session);

        // 연결 종료 시 참가자 수 갱신
        chatRoomSessionMap.forEach((roomId, roomSessions) -> roomSessions.remove(session));
    }

    private void removeClosedSession(Set<WebSocketSession> chatRoomSession) {
        chatRoomSession.removeIf(sess -> !sessions.contains(sess));
    }

    private void sendMessageToChatRoom(ChatMessageDto chatMessageDto, Set<WebSocketSession> chatRoomSession) {
        chatRoomSession.parallelStream().forEach(sess -> sendMessage(sess, chatMessageDto));
    }

    public <T> void sendMessage(WebSocketSession session, T message) {
        try {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(message)));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
