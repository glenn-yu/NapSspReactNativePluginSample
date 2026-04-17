const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');

/**
 * Metro configuration
 * https://facebook.github.io/metro/docs/configuration
 *
 * @type {import('metro-config').MetroConfig}
 */
const path = require('path');

const config = {
  projectRoot: __dirname,
  watchFolders: [path.resolve(__dirname, '..', '..')],
  resolver: {
    // Allow importing from the package root `src` directory
    extraNodeModules: new Proxy({}, {
      get: (target, name) => path.join(__dirname, '..', '..', 'node_modules', name),
    }),
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
