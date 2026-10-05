import test from 'node:test';
import assert from 'node:assert/strict';
import sharp from 'sharp';
import {normalizeProfilePhoto} from './profile-images.js';
test('photos are bounded JPEGs with metadata removed', async () => {
  const source = await sharp({create: {width: 1200, height: 900, channels: 3, background: '#44729b'}}).withMetadata().jpeg().toBuffer();
  for (const [scope, width, height] of [['user', 320, 320], ['classroom', 640, 400]]) {
    const result = await normalizeProfilePhoto(source, scope);
    const metadata = await sharp(result).metadata();
    assert.equal(metadata.width, width); assert.equal(metadata.height, height);
    assert.equal(metadata.format, 'jpeg'); assert.equal(metadata.exif, undefined);
    assert.ok(result.length < 200000);
  }
});
test('invalid bytes and SVG are rejected', async () => {
  await assert.rejects(() => normalizeProfilePhoto(Buffer.from('not an image'), 'user'));
  await assert.rejects(() => normalizeProfilePhoto(Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="50" height="50"></svg>'), 'user'));
});
