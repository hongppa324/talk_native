import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  Platform,
  StatusBar,
  Pressable,
  ViewStyle,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

type Props = {
  title: string;
  subtitle?: string;
  onBack?: () => void;                  // ← 뒤로가기 버튼 표시/동작
  right?: React.ReactNode;              // ← 우측 액션(아이콘/버튼 등)
  style?: ViewStyle;                    // ← 추가 스타일
  translucentStatusBar?: boolean;       // 기본 true (iOS는 자동)
};

export default function TopAppHeader({
  title,
  subtitle,
  onBack,
  right,
  style,
  translucentStatusBar = true,
}: Props) {
  const insets = useSafeAreaInsets();
  const topPad = translucentStatusBar ? insets.top : 0;

  return (
    <View style={[styles.wrapper, { paddingTop: topPad }, style]}>
      {/* Android 상태바 설정 */}
      {Platform.OS === 'android' && (
        <StatusBar
          backgroundColor="#ffffff"
          barStyle="dark-content"
          translucent={translucentStatusBar}
        />
      )}

      <View style={styles.bar}>
        {/* 왼쪽: Back */}
        <View style={styles.left}>
          {onBack ? (
            <Pressable onPress={onBack} style={styles.backBtn} android_ripple={{ color: '#eaeaea', borderless: true }}>
              {/* 아이콘 라이브러리 없이 텍스트 화살표 대체 */}
              <Text style={styles.backTxt}>‹</Text>
            </Pressable>
          ) : null}
        </View>

        {/* 가운데: 타이틀/서브타이틀 */}
        <View style={styles.center} pointerEvents="none">
          <Text style={styles.title} numberOfLines={1}>{title}</Text>
          {!!subtitle && (
            <Text style={styles.subtitle} numberOfLines={1}>{subtitle}</Text>
          )}
        </View>

        {/* 오른쪽: 액션 슬롯 */}
        <View style={styles.right}>
          {right}
        </View>
      </View>
    </View>
  );
}

const HEIGHT = 56;

const styles = StyleSheet.create({
  wrapper: {
    backgroundColor: '#fff',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#e6e6e6',
    // iOS 그림자
    shadowColor: '#000',
    shadowOpacity: 0.07,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 3 },
    // Android 엘리베이션
    elevation: 3,
  },
  bar: {
    height: HEIGHT,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
  },
  left: {
    width: 44,
    justifyContent: 'center',
    alignItems: 'flex-start',
  },
  backBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  backTxt: {
    fontSize: 28,
    marginTop: -2,
    color: '#222',
  },
  center: {
    flex: 1,
    alignItems: 'center',
  },
  title: {
    fontSize: 17,
    fontWeight: '700',
    color: '#111',
  },
  subtitle: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  right: {
    width: 44,
    alignItems: 'flex-end',
    justifyContent: 'center',
  },
});
