import { NativeInterstitial } from './nativeBridge';

export class InterstitialAd {
  adUnitId: string;
  private _loaded = false;
  constructor(adUnitId: string) { this.adUnitId = adUnitId; }
  async load(): Promise<void> { 
    if (NativeInterstitial.load) {
      await NativeInterstitial.load(this.adUnitId);
      this._loaded = true;
    } else {
      this._loaded = true; // fallback
    }
  }
  async show(): Promise<void> { 
    if (!this._loaded) throw new Error('not loaded');
    if (NativeInterstitial.show) {
      await NativeInterstitial.show();
    }
  }
  isLoaded(): boolean { return this._loaded; }
  addAdEventListener(_event: string, _handler: Function) { return () => {} }
  destroy() { this._loaded = false }
}
