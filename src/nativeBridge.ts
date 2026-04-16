// Lightweight mock bridge to call native modules when available
import { NativeModules } from 'react-native';

export const NativeNapSsp = NativeModules.NapSspModule || {};
export const NativeInterstitial = NativeModules.NapSspInterstitial || {};
export const NativeBannerView = NativeModules.NapSspBannerView || {};
