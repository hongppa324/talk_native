// App.tsx / App.js
import React, { useEffect, useMemo, useState } from 'react';
import {
  StyleSheet,
  View,
  Alert,
  StatusBar,
  Text,
  TextInput,
  TouchableOpacity,
  TouchableWithoutFeedback,
  BackHandler,
} from 'react-native';
import rooms from './assets/rooms.json';
import seed from './assets/data-1000.json';
import ComposeChatListView from './ComposeChatListView';
import ComposeChatRoomView from './ComposeChatRoomView';

type SelectedRoom = { roomId: string; title: string } | null;

export default function App() {
  const [selected, setSelected] = useState<SelectedRoom>(null);
  const [input, setInput] = useState('');
  const roomsJson = useMemo(() => JSON.stringify(rooms), []);
  const messagesJson = useMemo(() => JSON.stringify(seed), []);

  const inRoom = !!selected;
  const roomId = selected?.roomId ?? '채팅';

  const handleBack = () => setSelected(null);

  // ✅ 안드로이드 하드웨어 뒤로가기: 방 화면이면 리스트로 복귀
  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (inRoom) {
        setSelected(null);
        return true;
      }
      return false;
    });
    return () => sub.remove();
  }, [inRoom]);

  return (
    <View style={styles.container}>
      <StatusBar barStyle="dark-content" />

      {/* 헤더 */}
      <View style={styles.header}>
        {inRoom ? (
          <View style={styles.headerRow}>
            <TouchableWithoutFeedback onPress={handleBack}>
              <View style={styles.backBtn}>
                <Text style={styles.backText}>{'‹'}</Text>
              </View>
            </TouchableWithoutFeedback>
            <Text style={styles.headerTitle}>{selected?.title ?? roomId}</Text>
            <View style={{ width: 32 }} />
          </View>
        ) : (
          <Text style={styles.headerTitle}>채팅</Text>
        )}
      </View>

      {/* 본문 */}
      {!inRoom ? (
        // ✅ 초기엔 리스트
        <ComposeChatListView
          key="chat-list"
          style={{ flex: 1 }}
          collapsable={false}        // 초기 프레임 미표시 방지에 도움
          roomsJson={roomsJson}      // 핵심
          onOpenRoom={(roomId, title) => {
            setSelected({ roomId, title })
          }}
          onPin={(roomId) => { 
            /* toggle pin */ 
            console.log('toggle pin', roomId)
          }}
          onMute={(roomId) => { 
            /* toggle mute */ 
            console.log('toggle mute', roomId)
          }}
          onLongPress={(roomId) => { 
            /* open sheet */ 
            console.log('open sheet', roomId)
          }}
        />
      ) : (
        // ✅ 방 선택 후 룸
        <ComposeChatRoomView
          key={selected?.roomId}
          style={{ flex: 1 }}
          roomId={roomId}
          messagesJson={messagesJson}
          collapsable={false}
          onBack={handleBack} // 네이티브 뒤로 이벤트가 있다면 연결
          onMessagesInvalid={(errors) => {
            if (!errors?.length) return;
            Alert.alert('메시지 포맷 오류', errors.join('\n'));
          }}
          onMessageClick={(msg, target) => {
            console.log('CLICK', target, msg);
            // if (target === 'Content' && msg.mediaUrls?.length) openViewer(...)
          }}
          onMessageLongPress={(msg, target) => {
            console.log('LONG', target, msg);
            // openActionSheet(msg, target)
          }}
        />
      )}

      {/* 입력창: 룸 화면에서만 표시 */}
      {inRoom && (
        <View style={styles.inputBar}>
          <TextInput
            value={input}
            onChangeText={setInput}
            style={styles.textInput}
            placeholder="메시지를 입력하세요..."
            multiline
          />
          <TouchableOpacity
            style={styles.sendButton}
            onPress={() => {
              const v = input.trim();
              if (!v) return;
              Alert.alert('보냄', v);
              setInput('');
            }}>
            <Text style={styles.sendText}>전송</Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  header: {
    height: 56,
    justifyContent: 'center',
    alignItems: 'center',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderColor: '#ddd',
    paddingHorizontal: 12,
  },
  headerRow: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'stretch',
    justifyContent: 'space-between',
  },
  backBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  backText: { fontSize: 26, lineHeight: 26, color: '#222' },
  headerTitle: { fontSize: 18, fontWeight: 'bold', color: '#222' },

  inputBar: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    padding: 8,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderColor: '#ddd',
    backgroundColor: '#fafafa',
  },
  textInput: {
    flex: 1,
    minHeight: 40,
    maxHeight: 100,
    paddingHorizontal: 8,
    paddingVertical: 4,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 6,
  },
  sendButton: {
    marginLeft: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
    backgroundColor: '#007AFF',
    borderRadius: 6,
  },
  sendText: { color: '#fff', fontWeight: '600' },
});
