// ComposeChatListView.tsx
import React, { forwardRef, memo } from 'react';
import { requireNativeComponent, type ViewProps } from 'react-native';

type OpenRoomNativeEvent = { roomId: string; title: string };
type IdNativeEvent = { roomId: string };

// === 네이티브 뷰가 직접 받는 Props (requireNativeComponent 제네릭과 일치) ===
type NativeProps = ViewProps & {
  roomsJson?: string; // @ReactProp(name="roomsJson")
  // Direct events (Android: getExportedCustomDirectEventTypeConstants 등록명과 동일)
  onOpenRoom?: (e: { nativeEvent: OpenRoomNativeEvent }) => void;
  onPin?: (e: { nativeEvent: IdNativeEvent }) => void;
  onMute?: (e: { nativeEvent: IdNativeEvent }) => void;
  onLongPress?: (e: { nativeEvent: IdNativeEvent }) => void;
};

const NativeIMChatListView = requireNativeComponent<NativeProps>('IMChatListView');

// === 외부에 노출할 래퍼 Props (사용성 좋은 시그니처) ===
export type ComposeChatListViewProps = ViewProps & {
  roomsJson?: string;
  onOpenRoom?: (roomId: string, title: string) => void;
  onPin?: (roomId: string) => void;
  onMute?: (roomId: string) => void;
  onLongPress?: (roomId: string) => void;
};

const ComposeChatListView = forwardRef<any, ComposeChatListViewProps>(
  function ComposeChatListView(
    { onOpenRoom, onPin, onMute, onLongPress, style, ...rest },
    ref
  ) {
    return (
      <NativeIMChatListView
        ref={ref}
        {...rest}
        style={style ?? { flex: 1 }}
        onOpenRoom={(e) => {
          const { roomId, title } = e.nativeEvent;
          onOpenRoom?.(roomId, title);
        }}
        onPin={(e) => {
          const { roomId } = e.nativeEvent;
          onPin?.(roomId);
        }}
        onMute={(e) => {
          const { roomId } = e.nativeEvent;
          onMute?.(roomId);
        }}
        onLongPress={(e) => {
          const { roomId } = e.nativeEvent;
          onLongPress?.(roomId);
        }}
      />
    );
  }
);

export default memo(ComposeChatListView);
