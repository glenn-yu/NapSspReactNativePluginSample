export interface NapSspConfig {
  mediaKey: string;
  adUnitIds: string[];
  mediations?: any;
  logLevel?: string;
  coppa?: boolean;
}

class NapSspAd {
  static async initialize(_config: NapSspConfig): Promise<void> {
    // Placeholder: native bridge initialization should be implemented per-platform
    return Promise.resolve();
  }
  static setLogLevel(_level: string) {}
  static setCoppa(_enabled: boolean) {}
}

export default NapSspAd;
