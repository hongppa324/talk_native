import React, { forwardRef, memo } from "react";
import { requireNativeComponent, type ViewProps } from "react-native";
import { ChatFile, Message } from "@/core/types/TalkProps";
import { useLinkClick } from "@/core/hooks/useLinkClick";

export type PressTarget = "Profile" | "Message" | "ReplyMessage" | "UnreadBadge" | "Link" | "File" | "Emoticon" | "Video" | "Share" | "Reaction" | "Comment" | "Mention" | "Like" | "Cancel" | "Room" | "RoomProfile";
export type SelectMode = "SaveOne" | "SaveSelected" | "SaveThis" | "SaveAll" | "Save" | "Share" | "Delete";

type InvalidNativeEvent = { errors?: string[] };
type PressNativeEvent = {
  message: Message;
  element: PressTarget;
  reactionTp?: string;
};
type PressViewerEvent = {
  mode: SelectMode;
  images: ChatFile[];
  index: number;
}

type NativeProps = ViewProps & {
  // ChatRoomViewManager ReactProp
  roomId?: string; // @ReactProp(name = "roomId")
  userJson?: string; // @ReactProp(name = "userJson")
  messagesJson?: string; // @ReactProp(name = "messagesJson")
  userListJson?: string; // @ReactProp(name = "userListJson")
  unUsedUserListJson?: string; // @ReactProp(name = "unUsedUserListJson")
  talkThemeJson?: string; // @ReactProp(name = "talkThemeJson")
  i18n?: Record<string, string>; // @ReactProp(name = "i18n")
  isFetchingNextPage?: boolean; // @ReactProp(name = "isFetchingNextPage")
  scrollToBottom?: boolean; // @ReactProp(name = "scrollToBottom")
  scrollToTalkId?: string; // @ReactProp(name = "scrollToTalkId")
  scrollSeq?: number; // @ReactProp(name = "scrollSeq")
  highlightQuery?: string; // @ReactProp(name = "highlightQuery")
  videoTalkId?: string; // @ReactProp(name = "videoTalkId")
  isDownloading?: boolean; // @ReactProp(name = "isDownloading")
  downloadPercent?: number; // @ReactProp(name = "downloadPercent")
  downloadReceived?: number; // @ReactProp(name = "downloadReceived")
  downloadTotal?: number; // @ReactProp(name = "downloadTotal")
  // Native에서 RN에 전달하는 이벤트 (getExportedCustomDirectEventTypeConstants에 등록된 이벤트)
  onMessagesInvalid?: (e: { nativeEvent: InvalidNativeEvent }) => void;
  onPress?: (e: { nativeEvent: PressNativeEvent }) => void;
  onMessageLongPress?: (e: { nativeEvent: PressNativeEvent }) => void;
  onReachTop?: (e: { nativeEvent: {} }) => void;
  onPressViewerButton?: (e: { nativeEvent: PressViewerEvent }) => void;
};

const NativeChatRoomView = requireNativeComponent<NativeProps>("ChatRoomView");

export type ComposeChatRoomViewProps = ViewProps & {
  roomId?: string;
  userJson?: string;
  messagesJson?: string;
  userListJson?: string;
  unUsedUserListJson?: string;
  talkThemeJson?: string;
  i18n?: Record<string, string>;
  isFetchingNextPage?: boolean;
  scrollToBottom?: boolean;
  scrollToTalkId?: string;
  isScrolling?: boolean;
  scrollSeq?: number;
  highlightQuery?: string;
  videoTalkId?: string;
  isDownloading?: boolean;
  downloadPercent?: number;
  downloadReceived?: number;
  downloadTotal?: number;
  onMessagesInvalid?: (errors: string[]) => void;
  onPress?: (message: Message, target: PressTarget, reactionTp?: string) => void;
  onMessageLongPress?: (message: Message, target: PressTarget) => void;
  onReachTop?: () => void;
  onPressViewerButton?: (mode: SelectMode, images: ChatFile[], index?: number) => void; 
};

const ComposeChatRoomView = forwardRef<any, ComposeChatRoomViewProps>(function ComposeChatRoomView(
  { onMessagesInvalid, onPress, onMessageLongPress, onReachTop, onPressViewerButton, style, ...rest },
  ref,
) {
  const { clickLink } = useLinkClick();

  return (
    <NativeChatRoomView
      ref={ref}
      {...rest}
      style={style ?? { flex: 1 }}
      onMessagesInvalid={(e) => onMessagesInvalid?.(e.nativeEvent.errors ?? [])}
      onPress={(e) => {
        const { message, element, reactionTp } = e.nativeEvent;
        // console.log("ComposeChatRoomView element", element, "message", message);
        if (element === "Message") {
          onPress?.(message, element);
        } else if (element === "ReplyMessage") {
          onPress?.(message, element);
        } else if (element === "Link") {
          const url = message?.ln?.url;
          console.log("ComposeChatRoomView url", url);
          if (url) clickLink(url);
          return;
        } else if (element === "UnreadBadge") {
          onPress?.(message, element);
        } else if (element === "Profile") {
          onPress?.(message, element);
        } else if (element === "File") {
          onPress?.(message, element);
        } else if (element === "Video") {
          onPress?.(message, element);
        } else if (element === "Share") {
          onPress?.(message, element);
        } else if (element === "Reaction") {
          onPress?.(message, element, reactionTp);
        } else if (element === "Comment" || element === "Mention" || element === "Like") {
          onPress?.(message, element);
        } else if (element === "Cancel") {
          console.log("ComposeChatRoomView element", element, "message", message);
          onPress?.(message, element);
        }
      }}
      onMessageLongPress={(e) => {
        const { message, element } = e.nativeEvent;
        onMessageLongPress?.(message, element);
      }}
      onReachTop={() => onReachTop?.()} 
      onPressViewerButton={(e) => {
        const { mode, images, index } = e.nativeEvent;
        onPressViewerButton?.(mode, images, index);
      }}
    />
  );
});

export default memo(ComposeChatRoomView);