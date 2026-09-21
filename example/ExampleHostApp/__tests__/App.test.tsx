/**
 * @format
 *
 * Renders the reference app without a native bridge. `NapSspAd.initialize()` is expected to reject
 * here — the point of the test is that the app surfaces that failure instead of crashing.
 */
import React from 'react';
import ReactTestRenderer from 'react-test-renderer';
import App from '../App';

jest.spyOn(console, 'log').mockImplementation(() => {});

test('renders and reports the missing native module instead of crashing', async () => {
  let tree: ReactTestRenderer.ReactTestRenderer | undefined;

  await ReactTestRenderer.act(async () => {
    tree = ReactTestRenderer.create(<App />);
  });

  // Let the initialize() rejection settle.
  await ReactTestRenderer.act(async () => {
    await Promise.resolve();
  });

  const text = JSON.stringify(tree!.toJSON());
  expect(text).toContain('react-native-nap-ssp');
  expect(text).toContain('initialize() failed');

  await ReactTestRenderer.act(async () => {
    tree!.unmount();
  });
});
