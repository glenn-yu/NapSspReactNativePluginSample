jest.mock('react-native/Libraries/BatchedBridge/NativeModules', () => ({
  __fbBatchedBridgeConfig: {}, 
  UIManager: { RCTView: {}, setJSResponder: jest.fn() },
  PlatformConstants: { osVersion: '13.0', platform: 'iOS' },
}));

jest.mock('react-native/Libraries/StyleSheet/StyleSheet', () => ({
  create: jest.fn(),
}));

jest.mock('react-native/Libraries/TurboModule/TurboModuleRegistry', () => ({
  get: jest.fn(),
  getEnforcing: jest.fn(),
}));

jest.mock('react-native/Libraries/ReactNative/AppRegistry', () => ({
  registerComponent: jest.fn(),
  getApplication: jest.fn(),
}));

jest.mock('react-native/Libraries/Renderer/shims/ReactNative', () => ({
  render: jest.fn(),
}));

jest.mock('react-native', () => {
  const rn = jest.requireActual('react-native');
  return {
    ...rn,
    NativeModules: {
      ...rn.NativeModules,
      DevSettings: { addMenuItem: jest.fn() },
    },
  };
});