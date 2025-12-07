import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Alert, AppState, Dimensions, Keyboard, Platform, Pressable, ScrollView, StyleSheet, TouchableOpacity, View } from "react-native";
import { InfiniteData, useQueryClient } from "@tanstack/react-query";
import { hooks } from "@/core/hooks";
import { RootState } from "@/store";
import { URL } from "@/config";
import { registerCallback, syncRegisterCallback, syncUnregisterCallback, unregisterCallback } from "@/core/hooks/useWebSocket";
import { TalkModalScreenProps, TalkTabParamList } from "@/core/types/RootStackParamList";
import { Medium, SemiBold, Span } from "@/core/components/StyledText";
import { BottomSheetBody, buttonStyles, DpBetween, DpFlex, DpFlexTouchable, Line, SpanGray, ModalButton, DpBetweenTouchable } from "@/assets/styles";
import styled from "styled-components/native";
import TalkWriteArea from "@/core/components/talk/TalkWriteArea";
import { useModal } from "@/core/hooks/useModal";
import RoomContextMenu from "@/core/components/talk/RoomContextMenu";
import { useBottomSheet } from "@/core/hooks/useBottomSheet";
import TalkBottomSheet from "@/core/components/talk/TalkBottomSheet";
import { BottomSheetModal, BottomSheetView } from "@gorhom/bottom-sheet";
import { useTalkList } from "@/core/hooks/useTalkList";
import { ChatFile, ChatMember, Emoticon, Message } from "@/core/types/TalkProps";
import { CloseGray } from "@/constants/Icon";
import FileIcon from "@/core/components/common/FileIcon";
import TrashDeleteGray from "@/assets/images/icon/trash-gray.svg";
import { useFileHandler } from "@/core/hooks/useFileHandler";
import { useFileHandler2 } from "@/core/hooks/useFileHandler2";
import { HttpHook } from "@/core/hooks/HttpHook";
import { useDispatch } from "react-redux";
import { actions } from "@/store/actions";
import { useTalkSearchState } from "@/screens/modal/TalkModal/TalkSearchProvider";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { cancelNotification } from "@/core/utils/notifeeUtils";
import { roomKey } from "@/core/hooks/query/talk/roomKey";
import { useInfiniteMessages } from "@/core/hooks/useInfiniteMessages";
import { useMessageMutate } from "@/core/hooks/query/talk/mutate/useMessageMutate";
import { useTalkMessage } from "@/core/hooks/useTalkMessage";
import { useToast } from "@/core/components/common/ToastProvider";
import { useRoomInfoQuery, useRoomUserQuery } from "@/core/hooks/query/talk/useRoomInfoQuery";
import { useType } from "@/core/hooks/useType";
import InputCommon from "@/core/components/common/InputCommon";
import RadioCommon from "@/core/components/common/RadioCommon";
import CommonBottomSheet from "@/core/components/common/CommonBottomSheet";
import { useCommunityContents } from "@/core/hooks/useCommunityContents";
import { colors } from "@/constants/colors";
import TalkEmojiModal from "@/core/components/talk/TalkEmojiModal";
import { EmojiXIcon } from "@/constants/TalkIcon";
import TalkEmojiImage from "@/core/components/talk/TalkEmojiImage";
import ImageViewerSelectView from "@/core/components/image-viewer/ImageViewerSelectView";
import { ImageViewerProps } from "@/core/types/ImageViewerType";
import useAllImageFilesMutate from "@/core/hooks/query/talk/mutate/useAllImageFilesMutate";
import { useRoomUpdateMutate } from "@/core/hooks/query/talk/mutate/useRoomUpdateMutate";
import { insertMessagesToDatabase, updateDbTalk, updateCommentMentionCnt, updateCommentList } from "db/query/messageQuery";
import { useConfirmModal } from "@/core/components/common/ConfirmProvider";
import i18n, { t } from "i18next";
import { buildNativeI18n } from "@/i18n/talkNativeI18n";
import { talk, TalkInsertModel, TalkSendStatusEnum } from "db/schema";
import { nanoid } from "@reduxjs/toolkit";
import { eq } from "drizzle-orm";
import { startTempMessageTimeout } from "@/core/utils/talkTempMessageTimer";
import TalkNoticeArea from "@/core/components/talk/TalkNoticeArea";
import { BodyType } from "@/core/hooks/useSyncSocket";
import { updateDownloadYn } from "db/query/messageQuery";
import { CustomKeyboardAvoidingView } from "@/custom/CustomKeyboardAvoidingView";
import ComposeChatRoomView from "@/core/components/talk/ComposeChatRoomView";
import ReadBottomSheet from "@/core/components/talk/ReadBottomSheet";
import dayjs from "dayjs";
import useFileArrHandler, { FileArr } from "@/core/hooks/useFileArrHandler";
import useMessageShare from "@/core/hooks/useMessageShare";
import useTalkTheme from "@/core/hooks/useTalkTheme";
import { useWebSocket } from "@/core/hooks/useWebSocket";

const WINDOW_HEIGHT = Dimensions.get("window").height;
const BOTTOM_SHEET_HEIGHT = WINDOW_HEIGHT * 0.7;

const TalkContainer = styled.View`
  flex: 1;
`;
const ModalBody = styled.View`
  overflow: hidden;
  width: 280px;
  max-height: 440px;
  border-radius: 12px;
  background-color: #fff;
`;
const FileModalBody = styled(ModalBody)`
  padding: 20px;
  gap: 20px;
`;

type FileInfo = {
  name: string;
  size: string;
  url: string;
  type?: string;
};

const { checkSvg } = useType();

const FileSendItem = ({ fileList, deleteFile }: { fileList: FileInfo[]; deleteFile: (file: FileInfo) => void }) => {
  const fileSizing = (fileSize: string) => {
    let sizeName = "";
    let size = Number(fileSize);
    if (size < 1024) {
      sizeName = "Bytes";
    } else if (size < 1024 * 1024) {
      size = Math.round(size / 1024);
      sizeName = "KB";
    } else if (size < 1024 * 1024 * 1024) {
      size = Math.round(size / (1024 * 1024));
      sizeName = "MB";
    } else {
      size = Math.round(size / (1024 * 1024 * 1024));
      sizeName = "GB";
    }
    return size + sizeName;
  };

  return (
    <ScrollView style={{ maxHeight: 440 }} showsVerticalScrollIndicator={true}>
      {fileList.map((file, index) => (
        <DpFlex gap={8} key={index} style={{ paddingVertical: 8 }}>
          <FileIcon size={32} fileNm={file.name} uri={file.url} />
          <View style={{ flex: 1, gap: 2 }}>
            <Medium size={12}>{file.name}</Medium>
            <SpanGray size={10}>{fileSizing(file.size)}</SpanGray>
          </View>
          <TouchableOpacity onPress={() => deleteFile(file)}>
            <TrashDeleteGray />
          </TouchableOpacity>
        </DpFlex>
      ))}
    </ScrollView>
  );
};

export interface TalkLikeHandlerProps {
  handleLikeSheetOpen: (newRouteName: keyof TalkTabParamList, message: Message) => void;
}

