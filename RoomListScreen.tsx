// // RoomListScreen.tsx
// import React, { useMemo } from 'react';
// import { View, FlatList, TouchableOpacity, Text, StyleSheet } from 'react-native';
// import TopAppHeader from './TopAppHeader';
// import seed from './data-1000.json';
// import type { NativeStackScreenProps } from '@react-navigation/native-stack';
// import type { RootStackParamList } from './App';

// // (선택) 메시지 타입 명시 — 프로젝트 모델에 맞춰 확장 가능
// type ChatMessage = {
//   id: string;
//   sender: 'me' | 'other';
//   text: string;
//   time: string;
//   date: string;
//   // ...optional fields
// };

// type Props = NativeStackScreenProps<RootStackParamList, 'Rooms'>;

// export default function RoomListScreen({ navigation }: Props) {
//   // 1) 마지막 메시지 안전 접근
//   const last = (seed as ChatMessage[])[(seed as ChatMessage[]).length - 1];

//   // 2) JSON 직렬화 1회만
//   const messagesJson = useMemo(() => JSON.stringify(seed), []);

//   const rooms = [{ id: 'dev-room', title: '개발 톡방', lastMessage: last?.text }];

//   return (
//     <View style={{ flex: 1 }}>
//       <TopAppHeader title="채팅" subtitle="React Native ↔ Compose" onBack={() => {}} />

//       <FlatList
//         data={rooms}
//         keyExtractor={(item) => item.id}
//         ItemSeparatorComponent={() => (
//           <View style={{ height: StyleSheet.hairlineWidth, backgroundColor: '#eee' }} />
//         )}
//         // (선택) 스크롤 성능 파라미터
//         initialNumToRender={12}
//         windowSize={7}
//         renderItem={({ item }) => (
//           <TouchableOpacity
//             style={{ paddingHorizontal: 16, paddingVertical: 14 }}
//             onPress={() =>
//               navigation.navigate('ChatRoom', { roomId: item.id, messagesJson })
//             }
//           >
//             <Text style={{ fontSize: 16, fontWeight: '700' }}>{item.title}</Text>
//             {!!item.lastMessage && (
//               <Text style={{ marginTop: 4, color: '#666' }}>{item.lastMessage}</Text>
//             )}
//           </TouchableOpacity>
//         )}
//       />
//     </View>
//   );
// }
