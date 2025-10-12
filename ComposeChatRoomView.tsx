import React, { forwardRef, memo } from 'react';
import { requireNativeComponent, type ViewProps } from 'react-native';

export type MsgTapTarget = 'Avatar' | 'Content';

export type ChatMessagePayload = {
  id: string;
  sender: string;
  text: string;
  time: string;
  date: string;
  displayName?: string | null;
  type: string;
  avatarUrl?: string | null;

  mediaUrl?: string | null;
  mediaUrls?: string[] | null;
  thumbnailUrl?: string | null;
  fileSize?: string | null;
  mediaDuration?: string | null;

  totalRecipients?: number;
  readCount?: number;
  reactions?: { emoji: string; count: number }[];
};

type BackNativeEvent = {};
type InvalidNativeEvent = { errors?: string[] };
type MsgTapNativeEvent = {
  element: MsgTapTarget;
  message: ChatMessagePayload;
};

type NativeProps = ViewProps & {
  roomId?: string;
  messagesJson?: string;
  onBack?: (e: { nativeEvent: BackNativeEvent }) => void;
  onMessagesInvalid?: (e: { nativeEvent: InvalidNativeEvent }) => void;
  onMessageClick?: (e: { nativeEvent: MsgTapNativeEvent }) => void;
  onMessageLongPress?: (e: { nativeEvent: MsgTapNativeEvent }) => void;
};

const NativeIMChatRoomView = requireNativeComponent<NativeProps>('IMChatRoomView');

export type ComposeChatRoomViewProps = ViewProps & {
  roomId?: string;
  messagesJson?: string;
  onBack?: () => void;
  onMessagesInvalid?: (errors: string[]) => void;

  onMessageClick?: (message: ChatMessagePayload, target: MsgTapTarget) => void;
  onMessageLongPress?: (message: ChatMessagePayload, target: MsgTapTarget) => void;
};

const ComposeChatRoomView = forwardRef<any, ComposeChatRoomViewProps>(
  function ComposeChatRoomView(
    { onBack, onMessagesInvalid, onMessageClick, onMessageLongPress, style, ...rest },
    ref
  ) {
    return (
      <NativeIMChatRoomView
        ref={ref}
        {...rest}
        style={style ?? { flex: 1 }}
        onBack={() => onBack?.()}
        onMessagesInvalid={(e) => onMessagesInvalid?.(e.nativeEvent.errors ?? [])}
        onMessageClick={(e) => {
          const { message, element } = e.nativeEvent;
          onMessageClick?.(message, element);
        }}
        onMessageLongPress={(e) => {
          const { message, element } = e.nativeEvent;
          onMessageLongPress?.(message, element);
        }}
      />
    );
  }
);

export default memo(ComposeChatRoomView);
