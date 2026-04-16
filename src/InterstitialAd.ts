export class InterstitialAd {
  adUnitId: string;
  private _loaded = false;
  constructor(adUnitId: string) { this.adUnitId = adUnitId; }
  async load(): Promise<void> { this._loaded = true; }
  async show(): Promise<void> { if (!this._loaded) throw new Error('not loaded'); }
  isLoaded(): boolean { return this._loaded; }
  addAdEventListener(_event: string, _handler: Function) { return () => {} }
  destroy() { this._loaded = false }
}
