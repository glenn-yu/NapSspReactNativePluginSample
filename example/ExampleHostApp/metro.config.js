const path = require('path');
const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');

/**
 * Metro configuration
 * https://reactnative.dev/docs/metro
 *
 * The plugin is linked from the repository root (`file:../..`), so it lands in node_modules as a
 * symlink pointing outside the project. Three settings make that work, and none of them is needed
 * by an app that installs the package from npm:
 *
 *  1. `watchFolders` — Metro only resolves files inside the project root.
 *  2. `blockList` — the repository root has its own `node_modules` holding React and React Native
 *     as dev dependencies. Without blocking it, the plugin's `require('react')` resolves to that
 *     second copy and every hook fails with "Cannot read property 'useRef' of null".
 *  3. `nodeModulesPaths` — with the repository's `node_modules` blocked, the plugin's own imports
 *     (React, React Native and the `@babel/runtime` helpers Metro injects) resolve here instead.
 *
 * @type {import('@react-native/metro-config').MetroConfig}
 */
const packageRoot = path.resolve(__dirname, '../..');
const appModules = path.resolve(__dirname, 'node_modules');

function escapeForRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

const config = {
  watchFolders: [packageRoot],
  resolver: {
    blockList: [
      new RegExp(`^${escapeForRegExp(path.join(packageRoot, 'node_modules'))}\\b.*$`),
    ],
    nodeModulesPaths: [appModules],
    extraNodeModules: {
      react: path.join(appModules, 'react'),
      'react-native': path.join(appModules, 'react-native'),
      '@babel/runtime': path.join(appModules, '@babel/runtime'),
    },
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