export default function TalkNativeContent({ navigation, route, isScrolling, setIsScrolling }: TalkModalScreenProps<"TalkNativeContent"> & { isScrolling: boolean; setIsScrolling: React.Dispatch<React.SetStateAction<boolean>> }) {
  const { roomId, writeContent, groupFileList } = route.params;
  const { top, bottom } = useSafeAreaInsets();
  const dispatch = useDispatch();
  const queryClient = useQueryClient();

  const isLogin = hooks.useAppSelector((state: RootState) => state.userSlice.isLogin);
  const user = hooks.useAppSelector((state: RootState) => state.userSlice.user);
  const chatWebSocket = hooks.useAppSelector((state) => state.stompSlice.chatWebSocket);

  const { data: roomInfo } = useRoomInfoQuery(roomId);
  const { data: roomUserInfo, isLoading: userListLoading } = useRoomUserQuery(roomId);

  const userList = useMemo(() => {
    if (!roomUserInfo) return [];
    return roomUserInfo;
  }, [roomUserInfo]);

  const noticeList = useMemo(() => {
    if (!roomInfo) return [];
    return roomInfo.noticeList;
  }, [roomInfo]);

  const { data, hasNextPage, isLoading, isFetchingNextPage, fetchNextPage, refetch } = useInfiniteMessages(roomId);
  const { setNewMessageMutate, setMessageFindMutate, setSearchMessageMutate, updateReadTimeMutate } = useMessageMutate();
  const { updateRoomNoticeYnMutateFn } = useRoomUpdateMutate();
  const { sendComment, sendFile, sendNoteNotice, sendReply, removedMessage, sendMessage, enterRoom, readMessage, sendEditMessage, sendFileMessage } = useWebSocket();
  const { searchMessageStack, searchIdList, searching, searchTxt, searchIndex, rownumMapRef } = useTalkSearchState();
  const { getUpdatedMessage } = useTalkList(roomId);
  const { uploadFile } = useFileHandler();
  const { download, shareFile, shareFiles, cancelDownload } = useFileHandler2();
  const { _post } = HttpHook();
  const { EmogiMutation, deleteMessage, searchMessageLoad, selectEmoticon, editMessage } = useTalkMessage();
  const { downloadArrAndProgress } = useFileArrHandler();
  const { onMessageShare } = useMessageShare();
  const { openToast } = useToast();
  const { fileTp } = useType();
  const { insertReport } = useCommunityContents();
  const { showModal } = useConfirmModal();
  const { talkColor } = useTalkTheme();

  const [readMark, setReadMark] = useState<boolean>(true);
  const [readT, setReadT] = useState<number>(0);
  const [isSizeUpdate, setIsSizeUpdate] = useState(true);
  const [isFileLoading, setIsFileLoading] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState({ percent: 0, received: 0, total: 0 });
  const [isDownloading, setIsDownloading] = useState(false);

  const allMessages = useMemo(() => {
    if (!data) return [];
    return data;
  }, [data]);

  // 안 읽은 뱃지 클릭 이벤트 관련
  const activeUser = useMemo(() => {
    if (!userList) return [];
    return userList.filter((u) => {
      if (u.statFg.toString() === "1") return u;
    });
  }, [userList]);

  const inactiveUser = useMemo(() => {
    if (!userList) return [];
    return userList.filter((u) => {
      if (u.statFg.toString() === "0") return u;
    });
  }, [userList]);

  const [targetMessage, setTargetMessage] = useState<Message | null>(null); // LongPress의 targetMessage
  const [readTargetMessage, setReadTargetMessage] = useState<Message | null>(null); // UnreadBadge의 targetMessage
  const [fileTargetMessage, setFileTargetMessage] = useState<Message | null>(null); // File의 targetMessage
  const [videoTargetMessage, setVideoTargetMessage] = useState<Message | null>(null); // Video의 targetMessage

  const unreadUser = useMemo(() => {
    if (!activeUser || !readTargetMessage) return [];
    return activeUser.filter((u: ChatMember) => dayjs(readTargetMessage?.sendDtm).toDate() > dayjs(u.lastReadDtm).toDate());
  }, [activeUser, readTargetMessage]);

  const readUser = useMemo(() => {
    if (!activeUser || !readTargetMessage) return [];
    return activeUser.filter((u: ChatMember) => dayjs(readTargetMessage?.sendDtm).toDate() <= dayjs(u.lastReadDtm).toDate());
  }, [activeUser, readTargetMessage]);

  useEffect(() => {
    if (!readTargetMessage) return;
    handleUnreadBottomSheet();
  }, [readTargetMessage]);

  const failMessages = useMemo(() => allMessages.filter((m) => m?.status === "fail"), [allMessages]);
  const pendingMessages = useMemo(() => allMessages.filter((m) => m?.status === "pending"), [allMessages]);
  const otherMessages = useMemo(() => allMessages.filter((m) => m?.status !== "fail" && m?.status !== "pending"), [allMessages]);
  const messageData = useMemo(() => [...failMessages, ...pendingMessages, ...otherMessages], [failMessages, pendingMessages, otherMessages]);
  console.log("TalkNativeContent messageData", messageData);

  // Native에 전달하는 Json prop
  const messagesJson = useMemo(() => JSON.stringify(messageData), [messageData]);
  const userJson = useMemo(() => JSON.stringify(user), [user]);
  const userListJson = useMemo(() => JSON.stringify(activeUser), [activeUser]);
  const unUsedUserListJson = useMemo(() => JSON.stringify(inactiveUser), [inactiveUser]);
  const talkThemeJson = useMemo(() => JSON.stringify(talkColor), [talkColor]);
  const nativeI18n = useMemo(() => buildNativeI18n(), [i18n.language]);

  const [lastMessage, setLastMessage] = useState<Message>(null);
  const [noticeStyle, setNoticeStyle] = useState<"icon" | "short" | "long">("icon");
  const [noticeSearch, setNoticeSearch] = useState(false);
  const [replySearch, setReplySearch] = useState(false);
  const replyId = useRef<string>("");
  const noticeId = useRef<string>("");
  const [scrollToBottom, setScrollToBottom] = useState(false);
  const [nativeScrollToId, setNativeScrollToId] = useState<string | null>(null);

  const convertNativeImage = (images: ChatFile[]) => {
    const converted = images?.filter((f) => (f.fileTy || "").toLowerCase().startsWith("image/"))
      .map((f) => ({
        uri: `${URL.chatApi}/file/img?fileNo=${f.fileNo}`,
        svg: checkSvg(f.fileNm),
        downloadSource: {
          fileNm: f.fileNm,
          fileNo: f.fileNo,
          fileTy: f.fileTy,
          fileSize: f.fileSize,
          type: "talk",
          groupId: f.talkId,
          isAdmin: false,
        },
      }));

    return converted;
  };

  const [images, setImages] = useState<ImageViewerProps[]>([]);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  const groupImages = useMemo(() => {
    // console.log("groupImages images", images, "currentImageIndex", currentImageIndex);
    return images.filter((image) => {
      return image?.downloadSource?.groupId === images[currentImageIndex]?.downloadSource?.groupId;
    });
  }, [images, currentImageIndex]);

  const [selectImageModalVisible, setSelectImageModalVisible] = useState(false);
  const [selectImageModalMode, setSelectImageModalMode] = useState<"save" | "delete">("save");

  const handleSelectImageModal = useCallback((mode: "save" | "delete") => {
    setSelectImageModalMode(mode);
    setSelectImageModalVisible(true);
  }, []);

  const allFilesDownload = useCallback(async (fileArr: FileArr[]) => {
    // console.log("allFilesDownload fileArr", fileArr);
    try {
      await downloadArrAndProgress(fileArr);
    } catch (error) {
      console.log("묶음사진 전체 다운로드 실패", error);
    }
  }, []);

  const nowImageDownload = (images: ImageViewerProps[]) => {
    const file = {
      fileNo: images[0].downloadSource?.fileNo,
      fileNm: images[0].downloadSource?.fileNm,
      type: images[0].downloadSource?.type,
      fileTy: images[0].downloadSource?.fileTy,
    };
    try {
      allFilesDownload([file]);
    } catch (error) {
      console.log("현재 사진 전체 다운로드 실패", error);
    }
  };

  const allImagesDownload = useCallback((images: ImageViewerProps[]) => {
    const downloadArr: FileArr[] = images.map((image, index) => ({
      fileNo: image.downloadSource?.fileNo,
      fileNm: image.downloadSource?.fileNm,
      type: image.downloadSource?.type,
      fileTy: image.downloadSource?.fileTy,
    }));
    try {
      allFilesDownload(downloadArr);
    } catch (error) {
      console.log("묶음사진 전체 다운로드 실패", error);
    }
  }, [groupImages]);

  const isNullCheck = useCallback((image: ImageViewerProps[]) => {
    console.log("isNullCheck image", image);
    if (!image) {
      showModal(t("viewer.imageNull", { ns: "common" }), { onlyConfirm: true });
      return true;
    }
    return false;
  }, []);

  const shareImage = async (image: ImageViewerProps[]) => {
    // console.log("shareImage image", image);
    if (isNullCheck(image)) return;
    const { fileNm, fileTy } = image[0].downloadSource;
    const uri = image[0].uri;

    try {
      await shareFile(uri, fileNm, fileTy);
    } catch (e) {
      console.log("파일 공유 중 에러 발생", e);
    }
  };

  const handleviewImagesDelete = useCallback((deleteImages: ImageViewerProps[]) => {
    const filtered = images.filter((img) => !deleteImages.includes(img));
    const deletedGroupId = deleteImages[0]?.downloadSource?.groupId;

    const stillHasGroup = filtered.some((img) => img.downloadSource?.groupId === deletedGroupId);

    if (filtered.length === 0) {
      setImages([]);
      setCurrentImageIndex(0);
      setSelectImageModalVisible(false);
      return;
    }

    setImages(filtered);

    if (stillHasGroup) {
      const firstIdxInGroup = filtered.findIndex((img) => img.downloadSource?.groupId === deletedGroupId);
      setCurrentImageIndex(Math.max(0, firstIdxInGroup));
    } else {
      const newIndex = Math.min(currentImageIndex - deleteImages.length, filtered.length - 1);
      setCurrentImageIndex(Math.max(0, newIndex));
    }
  }, [images, currentImageIndex]);

  const onImageDelete = useCallback(async (deleteImages: ImageViewerProps[]) => {
    const message = queryClient.getQueryData<InfiniteData<Message[]>>(roomKey.roomList(roomId));
    const messageList = message.pages.flat();
    const targetMessage = messageList.find((message) => message.talkId === deleteImages[0].downloadSource.groupId);

    if (targetMessage) {
      const findImageList = targetMessage.fileList.filter((file) => deleteImages.some((image) => file.fileNo === image.downloadSource.fileNo));

      if (deleteImages.length === groupImages.length) {
        // 전체 선택해서 삭제할 때
        deleteMessage(targetMessage);
        if (messageList[0].talkId === targetMessage.talkId) {
          removedMessage(user.corpCd, roomId, user.userId, targetMessage.talkId);
        }
      } else {
        // 일부만 선택해서 삭제할 때
        deleteArrImageMutation.mutate({ groupId: images[currentImageIndex].downloadSource?.groupId, fileList: findImageList });
        if (messageList[0].talkId === targetMessage.talkId) {
          const message = t("imageSendMessage", { ns: "talk", count: groupImages.length - findImageList.length });
          removedMessage(user.corpCd, roomId, user.userId, targetMessage.talkId, message, groupImages.length - findImageList.length);
        }
      }
      await queryClient.invalidateQueries({ queryKey: roomKey.imageList(roomId) });
      handleviewImagesDelete(deleteImages);
    }
  }, [roomId, user.corpCd, groupImages]);

  const handleSelectModalClose = () => {
    setSelectImageModalVisible(false);
  };

  const handleSizeUpdate = useCallback((isUpdate: boolean) => {
    setIsSizeUpdate(isUpdate);
  }, []);

  const newMessageControl = useCallback(() => {
    setNewMessage(false);
    handleSizeUpdate(false);
    setLastMessage(null);

    setScrollToBottom((prev) => !prev);
  }, []);

  const unReadMessageCount = async () => {
    // a - 안 읽은 메세지 수
    let data = await _post<number>(`${URL.chatApi}/chat/unReadMessageCount`, {
      roomId: roomId,
      corpCd: user?.corpCd,
      userId: user?.userId,
    });

    // b - 댓글 멘션이 있는 메세지 ~ 가장 최근 메세지 수
    let mention = await _post(`${URL.chatApi}/chat/unReadMentionCommentMessageCnt`, {
      roomId: roomId,
      corpCd: user?.corpCd,
      userId: user?.userId,
    });
    // a와 b 중 더 큰 값을 넣어줌
    setReadT(data.data > mention.data ? data.data : mention.data);
    return true;
  };

  const pressSend = async (messages: string, emoticon?: Emoticon, replyTo?: string) => {
    await sendMessageText(messages, emoticon, replyTo);
  };

  const handleLoadMore = useCallback(async () => {
    if (hasNextPage && !isFetchingNextPage) {
      await fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage]);

  const refreshImages = async () => {
    queryClient.invalidateQueries({ queryKey: roomKey.imageList(roomId) });
  };

  const [fileList, setFileList] = useState<FileInfo[]>([]);

  const setFile = (fileList: FileInfo[]) => {
    setFileList(fileList);
    if (fileList.length > 0) {
      openFileModal();
    }
  };

  const filePreUpload = async (fileList: { name: string; size: string; url: string; type?: string }[]) => {
    console.log("TalkNativeContent filePreUpload fileList", fileList);
    if (fileList.length > 30) {
      showModal(t("imageLimit", { ns: "talk" }), { onlyConfirm: true, icon: "warning" });
      return;
    }
    if (fileList.find((file) => parseInt(file.size) > 300000000)) {
      openToast(t("file.sizeLimitAlert", { ns: "common" }), { icon: "alert" });
      return;
    }
    setIsFileLoading(true);

    const data = await uploadFile(fileList);

    if (data && data.length > 0) {
      sendFile(user.corpCd, user.userId, user.userNm, data);
      if (data[0].fileTy.includes("image")) {
        sendFileMessage(
          user.corpCd,
          roomInfo.roomId,
          user.userId,
          user.userNm,
          data[0].talkId,
          data.length > 1 ? t("imageSendMessage", { ns: "talk", count: data.length }) : data[0].fileNm,
          data[0].fileNo,
          data[0].fileNm,
          data[0].fileTy,
          data[0].fileSize,
          data,
        );
      } else {
        for (const d of data) {
          sendFileMessage(user.corpCd, roomInfo.roomId, user.userId, user.userNm, d.talkId, d.fileNm, d.fileNo, d.fileNm, d.fileTy, d.fileSize, []);
        }
      }
    }
    newMessageControl();

    setFileList([]);
    setIsFileLoading(false);
  };

  const deleteFile = (file: FileInfo) => {
    const copyFileList = fileList.filter((f) => f !== file);
    setFileList(copyFileList);
    if (copyFileList.length === 0) {
      closeFileModal();
    }
  };

  const mentionUserList = useRef<string[]>([]);
  // TODO: 멘션 있을 경우 알림 기능 필요 > 톡은 멘션 알림 처리 안함
  function convertMentionsToWeb(messageTxt: string) {
    let result = "";
    const regex = /@\[(.*?)\]\((.*?)\)/g;
    // const linkRegex = /https?:\/\/[^\s]+/g;
    let lastIndex = 0;
    let cnt = 0;

    const matches = [...messageTxt.matchAll(regex)];
    // const linkMatches = [...messageTxt.matchAll(linkRegex)];

    let combinedMatches = [...matches].sort((a, b) => a.index! - b.index!);
    const message = messageTxt.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    combinedMatches.forEach((match) => {
      result += message.slice(lastIndex, match.index);
      if (match[0].match(regex)) {
        const userNm = match[1];
        const userId = match[2];
        mentionUserList.current.push(userId);
        result +=
          '<span class="mention" data-index="' +
          cnt +
          '" data-denotation-char="@" data-id="' +
          userId +
          '" data-value="' +
          userNm +
          '"><span contenteditable="false"><span class="ql-mention-denotation-char">@</span>' +
          userNm +
          "</span></span>";
        cnt++;
      }
      lastIndex = match.index! + match[0].length;
    });

    result += message.slice(lastIndex);
    return result;
  }

  async function sendMessageText(messageTxt: string, emoticon?: Emoticon, replyTo?: string) {
    // console.log("sendMessageText messageTxt >>>>", messageTxt, "emoticon >>>>", emoticon);
    newMessageControl();
    // 일반 / 댓글 구분
    if (isOpened.current === false) {
      // 일반 - 답장 여부 구분
      if (isReply) {
        sendReply(user.corpCd, roomInfo.roomId, user.userId, user.userNm, convertMentionsToWeb(messageTxt), targetMessage, mentionUserList.current, emoticon);
        setIsReply(false);
      } else if (isEdit) {
        if (targetMessage.messageTxt === messageTxt) return;
        editMessage({
          talkId: targetMessage.talkId,
          messageTxt: convertMentionsToWeb(messageTxt),
        });
        const isLastMsg = messageData[0]?.talkId === targetMessage.talkId;
        sendEditMessage(roomInfo.roomId, targetMessage.talkId, convertMentionsToWeb(messageTxt), isLastMsg);
        setIsEdit(false);
      } else {
        const tmpTalkId = `TEMP_${nanoid(10)}`;
        const insertTempMessage: TalkInsertModel = {
          talkId: tmpTalkId,
          corpCd: user.corpCd,
          roomId,
          sendDtm: new Date().toISOString(),
          messageTxt: messageTxt === "" && emoticon?.emoticonId ? emoticon?.emoticonId : convertMentionsToWeb(messageTxt),
          userId: user.userId,
          userNm: user.userNm,
          status: TalkSendStatusEnum.pending,
          replyId: null,
          emoticonId: emoticon?.emoticonId ?? null,
          emoticonFileNo: emoticon?.fileNo ?? null,
          viewYn: null,
          commentMentionCnt: 0,
          isEdited: false,
        };
        let ln = null;
        if (messageTxt.includes("https://") || messageTxt.includes("http://")) {
          let data = await _post(`${URL.chatApi}/link`, insertTempMessage);
          ln = data.data;
        }

        const newInsertMessage: Message = {
          ...insertTempMessage,
          jobNm: user.jobNm,
          iconUrl: user.iconUrl,
          deptNm: user.deptNm,
          fileNo: null,
          fileNm: null,
          fileTy: null,
          ln: ln,
          reMessage: null,
          fileSize: null,
        };

        // console.log("sendMessageText insertTempMessage >>>>", insertTempMessage);
        // console.log("sendMessageText newInsertMessage >>>>", newInsertMessage);
        setNewMessageMutate(newInsertMessage);
        await insertMessagesToDatabase([newInsertMessage], user.userId, roomId);
        sendMessage(user, roomId, convertMentionsToWeb(messageTxt), tmpTalkId, mentionUserList.current, emoticon);
        startTempMessageTimeout(tmpTalkId, async () => {
          const tempMessages = await updateDbTalk({
            talkId: tmpTalkId,
            status: TalkSendStatusEnum.fail,
          }, eq(talk.status, TalkSendStatusEnum.pending));

          for (let msg of tempMessages) {
            setMessageFindMutate({
              talkId: msg.talkId,
              roomId: msg.roomId,
              status: "fail",
            });
          }
          console.log("전송 타이머에서 실패후 업데이트 완료", tempMessages);
        });
      }
      if (emoticon?.emoticonId) {
        selectEmoticon();
      }
    } else {
      let parentCommentId = "";

      if (replyTo) {
        parentCommentId = replyTo;
      } else {
        const isParentComment = targetMessage?.commentList?.find((comment) => comment.parentCommentId === "" && comment.child.length > 0);
        parentCommentId = isParentComment ? "" : targetMessage?.commentList?.find((comment) => comment.parentCommentId === "" && comment.child.length > 0)?.commentId || null;
      }

      let message = await _post<Message>(`${URL.chatApi}/inputComment`, {
        userId: user?.userId,
        corpCd: user?.corpCd,
        messageTxt: convertMentionsToWeb(messageTxt),
        talkId: targetMessage?.talkId,
        roomId: roomId,
        mentionUserList: mentionUserList.current,
        parentCommentId,
      });
      // console.log("sendMessageText response :::: message", message, "parentCommentId", parentCommentId);
      setMessageFindMutate({
        talkId: message.data.talkId,
        roomId: message.data.roomId,
        commentList: message.data.commentList,
        commentMentionCnt: message.data.commentMentionCnt,
      });
      insertMessagesToDatabase([message.data], user?.userId, roomId);

      // 댓글
      sendComment(
        user?.userId as string,
        user?.corpCd as string,
        targetMessage?.talkId,
        roomId,
        user?.userNm as string,
        convertMentionsToWeb(messageTxt),
        mentionUserList.current,
        parentCommentId,
        message.data.commentList || [],
      );
    }
    mentionUserList.current = [];
  }

  // modal
  const { openModal, closeModal, Modal } = useModal();
  const { openModal: openFileModal, closeModal: closeFileModal, Modal: FileModal } = useModal();
  const { openModal: openFileOptionModal, closeModal: closeFileOptionModal, Modal: FileOptionModal } = useModal();
  const { openModal: openVideoOptionModal, closeModal: closeVideoOptionModal, Modal: VideoOptionModal } = useModal();
  const { bottomSheet, handleBottomSheet, renderBackdrop, handleBottomClose } = useBottomSheet();
  const { bottomSheet: reportBottomSheet, handleBottomSheet: handleReportBottomSheet, handleBottomClose: closeReportBottomSheet } = useBottomSheet();
  const { bottomSheet: unreadBottomSheet, handleBottomSheet: handleUnreadBottomSheet, handleBottomClose: closeUnreadBottomSheet, renderBackdrop: unreadRenderBackdrop } = useBottomSheet();
  const [isSelectCopy, setIsSelectCopy] = useState(false);

  const handleSelectImageModalVisible = useCallback((value: boolean) => {
    setSelectImageModalVisible(value);
  }, []);

  const setSelectedMessageDeleteImage = (newList: ChatFile[]) => {
    setTargetMessage((prev) => ({
      ...prev,
      fileList: newList,
    }));
  };

  const { deleteArrImageMutation } = useAllImageFilesMutate();

  const [likeSheetRoute, setLikeSheetRoute] = useState<keyof TalkTabParamList>("Like");
  const [isReply, setIsReply] = useState(false);
  const [newMessage, setNewMessage] = useState<boolean>(false);
  const [isKeyboardVisible, setKeyboardVisible] = useState(false);
  const [isEmojiVisible, setIsEmojiVisible] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState<Emoticon | null>(null);
  const [isEdit, setIsEdit] = useState(false);

  const handleEdit = () => {
    closeModal();
    setIsEdit(true);
  };

  const handleLongPress = useCallback((message: Message) => {
    setTargetMessage(message);
    openModal();
  }, []);

  const handleLikeSheetOpen = useCallback((newRouteName: keyof TalkTabParamList, message?: Message) => {
    setLikeSheetRoute(newRouteName);
    if (message) {
      setTargetMessage(message);
    }
    handleBottomSheet();
  }, []);

  const snapPoints = useMemo(() => [BOTTOM_SHEET_HEIGHT, "90%"], []);

  const clickReply = () => {
    setIsReply(true);
    closeModal();
  };

  const unReply = () => {
    setIsReply(false);
  };

  const sendNoticeMessage = () => {
    sendNoteNotice(user.corpCd, roomInfo.roomId, user.userNm);
  };

  const handleDelete = async () => {
    deleteMessage(targetMessage);
    closeModal();

    if (targetMessage && messageData[0]?.talkId === targetMessage.talkId) {
      removedMessage(user.corpCd, roomInfo.roomId, user.userId, targetMessage.talkId);
    }
  };

  const handleReport = async () => {
    handleReportBottomSheet();
    closeModal();
  };

  const [report, setReport] = useState({ reportType: "상업적/홍보성", reportCd: "001" });
  const [reportTxt, setReportTxt] = useState("");

  const handleSelectCopy = (message: Message) => {
    setIsSelectCopy(true);
    closeModal();
  };

  const loadingRef = useRef(false);
  const pendingMessagesRef = useRef<any[]>([]);

  useEffect(() => {
    loadingRef.current = userListLoading;
    if (!userListLoading) {
      pendingMessagesRef.current.forEach((body) => {
        roomFunction(body);
      });
      pendingMessagesRef.current = [];
    }
  }, [userListLoading]);

  // 구독 중 이벤트 수신 시 동작
  const roomFunction = async (body: any) => {
    // body.type === "message" && console.log("TalkNativeContent 룸 펑션 body", body);

    if (body.type === "out" && user?.userId == body.userId && user?.corpCd == body.corpCd) {
      //   back()
    }

    if (body.type === "read" && loadingRef.current) {
      pendingMessagesRef.current.push(body);
      return;
    }

    if (body.type === "message" || body.type === "notice" || body.type === "file") {
      if (body.roomId === roomId) {
        setReadMark(false);
        if (getAppState() === "active") {
          readMessage(user?.corpCd, user?.userId, roomId);
        }
        if (body.type === "notice") {
          queryClient.invalidateQueries({ queryKey: roomKey.roomInfo(roomId) });
        }
        if (body.messageTxt.includes("https://") || body.messageTxt.includes("http://")) {
          let data = await _post(`${URL.chatApi}/link`, body);
          body.ln = data.data;
        }
        if (body.userId !== user?.userId) body.downloadYn === "N";

        const message = body as Message;
        if (message.fileList && message.fileList.length > 0) refreshImages();
        if (!isSizeUpdate) {
          setLastMessage(message);
          setNewMessage(true);
        }
      }
    } else if (body.type === "delete") {
      refreshImages();
    } else if (body.type === "deleteFile") {
      refreshImages();
    } else if (body.type === "read") {
      // console.log("읽는 소켓 받음", body.userId);
      if (body.roomId === roomId) {
        updateReadTimeMutate(body.roomId, body.userId, body.readDtm);
      }
    } else if (body.type === "alim") {
      // console.log("알림 소켓 받음", body.userId);
      if (body.roomId === roomId) {
        updateRoomNoticeYnMutateFn(body.roomId, body.chatNoticeYn);
      }
    } else if (body.type === "connection") {
      // 접속 정보
      // console.log("roomFunction body.type === connection", body);
    } else if (body.type === "comment") {
      setMessageFindMutate({
        talkId: body.talkId,
        roomId: body.roomId,
        commentList: body.commentList,
      });
      updateCommentList({ commentList: body.commentList, talkId: body.talkId, corpCd: body.corpCd });
    } else if (body.type === "commentMention") {
      const target = queryClient
        .getQueryData<InfiniteData<Message[]>>(roomKey.roomList(roomId))
        ?.pages.flat()
        .find((m) => m.talkId === body.talkId);
      if (target) {
        const newCount = target.commentMentionCnt > 0 ? target.commentMentionCnt + 1 : 1;
        setMessageFindMutate({
          talkId: target.talkId,
          roomId: target.roomId,
          commentMentionCnt: newCount,
        });
        updateCommentMentionCnt(target.talkId, target.corpCd, newCount);
      }
    }
    // setNewMessage(false)
  };

  const changeSheetState = useCallback((index: number) => {
    console.log("changeSheetState =========", index);
    if (index === -1) isOpened.current = false;
    else isOpened.current = true;
  }, []);

  const isOpened = useRef(false);
  const [showNoti, setShowNoti] = useState<boolean>(false);

  const noticeOff = () => {
    setShowNoti(false);
  };

  useEffect(() => {
    if (roomInfo) {
      setShowNoti(roomInfo.showNotiYn === "Y");
    }
  }, [roomInfo]);

  const getAppState = () => {
    return AppState.currentState;
  };

  const syncReadFunction = async (body: BodyType) => {
    if (body.roomId !== roomId) return;
    if (body.type === "message" || body.type === "notice") {
      if (getAppState() === "active") {
        readMessage(user?.corpCd, user?.userId, roomId);
      }
    }
  };

  useEffect(() => {
    dispatch(actions.setRoomId(roomId));
    let subscription;
    if (isLogin) {
      pendingMessagesRef.current = [];
      // 쳇 커넥션 테스트용. 쳇 로그인 성공후 connect 추가 해야함.
      if (Platform.OS === "ios") {
        subscription = AppState.addEventListener("change", async (state) => {
          if (state === "active") {
            await unReadMessageCount();
            readMessage(user?.corpCd, user?.userId, roomId);
          }
        });
      }

      console.log(" TalkNativeContent chat con start ");
      registerCallback(roomFunction);
      syncRegisterCallback(syncReadFunction);
      console.log(" TalkNativeContent chat con end ");
    }

    return () => {
      newMessageControl();
      unregisterCallback(roomFunction);
      syncUnregisterCallback(syncReadFunction);
      dispatch(actions.setRoomId(null));
      if (subscription) {
        subscription.remove();
      }
    };
  }, [roomId]);

  useEffect(() => {
    console.log("TalkNativeContent chatWebSocket", chatWebSocket);
    if (isLogin && roomId && chatWebSocket && !isLoading) {
      console.log("chatWebSocket useEffect 실행");

      enterRoom(roomId);
      if (getAppState() === "active") {
        readMessage(user?.corpCd, user?.userId, roomId);
      }

      const set = () => {
        unReadMessageCount().then((res) => {
          if (res) {
            cancelNotification(roomId);
          }
        });
      };
      if (!searching) {
        set();
      }
      getUpdatedMessage();
      (async () => {
        console.log("pendingMessagesRef.current", pendingMessagesRef.current);
        pendingMessagesRef.current.forEach((body) => {
          roomFunction(body);
        });
        pendingMessagesRef.current = [];
      })();
    }
  }, [chatWebSocket, roomId, isLoading]);

  const focusTalkId = useMemo(() => {
    if (!searching || !searchIdList?.length) return null;
    const idx = Math.max(0, Math.min(searchMessageStack - 1, searchIdList.length - 1));
    return searchIdList[idx];
  }, [searching, searchIdList, searchMessageStack]);

  useEffect(() => {
    if (focusTalkId) setNativeScrollToId(focusTalkId);
  }, [focusTalkId]);

  useEffect(() => {
    if (!focusTalkId) {
      setIsScrolling(false);
      return;
    }
    if (messageData.some((m) => m.talkId === focusTalkId)) {
      setIsScrolling(false);
      return;
    }

    let cancelled = false;
    (async () => {
      setIsScrolling(true);

      try {
        // 계속 불러오되, 이미 로딩 중이면 기다렸다가 다음 루프에서 재확인됩니다.
        while (!cancelled && !messageData.some((m) => m.talkId === focusTalkId) && hasNextPage) {
          // 중복 호출 방지: 진행 중이면 살짝 대기
          if (isFetchingNextPage) {
            await new Promise((r) => setTimeout(r, 80));
            continue;
          }

          await fetchNextPage();
          // 다음 루프로 돌아가면 messageData가 갱신되어 재확인됩니다.
        }
      } catch (e) {
      } finally {
        if (!hasNextPage || cancelled) setIsScrolling(false);
      }
    })();

    return () => {
      cancelled = true;
      setIsScrolling(false);
    };
  }, [focusTalkId, messageData, hasNextPage, isFetchingNextPage, fetchNextPage, setIsScrolling]);

  const scrollToNoticeMessage = useCallback(async (talkId: string) => {
    setIsScrolling(true);

    const data = await _post(`${URL.chatApi}/replyMessageSelectCount`, {
      talkId: talkId,
      corpCd: user?.corpCd,
      roomId: roomId,
      userId: user?.userId,
    });
    noticeId.current = talkId;

    if (data) {
      const searchIndex = data.data[0].rownum;
      const response = await searchMessageLoad({ searchIndex: [searchIndex], afterIndex: 1, roomId });
      await setSearchMessageMutate(roomId, response);
    }
    setNoticeSearch(true);
    setNoticeStyle("short");
    setNativeScrollToId(talkId);
  }, [messageData]);

  const replyScroll = async (repId: string) => {
    setIsScrolling(true);
    replyId.current = repId;
    const data = await _post(`${URL.chatApi}/replyMessageSelectCount`, {
      talkId: repId,
      corpCd: user?.corpCd,
      roomId: roomId,
      userId: user?.userId,
    });
    console.log("TalkNativeContent replyScroll data", data);

    await replyMessageLoad(data.data[0].rownum);
    setReplySearch(true);
    setNativeScrollToId(repId);
    setIsScrolling(false);
  };

  const replyMessageLoad = async (searchIndex: number) => {
    const data = await searchMessageLoad({ searchIndex: [searchIndex], afterIndex: 1 });
    await setSearchMessageMutate(roomId, data);
  };

  const [alertMessage, setAlertMessage] = useState(false);

  const handleInsertReport = async () => {
    if (report.reportCd === "005" && reportTxt === "") {
      setAlertMessage(true);
      return;
    }
    Keyboard.dismiss();
    closeReportBottomSheet();

    const params = {
      relId: targetMessage.talkId,
      relTp: "05",
      insertUserId: targetMessage.userId,
      reasonTp: report.reportCd,
      reasonTxt: report.reportCd === "005" ? reportTxt : report.reportCd,
    };
    await insertReport(params);
    setAlertMessage(false);
    setReportTxt("");
  };

  const renderSelectedEmoji = () => {
    if (!selectedIndex) return null;

    const emoticon = selectedIndex;
    const bottomOffset = isEmojiVisible ? 332 : isKeyboardVisible ? 52 : 50;

    return (
      <View style={[styles.selectedEmojiContainer, { bottom: isReply ? bottomOffset + 44 : bottomOffset }]}>
        <DpFlex>
          <TalkEmojiImage emoticon={emoticon} />
          <TouchableOpacity onPress={() => setSelectedIndex(null)} style={{ position: "absolute", top: 0, right: -10 }} hitSlop={{ top: 10, right: 10, bottom: 10, left: 10 }}>
            <EmojiXIcon />
          </TouchableOpacity>
        </DpFlex>
      </View>
    );
  };

  const handleEmogiUpdate = async (message: Message, reactionTp: string) => {
    if (!user || !user?.userId) return;

    EmogiMutation.mutate({ talkId: message.talkId, likeIndex: reactionTp });
  };

  const downloadCheck = async (fileNo: string, fileNm: string, type: string, talkId: string, roomId: string, mode: string) => {
    try {
      setIsDownloading(true);
      setDownloadProgress({ percent: 0, received: 0, total: 0 });

      const fileTy = fileTp(fileNm);
      const result = await download(fileNo, fileNm, fileTy, type, { partId: "", userEmail: "" }, mode, (percent: number, received?: number, total?: number) =>
        setDownloadProgress({ percent, received, total }),
      );
      if (!result) return;

      const response = await _post(`${URL.chatApi}/file/downloadCheck`, {
        talkId: talkId,
        fileNo: fileNo,
        userId: user?.userId || "",
        corpCd: user?.corpCd || "",
        deviceId: "mobile",
        deviceTp: "02",
        osTp: Platform.OS === "android" ? "06" : "07",
        osTxt: Platform.OS === "android" ? "Android OS" : "Apple iOS",
      });
      console.log("downloadCheck response", response);
      if (response.status === 200) {
        await updateDownloadYn(talkId, "Y");
        await queryClient.cancelQueries({ queryKey: roomKey.roomList(roomId) });

        queryClient.setQueryData<InfiniteData<Message[]>>(roomKey.roomList(roomId), (oldData) => {
          if (oldData) {
            const updatedPages = oldData.pages.map((page) => {
              const newPages = page.map((message) => {
                if (message.talkId === talkId) {
                  return {
                    ...message,
                    downloadYn: "Y", // 파일 다운로드 여부 업데이트
                  };
                }
                return message;
              });
              return newPages;
            });
            return {
              ...oldData,
              pages: updatedPages,
            };
          }
        });
      }

      setDownloadProgress((prev) => ({
        ...prev,
        percent: 100,
      }));
    } catch (error) {
      console.error(error);
    } finally {
      setTimeout(() => {
        setIsDownloading(false);
        setDownloadProgress({ percent: 0, received: 0, total: 0 });
      }, 300);
    }
  };

  const handleCancelDownload = () => {
    console.log("handleCancelDownload 호출");
    cancelDownload();
    setIsDownloading(false);
    setDownloadProgress({ percent: 0, received: 0, total: 0 });
  };

  const handleSaveFile = () => {
    closeFileOptionModal();
    setTimeout(() => {
      downloadCheck(fileTargetMessage.fileNo, fileTargetMessage.fileNm, "talk", fileTargetMessage.talkId, fileTargetMessage.roomId, "save");
    }, 0);
  };

  const handleSaveVideo = () => {
    closeVideoOptionModal();
    setTimeout(() => {
      downloadCheck(videoTargetMessage.fileNo, videoTargetMessage.fileNm, "talk", videoTargetMessage.talkId, videoTargetMessage.roomId, "save");
    }, 0);
  }

  const handleOpenViewer = () => {
    downloadCheck(fileTargetMessage.fileNo, fileTargetMessage.fileNm, "talk", fileTargetMessage.talkId, fileTargetMessage.roomId, "view");
    closeFileOptionModal();
  };

  const handleOpenVideo = () => {
    downloadCheck(videoTargetMessage.fileNo, videoTargetMessage.fileNm, "talk", videoTargetMessage.talkId, videoTargetMessage.roomId, "view");
    closeVideoOptionModal();
  }

  const goProfile = useCallback((userId: string) => {
    closeUnreadBottomSheet();
    navigation.navigate("profile", { userId });
  }, [navigation]);

  const { height: SCREEN_HEIGHT } = Dimensions.get("window");

  const unreadSnapPoints = useMemo(() => {
    const DEFAULT = 15;
    const MIN = 35;
    const MAX = 80;
    const itemCount = userList ? userList.length : 0;
    const calculatedHeight = DEFAULT + (itemCount - 1) * 7;

    if (calculatedHeight >= MAX) {
      return [`${MAX}%`];
    } else if (calculatedHeight < MAX && calculatedHeight >= MIN) {
      return [`${calculatedHeight}%`];
    } else if (calculatedHeight < MIN) {
      return [`${MIN}%`]; // 2명 ~ 4명 있는 톡방까지 높이 같거나 비슷하고 그 후 늘어남
    }
  }, [userList]);

  const minHeight = useMemo(() => {
    const percent = parseInt(unreadSnapPoints[0]);
    return SCREEN_HEIGHT * (percent / 100);
  }, [unreadSnapPoints]);

  return (
    <CustomKeyboardAvoidingView>
      <TalkContainer>
        {/* 채팅방 공지사항 */}
        {roomInfo && noticeList?.length > 0 && showNoti && (
          <TalkNoticeArea roomInfo={roomInfo} noticeOff={noticeOff} scrollToMessage={scrollToNoticeMessage} noticeStyle={noticeStyle} setNoticeStyle={setNoticeStyle} />
        )}
        {/* TalkContent, MessageListArea */}
        <ComposeChatRoomView
          key={roomId}
          style={{ flex: 1 }}
          roomId={roomId}
          userJson={userJson}
          messagesJson={messagesJson}
          userListJson={userListJson}
          unUsedUserListJson={unUsedUserListJson}
          talkThemeJson={talkThemeJson}
          i18n={nativeI18n} // 번역 map
          isFetchingNextPage={isFetchingNextPage}
          scrollToBottom={scrollToBottom}
          scrollToTalkId={nativeScrollToId}
          isScrolling={isScrolling}
          scrollSeq={searchMessageStack}
          highlightQuery={searching ? (searchTxt ?? "") : ""}
          videoTalkId={videoTargetMessage?.talkId ?? null}
          isDownloading={isDownloading}
          downloadPercent={downloadProgress.percent}
          downloadReceived={downloadProgress.received}
          downloadTotal={downloadProgress.total}
          collapsable={false}
          onMessagesInvalid={(errors) => {
            if (!errors?.length) return;
            Alert.alert("메시지 포맷 오류", errors.join("\n"));
          }}
          onPress={(msg, target, reactionTp) => {
            // console.log("TalkNativeContent onPress target", target, "msg", msg);
            if (target === "Message") {
              Keyboard.dismiss();
            } else if (target === "ReplyMessage") {
              replyScroll(msg.replyId);
            } else if (target === "UnreadBadge") {
              setReadTargetMessage(msg);
            } else if (target === "Profile") {
              goProfile(msg.userId);
            } else if (target === "File") {
              setFileTargetMessage(msg);
              openFileOptionModal();
            } else if (target === "Video") {
              setVideoTargetMessage(msg);
              openVideoOptionModal();
            } else if (target === "Share") {
              onMessageShare(msg);
            } else if (target === "Reaction") {
              handleEmogiUpdate(msg, reactionTp);
            } else if (target === "Comment") {
              handleLikeSheetOpen(target, msg);
            } else if (target === "Mention") {
              handleLikeSheetOpen(target, msg);
            } else if (target === "Like") {
              handleLikeSheetOpen(target, msg);
            } else if (target === "Cancel") {
              handleCancelDownload();
            }
          }}
          onMessageLongPress={(msg, target) => {
            // console.log("TalkNativeContent onMessageLongPress target", target, "msg", msg);
            if (target === "Message") {
              handleLongPress(msg);
            }
          }}
          onReachTop={() => {
            handleLoadMore();
          }}
          onPressViewerButton={(mode, images, index) => {
            // console.log("TalkNativeContent onPressViewerButton mode", mode, "images", images, "index", index);
            const converted = convertNativeImage(images);
            // console.log("converted", converted);
            const selected = Array(converted[index]);

            setImages(converted);
            setCurrentImageIndex(index);

            if (mode === "SaveOne") {
              download(converted[0].downloadSource.fileNo, converted[0].downloadSource.fileNm, "talk", converted[0].downloadSource.fileTy);
            } else if (mode === "SaveSelected") {
              handleSelectImageModal("save");
            } else if (mode === "SaveThis") {
              nowImageDownload(selected);
            } else if (mode === "Save" || mode === "SaveAll") {
              allImagesDownload(converted);
            } else if (mode === "Share") {
              shareImage(selected);
            } else if (mode === "Delete") {
              handleSelectImageModal("delete");
            }
          }}
        />
        {/* 입력 및 send 버튼 */}
        <TalkWriteArea
          placeholder={t("placeholder", { ns: "talk" })}
          userList={userList}
          pressSend={pressSend}
          selectedMessage={targetMessage}
          isReply={isReply}
          unReply={unReply}
          setFile={setFile}
          newMessage={newMessage}
          lastMessage={lastMessage}
          newMessageControl={newMessageControl}
          isEmojiVisible={isEmojiVisible}
          setIsEmojiVisible={setIsEmojiVisible}
          isKeyboardVisible={isKeyboardVisible}
          setKeyboardVisible={setKeyboardVisible}
          selectedIndex={selectedIndex}
          setSelectedIndex={setSelectedIndex}
          isEdit={isEdit}
          setIsEdit={setIsEdit}
          path="TalkNativeContent"
        />
        {/* longPress Modal */}
        <Modal>
          <RoomContextMenu
            closeModal={closeModal}
            handleLikeSheetOpen={handleLikeSheetOpen}
            selectedMessage={targetMessage!}
            clickReply={clickReply}
            sendNotice={sendNoticeMessage}
            deleteMessage={handleDelete}
            handleReport={handleReport}
            handleSelectImageModalVisible={handleSelectImageModalVisible}
            handleSelectCopy={handleSelectCopy}
            handleEdit={handleEdit}
          />
        </Modal>
        <FileModal>
          <FileModalBody>
            <DpBetween>
              <Medium>{t("fileTransfer", { ns: "talk" })}</Medium>
              <Pressable onPress={closeFileModal}>
                <CloseGray />
              </Pressable>
            </DpBetween>
            <FileSendItem fileList={fileList} deleteFile={deleteFile} />
            <TouchableOpacity
              style={{ alignSelf: "center", justifyContent: "center", alignItems: "center", width: 100, height: 28, borderRadius: 4, backgroundColor: "#EEF1F6" }}
              onPress={() => {
                filePreUpload(fileList);
                closeFileModal();
              }}
            >
              <Span size={12}>{t("file.fileTransfer", { ns: "common", count: fileList.length })}</Span>
            </TouchableOpacity>
          </FileModalBody>
        </FileModal>
        {/* 선택 이미지 저장 모달 */}
        {selectImageModalVisible && (
          <ImageViewerSelectView
            groupImages={groupImages}
            visible={selectImageModalVisible}
            deleteImageMode={selectImageModalMode === "delete" && { onDelete: onImageDelete }}
            onClose={handleSelectModalClose}
            backgroundColor="#181818"
          />
        )}
        <CommonBottomSheet bottomSheetModalRef={reportBottomSheet} headerTitle={t("report", { ns: "talk" })}>
          <BottomSheetBody>
            <View style={{ gap: 32, paddingBottom: 20 }}>
              <DpBetweenTouchable
                gap={12}
                onPress={() => {
                  setReport({ reportType: "상업적/홍보성", reportCd: "001" });
                  setAlertMessage(false);
                }}
                activeOpacity={1}
              >
                <Medium size={14}>{t("reportType1", { ns: "talk" })}</Medium>
                <RadioCommon selected={report.reportType === "상업적/홍보성"} />
              </DpBetweenTouchable>
              <DpBetweenTouchable
                gap={12}
                onPress={() => {
                  setReport({ reportType: "음란/선정성", reportCd: "002" });
                  setAlertMessage(false);
                }}
                activeOpacity={1}
              >
                <Medium size={14}>{t("reportType2", { ns: "talk" })}</Medium>
                <RadioCommon selected={report.reportType === "음란/선정성"} />
              </DpBetweenTouchable>
              <DpBetweenTouchable
                gap={12}
                onPress={() => {
                  setReport({ reportType: "불법정보", reportCd: "003" });
                  setAlertMessage(false);
                }}
                activeOpacity={1}
              >
                <Medium size={14}>{t("reportType3", { ns: "talk" })}</Medium>
                <RadioCommon selected={report.reportType === "불법정보"} />
              </DpBetweenTouchable>
              <DpBetweenTouchable
                gap={12}
                onPress={() => {
                  setReport({ reportType: "욕설/인신공격", reportCd: "004" });
                  setAlertMessage(false);
                }}
                activeOpacity={1}
              >
                <Medium size={14}>{t("reportType4", { ns: "talk" })}</Medium>
                <RadioCommon selected={report.reportType === "욕설/인신공격"} />
              </DpBetweenTouchable>
              <DpBetweenTouchable
                gap={12}
                onPress={() => {
                  setReport({ reportType: "기타", reportCd: "005" });
                }}
                activeOpacity={1}
              >
                <Medium size={14}>{t("reportType5", { ns: "talk" })}</Medium>
                <RadioCommon selected={report.reportType === "기타"} />
              </DpBetweenTouchable>
            </View>
            <InputCommon
              placeholder={t("inputLimit", { ns: "talk" })}
              editable={report.reportType === "기타"}
              onChangeText={(text) => setReportTxt(text)}
              height={40}
              style={{ borderColor: alertMessage ? colors.Red500 : "#E1E5E8" }}
              maxLength={20}
            />
            {alertMessage && (
              <View style={{ paddingTop: 8 }}>
                <Span color={colors.Red500}>{t("reason", { ns: "talk" })}</Span>
              </View>
            )}
            <DpFlex style={{ paddingTop: 16 }}>
              <DpFlexTouchable style={[buttonStyles.button, buttonStyles.editButton]} onPress={handleInsertReport}>
                <SemiBold size={15} color="#fff">{t("insertReport", { ns: "talk" })}</SemiBold>
              </DpFlexTouchable>
            </DpFlex>
          </BottomSheetBody>
        </CommonBottomSheet>
        {/* 공감, 댓글, 멘션 바텀시트 */}
        <BottomSheetModal
          ref={bottomSheet}
          index={0}
          snapPoints={snapPoints}
          backdropComponent={(props) => renderBackdrop({ ...props, disappearsOnIndex: -1 })}
          enableDynamicSizing
          handleComponent={null}
          enableContentPanningGesture={false}
          onChange={changeSheetState}
          maxDynamicContentSize={BOTTOM_SHEET_HEIGHT}
        >
          <BottomSheetView style={{ minHeight: BOTTOM_SHEET_HEIGHT, paddingBottom: bottom }}>
            {messageData && targetMessage && (
              <CustomKeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : "height"} keyboardVerticalOffset={top + bottom + 8}>
                <TalkBottomSheet
                  routeName={likeSheetRoute}
                  userList={userList}
                  selectedMessage={targetMessage}
                  pressSend={pressSend}
                  messageData={messageData}
                  setIsEmojiVisible={setIsEmojiVisible}
                  isKeyboardVisible={isKeyboardVisible}
                  setKeyboardVisible={setKeyboardVisible}
                  goProfile={goProfile}
                />
              </CustomKeyboardAvoidingView>
            )}
          </BottomSheetView>
        </BottomSheetModal>
        {/* 안 읽은 사람 바텀시트 */}
        <BottomSheetModal
          ref={unreadBottomSheet}
          index={0}
          snapPoints={snapPoints}
          backdropComponent={(props) => unreadRenderBackdrop({ ...props, disappearsOnIndex: -1 })}
          enableDynamicSizing={false}
          handleIndicatorStyle={{
            backgroundColor: colors.CoolGray400,
          }}
          handleComponent={null}
          enableContentPanningGesture={false}
          bottomInset={Platform.OS === "android" ? bottom : 0}
        >
          <BottomSheetView style={{ paddingTop: 24, minHeight, paddingBottom: 24 }}>
            <ReadBottomSheet activeUser={activeUser} unreadUser={unreadUser} readUser={readUser} message={readTargetMessage} goProfile={goProfile} />
          </BottomSheetView>
        </BottomSheetModal>
      </TalkContainer>
      <FileOptionModal>
        <ModalBody style={{ width: 200 }}>
          <ModalButton height={42} onPress={handleSaveFile}>
            <DpFlex>
              <Span size={14} style={{ flex: 1, textAlign: "center" }}>{t("setting.save", { ns: "more" })}</Span>
            </DpFlex>
          </ModalButton>
          <Line />
          <ModalButton height={42} onPress={handleOpenViewer}>
            <DpFlex>
              <Span size={14} style={{ flex: 1, textAlign: "center" }}>{t("preview", { ns: "talk" })}</Span>
            </DpFlex>
          </ModalButton>
        </ModalBody>
      </FileOptionModal>
      <VideoOptionModal>
        <ModalBody style={{ width: 200 }}>
          <ModalButton height={42} onPress={handleSaveVideo}>
            <DpFlex>
              <Span size={14} style={{ flex: 1, textAlign: "center" }}>{t("setting.save", { ns: "more" })}</Span>
            </DpFlex>
          </ModalButton>
          <Line />
          <ModalButton height={42} onPress={handleOpenVideo}>
            <DpFlex>
              <Span size={14} style={{ flex: 1, textAlign: "center" }}>{t("preview", { ns: "talk" })}</Span>
            </DpFlex>
          </ModalButton>
        </ModalBody>
      </VideoOptionModal>
      <TalkEmojiModal isVisible={isEmojiVisible} setIsVisible={setIsEmojiVisible} setSelectedIndex={setSelectedIndex} />
      {selectedIndex !== null && renderSelectedEmoji()}
    </CustomKeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  selectedEmojiContainer: {
    width: 128,
    height: 96,
    paddingHorizontal: 24,
    paddingVertical: 8,
    borderRadius: 12,
    backgroundColor: "rgba(15, 27, 42, 0.60)",
    position: "absolute",
    alignSelf: "center",
    zIndex: 10,
    justifyContent: "center",
    alignItems: "center",
  },
});