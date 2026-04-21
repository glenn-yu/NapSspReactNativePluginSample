const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');
const path = require('path');

const root = path.resolve(__dirname, '..');

/**
 * Metro configuration
 * https://reactnative.dev/docs/metro
 *
 * @type {import('metrobundler').ConfigT}
 */
// Packages that must have exactly one instance in the bundle.
// When the plugin (root/) imports these, Metro follows the symlink to root/
// and finds root/node_modules/react-native (0.72.0), creating a second instance
// that has a separate ReactNativeViewConfigRegistry Map → ViewConfig errors.
const SINGLETON_PACKAGES = new Set(['react', 'react-native']);

const config = {
  watchFolders: [root],
  resolver: {
    nodeModulesPaths: [
      path.resolve(__dirname, 'node_modules'),
      path.resolve(root, 'node_modules'),
    ],
    resolveRequest: (context, moduleName, platform) => {
      const topLevel = moduleName.split('/')[0];
      if (SINGLETON_PACKAGES.has(topLevel)) {
        // Force resolution to start from integration-test-app/ so that
        // integration-test-app/node_modules/react-native is always found first.
        return context.resolveRequest(
          { ...context, originModulePath: path.resolve(__dirname, 'index.js') },
          moduleName,
          platform,
        );
      }
      return context.resolveRequest(context, moduleName, platform);
    },
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
