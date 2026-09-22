const test = require('node:test');
const assert = require('node:assert/strict');
const {after, stamp, readKey, unreadCount, latestRead} = require('./public/web-parity.js');

test('read markers preserve exact Firestore nanoseconds', () => {
  const read = {seconds: 100, nanoseconds: 500};
  assert.equal(after({seconds:100,nanoseconds:501},read),true);
  assert.equal(after(read,read),false);
  assert.deepEqual(stamp({seconds:100,nanoseconds:501}),{seconds:100,nanoseconds:501});
});
test('unread count excludes own messages, read messages, and pending timestamps', () => {
  const read = {seconds:100,nanoseconds:500};
  const messages = [
    {senderId:'other',timestamp:read},
    {senderId:'other',timestamp:{seconds:100,nanoseconds:501}},
    {senderId:'me',timestamp:{seconds:101,nanoseconds:0}},
    {senderId:'other',timestamp:null}
  ];
  assert.equal(unreadCount(messages,'me',read),1);
});
test('viewing chat clears displayed messages without clearing newer arrivals', () => {
  const old = {seconds:100,nanoseconds:0};
  const viewed = {classId:'room',senderId:'other',timestamp:{seconds:101,nanoseconds:0}};
  const newer = {...viewed,timestamp:{seconds:101,nanoseconds:1}};
  const read = latestRead([viewed],'room',old);
  assert.equal(unreadCount([viewed],'me',read),0);
  assert.equal(unreadCount([viewed,newer],'me',read),1);
});
test('read marker ignores other classrooms and never moves backwards', () => {
  const read = {seconds:100,nanoseconds:0};
  assert.deepEqual(latestRead([{classId:'other',timestamp:{seconds:200}}, {classId:'room',timestamp:{seconds:10}}],'room',read),read);
});
test('read marker storage is isolated by account and classroom', () => {
  assert.notEqual(readKey('a','room'),readKey('b','room'));
  assert.notEqual(readKey('a','room'),readKey('a','other'));
  assert.notEqual(readKey('a:b','c'),readKey('a','b:c'));
});
