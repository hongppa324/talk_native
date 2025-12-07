import { useRef, useState } from "react";
import { NativeModules, Platform, ToastAndroid } from "react-native";
import { hooks } from "@/core/hooks/index";
import { RootState } from "@/store";
import { OFFICE_SVR_ID, OFFICE_SYSTEM_TP, URL } from "@/config";
import * as FileSystem from "expo-file-system";
import { shareAsync } from "expo-sharing";
import { HttpHook } from "@/core/hooks/HttpHook";
import axios from "axios";
import RNBlobUtil from "react-native-blob-util";
import { useToast } from "@/core/components/common/ToastProvider";
import Share from "react-native-share";
import { utils } from "@/core/utils/index";
import { CameraRoll } from "@react-native-camera-roll/camera-roll";
import { useType } from "@/core/hooks/useType";
import mime from "react-native-mime-types";
import { useNavigation } from "@react-navigation/native";
import { useTranslation } from "react-i18next";

export const useFileHandler2 = () => {
  const chatAuthToken = hooks.useAppSelector((state: RootState) => state.userSlice.chatAuthToken);
  const authToken = hooks.useAppSelector((state: RootState) => state.userSlice.authToken);
  const mailAuthToken = hooks.useAppSelector((state: RootState) => state.userSlice.mailAuthToken);
  const user = hooks.useAppSelector((state: RootState) => state.userSlice.user);
  const roomId = hooks.useAppSelector((state: RootState) => state.talkSlice.roomId);
  const navigation = useNavigation();

  const { MyNativeModule, MyFilePicker } = NativeModules;
  const { _post } = HttpHook();
  const { openToast } = useToast();
  const { convertSafeFileName, strictEncodeFilename } = utils;
  const { fileTp } = useType();
  const { t } = useTranslation("common");

  const isIOS = Platform.OS === "ios";
  const isAndroid = Platform.OS === "android";
  const { dirs } = RNBlobUtil.fs;
  const androidPath = "/storage/emulated/0/Download/bizbee Works";
  const dirToSave = isIOS ? dirs.DocumentDir : androidPath;
  const downloadTaskRef = useRef(null);
  const uploadTaskRef = useRef(null);

  const [uploadProgress, setUploadProgress] = useState({ percent: 0, received: 0, total: 0 });
  const [isFileUploading, setIsFileUploading] = useState(false);
  

  const save = async (uri: string, filename: string, mimetype: string) => {
    console.log(uri, filename, mimetype);
    if (Platform.OS === "android") {
      const permissions = await FileSystem.StorageAccessFramework.requestDirectoryPermissionsAsync();
      if (permissions.granted) {
        const base64 = await FileSystem.readAsStringAsync(uri, { encoding: FileSystem.EncodingType.Base64 });
        await FileSystem.StorageAccessFramework.createFileAsync(permissions.directoryUri, filename, mimetype)
          .then(async (uri) => {
            await FileSystem.writeAsStringAsync(uri, base64, { encoding: FileSystem.EncodingType.Base64 });
            ToastAndroid.show(t("download.simpleDownloadSuccess"), 3);
          })
          .catch((e) => console.log(e));
      }
    } else {
      shareAsync(uri);
    }
  };

  const download = async (fileNo: string, fileNm: string, fileTy: string, type: string, mailDownload?: { partId: string; userEmail: string }, mode?: string, onProgress?: (percent: number, received?: number, total?: number) => void) => {
    const fileName = fileNm;
    let uri;
    try {
      if (type === "talk") {
        uri = `${URL.chatApi}/file/mobileDownload?fileNo=${fileNo}&token=${chatAuthToken}`;
      } else if (type === "community") {
        uri = `${URL.base}/file/mobileDownload?fileNo=${fileNo}&token=${authToken}`;
      } else if (type === "docbox") {
        uri = `${URL.base}/docBox/mobileDownload?url=${fileNo}&fileNm=${fileNm}&fileTy=${fileTy}&fileTy&token=${authToken}`;
      } else if (type === "mail") {
        uri = `${URL.mailApi}/msgDownload?id=${fileNo}&partId=${mailDownload.partId}&userEmail=${mailDownload.userEmail}&fileName=${fileName}&zToken=${mailAuthToken}`;
      }

      const result = await onFileDownload(uri, fileName, fileTy, type, () => openToast(t("download.simpleDownloadSuccess"), { icon: "success" }), fileNo, mode, onProgress);
      return result;
    } catch (error) {
      console.log("download error:", error);
    }
  };

  const getFileType = async (path: string) => {
    const mimeType = mime.lookup(path);
    return mimeType || null;
  };

  const checkDir = async (path: string) => {
    if (Platform.OS === "android") {
      try {
        RNBlobUtil.fs.exists(path).then(async (exists) => {
          if (!exists) {
            await RNBlobUtil.fs.mkdir(path);
          }
        });
      } catch (error) {
        console.log("checkDir error:", error);
      }
    }
  };

  const onFileDownload = async (uri: string, fileName: string, fileType: string, type: string, callback: () => void, fileNo?: string, mode?: string, onProgress?: (percent: number, received?: number, total?: number) => void) => {
    const detailPath = () => {
      const match = uri.match(/^(https?:\/\/[^/]+)/);
      if (match) {
        switch (match[0]) {
          case URL.base:
            return "works";
          case URL.chatApi:
            return "talk";
          case URL.mailApi:
            return "mail";
          default:
            return "default";
        }
      } else {
        return "default";
      }
    };
    const path = `${dirToSave}/${detailPath()}/${fileNo}`;
    await checkDir(path);

    const extension = fileName.split(".").pop();
    const safeFileName = convertSafeFileName(fileName);
    const finalPath = `${path}/${safeFileName}`;

    if (mode === "view") {
      // 미리보기
      try {
        // 여기서 문서 뷰어 호출
        const baseUrl = URL.base;
        const talkUrl = URL.chatApi;
        const WOPISrc = type === "talk" ? `${talkUrl}/wopi/files/${fileNo}?access_token=${chatAuthToken}` : `${baseUrl}/wopi/files/${fileNo}?access_token=${authToken}`;
        const encodedWOPISrc = encodeURIComponent(WOPISrc);
        // 엑셀, 워드, ppt, pdf, txt, 기타 파일 경로
        const viewerUrl = `https://coolwsd.bizbee.co.kr/browser/dist/cool.html?WOPISrc=${encodedWOPISrc}`;

        // 한글 파일은 뷰어에서 안 열리긴 함.
        if (fileTp(fileName) === "excel" || fileTp(fileName) === "word" || fileTp(fileName) === "ppt" || fileTp(fileName) === "pdf" || fileTp(fileName) === "hwp" || fileTp(fileName) === "txt" || fileTp(fileName) === "zip" || fileTp(fileName) === "etc") {
          navigation.navigate("FileViewer", { url: viewerUrl });
        }
        return false;
      } catch (e) {
        const message = e.message.includes("No app installed") ? t("download.noApplication") : t("download.openFileError");
        openToast(message, { icon: "alert" });
        return false;
      }
    } else {
      // 저장하기
      if (await checkFile(finalPath)) {
        if (isAndroid) {
          try {
            const mimeType = await getFileType(finalPath);
            await RNBlobUtil.android.actionViewIntent(finalPath, mimeType);
          } catch (e) {
            const message = e.message.includes("No app installed") ? t("download.noApplication") : t("download.openFileError");
            openToast(message, { icon: "alert" });
          }
        } else {
          RNBlobUtil.ios.openDocument(finalPath);
        }
        return true;
      }

      // openToast(t("download.downloading"));
      try {
        let config;
        if (isIOS && (fileTp(fileName) === "img" || fileTp(fileName) === "video")) {
          config = {
            fileCache: false,
            appendExt: extension,
          };
        } else {
          config = {
            fileCache: true,
            path: finalPath,
            overwrite: false,
            addAndroidDownloads: {
              useDownloadManager: true,
              notification: true,
              mediaScannable: true,
              title: safeFileName,
              description: "Downloading file",
              path: finalPath,
            },
          };
        }

        const task = RNBlobUtil.config(config).fetch("GET", uri);
        downloadTaskRef.current = task;

        if (onProgress) {
          task.progress((received, total) => {
            if (!total || Number(total) <=0) return;
            const r = Number(received)
            const t = Number(total)
            // console.log("received", received, "total", total);
            const percent = Math.floor((r / t) * 100);
            onProgress(percent, r, t);
          });
        }

        const response = await task;
        downloadTaskRef.current = null;

        if (isIOS) {
          if (!response.path() || !response.data) {
            console.log("Invalid response path or data", response.path(), response.data);
            openToast(t("download.simpleDownloadFail"), { icon: "alert" });
            return;
          }

          if (fileTp(fileName) === "img" || fileTp(fileName) === "video") {
            await CameraRoll.saveAsset(response.path());
          } else {
            RNBlobUtil.ios.openDocument(response.path());
          }

          openToast(t("download.simpleDownloadSuccess"), { icon: "success" });
          await RNBlobUtil.fs.unlink(`${dirs.DocumentDir}/ReactNativeBlobUtil_tmp`);
        } else {
          callback && callback();
        }

        onProgress && onProgress(100, 0, 0);
        return true;
      } catch (error) {
        console.log("onFileDownload ERROR", error);
        downloadTaskRef.current = null;

        const name = String(error?.name || "");
        const message = String(error?.message || "");

        const RNBlobNm = "ReactNativeBlobUtilCanceledFetch";
        const cancelMsg = "canceled";
        const downloadManagerCancelMsg = "Download manager download failed, the file does not downloaded to destination."
        
        const userCanceled = name === RNBlobNm && message === cancelMsg || message.includes(downloadManagerCancelMsg);

        if (userCanceled) {
          onProgress && onProgress(0, 0, 0);
          return false;
        }

        openToast(t("download.simpleDownloadFail"));
        onProgress && onProgress(0, 0, 0);
        return false;
      }
    }
  };

  const cancelDownload = () => {
    const task = downloadTaskRef.current;
    // console.log("useFileHandler2 cancelDownload task", task);
    if (task && typeof task.cancel === "function") {
      try {
        task.cancel();
      } catch (e) {
        console.log("cancelDownload error", e);
      }
    }
    downloadTaskRef.current = null;
  };

  const getDownloadUrl = async (file: DocBox, projectId: string) => {
    const response = await _post("/docBox/getMobileDownLoadLink", {
      multiSelectFiles: [file],
      corpCd: user?.corpCd,
      projectId: projectId,
    });

    download(response.data, file.fileNm, "docbox", file.fileTy);
  };

  const downloadFileOffice = async (fileNo: string, fileName: string) => {
    const params = {
      systemTp: OFFICE_SYSTEM_TP,
      serverId: OFFICE_SVR_ID,
      fileNo,
    };
    const url = `${URL.officeApi}/file/downloadNotice`;
    const isIOS = Platform.OS === "ios";
    const isAndroid = Platform.OS === "android";
    const { dirs } = RNBlobUtil.fs;
    const androidPath = "/storage/emulated/0/Download/bizbee Works";
    const dirToSave = isIOS ? dirs.DocumentDir : androidPath;
    await checkDir(androidPath);
    const path = `${dirToSave}/${fileName}`;

    openToast(t("download.downloading"));

    const response = await axios.post(url, params, {
      responseType: "blob",
    });

    RNBlobUtil.base64.encode(response.data.data);

    // Blob 데이터를 base64로 변환합니다.
    const reader = new FileReader();
    reader.readAsDataURL(response.data);
    reader.onloadend = async () => {
      const base64data = (reader.result as string)?.split(",")[1]; // base64 데이터 추출

      if (base64data) {
        await RNBlobUtil.fs.writeFile(path, response.data, "base64");

        if (Platform.OS === "android") {
          RNBlobUtil.android.addCompleteDownload({
            title: fileName,
            description: "Downloading file",
            mime: response.headers["content-type"] || "application/octet-stream",
            path: path,
            showNotification: true,
          });
        } else {
          RNBlobUtil.ios.openDocument(path);
        }

        openToast(t("download.simpleDownloadSuccess"), { icon: "success" });
      } else {
        openToast(t("download.simpleDownloadFail"), { icon: "alert" });
      }
    };
  };

  const pickFile = async (o?: { limit?: number }) => {
    const type = {
      selectFile: t("file.selectFile"),
      selectMessage: t("file.selectMessage"),
      selectImage: t("file.selectImage"),
      cancel: t("cancel"),
      limit: o?.limit ? o?.limit : null,
    };
    try {
      if (Platform.OS === "android") {
        return await MyNativeModule?.openFileChooser(t("file.placeHolder"));
      } else {
        return await MyFilePicker?.openFileChooser(type);
      }
    } catch (error) {
      console.log(error);
    }
  };

  const uploadFile = async (fileList: { name: string; size: string; url: string; type?: string }[], msgTargetId?: string[]) => {
    // console.log("useFileHandler2 fileList", fileList);
    let frm = new FormData();

    if (fileList.length > 0) {
      for (let i = 0; i < fileList.length; i++) {
        //@ts-ignore
        frm.append(`fileList[${i}].file`, { uri: fileList[i].url, type: fileList[i].type, name: strictEncodeFilename(fileList[i].name) });
        frm.append(`fileList[${i}].fileNm`, fileList[i].name);
        frm.append("fileList[" + i + "].fileSize", utils.transformFileSize(parseInt(fileList[i].size)) as string);
      }
      frm.append("corpCd", user?.corpCd as string);
      frm.append("userId", user?.userId as string);
      frm.append("fileDiv", "room");

      if (msgTargetId) {
        // 단체문자 파일 업로드시 relKey1 변경
        for (let key of msgTargetId) {
          frm.append("relKeys", key);
        }
      } else {
        frm.append("relKey1", roomId);
      }
      frm.append("osTp", Platform.OS === "android" ? "06" : "07");
      frm.append("osTxt", Platform.OS === "android" ? "Android OS" : "Apple iOS");
      frm.append("deviceId", "mobile");
      frm.append("deviceTp", "02");
    }
    // console.log("useFileHandler2 uploadFile 최종 frm", frm);

    const source = axios.CancelToken.source();
    uploadTaskRef.current = source;

    try {
      const response = await axios.post(`${URL.chatApi}/file/upload`, frm, {
        headers: {
          "Content-Type": "multipart/form-data",
          Authorization: `Bearer ${chatAuthToken}`,
          Accept: "application/json",
        },
        cancelToken: source.token,
        onUploadProgress: (e) => {
          if (!e.total || e.total <= 0) return;
          const loaded = e.loaded ?? 0;
          const total = e.total ?? 0;
          const percent = Math.floor((loaded / total) * 100);
          // console.log("upload progress", loaded, total, percent);
          setUploadProgress({ percent, received: loaded, total });
        },
        transformRequest: [(data) => data],
      });

      uploadTaskRef.current = null;
      setUploadProgress({ percent: 100, received: 0, total: 0 });
      return response.data;
    } catch (error) {
      console.log("upload error", error);
      const userCanceled = axios.isCancel(error);
      uploadTaskRef.current = null;

      if (userCanceled) {
        setUploadProgress({ percent: 0, received: 0, total: 0 });
        return null;
      }
    }
  };

  const cancelUpload = () => {
    const task = uploadTaskRef.current;
    // console.log("useFileHandler2 cancelUpload task", task);
    if (task && typeof task.cancel === "function") {
      try {
        task.cancel();
      } catch (e) {
        console.log("upload cancel error", e);
      }
    }
    uploadTaskRef.current = null;
    setIsFileUploading(false);
    setUploadProgress({ percent: 0, received: 0, total: 0 });
  };

  const shareFile = async (uri: string, fileNm: string, fileTy: string) => {
    // console.log("useFileHandler2 shareFile 파일 공유 시작", uri, fileNm, fileTy);
    const safeFileName = convertSafeFileName(fileNm);
    try {
      const downLoadFilePath = `${RNBlobUtil.fs.dirs.CacheDir}/${safeFileName}`;
      const tmpRes = await RNBlobUtil.config({ fileCache: true, path: downLoadFilePath, indicator: true }).fetch("GET", uri);
      const filePath = tmpRes.path();

      const open = await Share.open({
        url: `file://${downLoadFilePath}`,
        title: safeFileName,
        type: fileTy,
      });
    } catch (e) {
      console.log("파일 공유 중 에러 발생", e);
    }
  };

  const checkFile = async (path: string) => {
    try {
      const exists = await RNBlobUtil.fs.exists(path);
      return exists;
    } catch (error) {
      console.log("checkFile error", error);
    }
  };

  const createTextFile = async (titleTxt: string, contentsTxt: string) => {
    let index = 1;

    let path = `${dirToSave}/${titleTxt}.txt`;
    while (await checkFile(path)) {
      path = `${dirToSave}/${titleTxt} (${index}).txt`;
      index++;
    }

    try {
      RNBlobUtil.fs.writeFile(path, utils.removeTag(contentsTxt), "utf8");
    } catch (error) {
      throw new Error("createTextFile error");
    }
  };

  const shareFiles = async (files: { fileNm: string; uri: string; fileTy: string }[]) => {
    // console.log("useFileHandler2 shareFiles files", files);
    try {
      const filePaths = await Promise.all(files.map(async (file, index) => {
        const safeFileName = convertSafeFileName(file.fileNm);
        const downLoadFilePath = `${RNBlobUtil.fs.dirs.CacheDir}/${safeFileName}`;
        const tmpRes = await RNBlobUtil.config({ fileCache: true, path: downLoadFilePath, indicator: true }).fetch("GET", file.uri);
        const filePath = tmpRes.path();
        return `file://${filePath}`;
      }));
      const result = await Share.open({
        urls: filePaths,
      });
    } catch (e) {
      console.log("shareFiles 파일 공유 중 에러 발생", e);
    }
  };

  const shareLink = async (url: string) => {
    try {
      const open = await Share.open({
        message: url,
        // subject : "링크 공유"
      });
    } catch (e) {
      console.log("shareLink 링크 공유 중 에러 발생", e);
    }
  };

  return {
    download,
    uploadFile,
    cancelUpload,
    getDownloadUrl,
    save,
    pickFile,
    downloadFileOffice,
    onFileDownload,
    cancelDownload,
    shareFile,
    createTextFile,
    checkDir,
    shareFiles,
    shareLink,
    uploadProgress,
    setUploadProgress,
    isFileUploading,
    setIsFileUploading,
  };
};