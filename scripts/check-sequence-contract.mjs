// Development-only contract check. This is NOT a Mermaid parser.
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

export function validateSequence(model) {
  const errors = [];
  const fail = (message) => errors.push(message);
  const object = (value) => value !== null && typeof value === 'object' && !Array.isArray(value);
  const text = (value) => typeof value === 'string';
  const nonempty = (value) => text(value) && value.trim().length > 0;
  const shape = (value, keys, where) => {
    if (!object(value)) { fail(where + ': expected object'); return false; }
    for (const key of keys) if (!(key in value)) fail(where + ': missing ' + key);
    for (const key of Object.keys(value)) if (!keys.includes(key)) fail(where + ': unexpected ' + key);
    return true;
  };
  if (!shape(model, ['schemaVersion', 'kind', 'participants', 'events'], 'document')) return errors;
  if (model.schemaVersion !== 1 || model.kind !== 'sequence') fail('unsupported document version/kind');
  if (!Array.isArray(model.participants) || !Array.isArray(model.events)) {
    fail('participants/events must be arrays'); return errors;
  }
  const ids = new Set(), participants = new Set(), symbols = new Set();
  const addId = (id) => {
    if (!nonempty(id)) fail('invalid id');
    else if (ids.has(id)) fail('duplicate id: ' + id);
    else ids.add(id);
  };
  for (const p of model.participants) {
    if (!shape(p, ['id', 'symbol', 'label', 'kind'], 'participant')) continue;
    addId(p.id);
    participants.add(p.id);
    if (!nonempty(p.symbol) || symbols.has(p.symbol)) fail('invalid/duplicate symbol');
    symbols.add(p.symbol);
    if (!text(p.label) || !['participant', 'actor'].includes(p.kind)) fail('invalid participant');
  }
  const ref = (id) => { if (!participants.has(id)) fail('unknown participant: ' + id); };
  const frames = [], active = new Map();
  const snapshot = () => JSON.stringify([...active.entries()].filter(([, stack]) => stack.length).sort());
  const fields = {
    message: ['from', 'to', 'text', 'arrow'],
    activate: ['participant'], deactivate: ['participant'],
    altStart: ['text'], loopStart: ['text'],
    else: ['frame', 'text'], altEnd: ['frame'], loopEnd: ['frame']
  };
  for (const event of model.events) {
    if (!object(event)) { fail('event must be object'); continue; }
    const extra = Object.hasOwn(fields, event.kind) ? fields[event.kind] : null;
    if (!extra) { fail('unsupported event: ' + event.kind); continue; }
    shape(event, ['id', 'kind', ...extra], 'event');
    addId(event.id);
    switch (event.kind) {
      case 'message':
        ref(event.from); ref(event.to);
        if (!text(event.text) || !['->>', '-->>', '->', '-->', '-x', '--x', '-)', '--)'].includes(event.arrow))
          fail('invalid message text/arrow');
        break;
      case 'activate':
        ref(event.participant);
        if (!active.has(event.participant)) active.set(event.participant, []);
        active.get(event.participant).push(event.id);
        break;
      case 'deactivate': {
        ref(event.participant);
        const stack = active.get(event.participant);
        if (!stack?.length) fail('activation underflow');
        else stack.pop();
        break;
      }
      case 'altStart':
      case 'loopStart':
        if (!text(event.text)) fail('invalid frame text');
        frames.push({ id: event.id, kind: event.kind, activation: snapshot() });
        break;
      case 'else': {
        const frame = frames.at(-1);
        if (!frame || frame.kind !== 'altStart' || frame.id !== event.frame) fail('else outside matching alt');
        else if (snapshot() !== frame.activation) fail('activation must balance within each branch');
        if (!text(event.text)) fail('invalid branch text');
        break;
      }
      case 'altEnd':
      case 'loopEnd': {
        const frame = frames.at(-1);
        const startKind = event.kind === 'altEnd' ? 'altStart' : 'loopStart';
        if (!frame || frame.id !== event.frame || frame.kind !== startKind) fail('mismatched frame end');
        else {
          if (snapshot() !== frame.activation) fail('activation must balance within each frame');
          frames.pop();
        }
        break;
      }
    }
  }
  if (frames.length) fail('unclosed frame');
  if ([...active.values()].some(stack => stack.length)) fail('unclosed activation');
  return errors;
}

const fixture = (name) => JSON.parse(readFileSync(
  new URL('../examples/sequence/' + name + '.expected.json', import.meta.url), 'utf8'));
let checks = 0;
for (const name of ['basic', 'nested']) {
  const model = fixture(name);
  assert.deepEqual(validateSequence(model), [], name);
  assert.deepEqual(validateSequence(JSON.parse(JSON.stringify(model))), [], name + ' JSON round trip');
  checks += 2;
}
const rejects = [
  ['unknown reference', m => { m.events[0].to = 'missing'; }],
  ['duplicate id', m => { m.events[1].id = m.events[0].id; }],
  ['duplicate symbol', m => { m.participants[1].symbol = 'U'; }],
  ['missing field', m => { delete m.events[0].arrow; }],
  ['coordinate in semantics', m => { m.events[0].y = 100; }],
  ['unsupported event', m => { m.events[0].kind = 'parStart'; }],
  ['activation underflow', m => { m.events[1].kind = 'deactivate'; }],
  ['unclosed activation', m => { m.events.pop(); }],
  ['mismatched nested end', m => { m.events[5].frame = 'f-auth'; }],
  ['unclosed frame', m => { m.events.splice(9, 1); }],
  ['else inside loop', m => { m.events[4] = {id:'bad', kind:'else', frame:'f-auth', text:'wrong'}; }],
  ['branch leaks activation', m => { m.events.splice(7, 0, {id:'a-leak', kind:'activate', participant:'p-user'}); }],
  ['wrong version', m => { m.schemaVersion = 2; }],
  ['null document', () => null]
];
for (const [name, mutate] of rejects) {
  const model = fixture('nested');
  const result = mutate(model);
  assert.ok(validateSequence(result === null ? null : model).length, name);
  checks++;
}
// Identity remains stable when display text changes; no parser identity reconciliation is claimed.
const renamed = fixture('basic');
const beforeIds = [...renamed.participants, ...renamed.events].map(e => e.id);
renamed.participants[0].label = '고객';
renamed.events[0].text = '수정된 요청';
assert.deepEqual(validateSequence(renamed), []);
assert.deepEqual([...renamed.participants, ...renamed.events].map(e => e.id), beforeIds);
checks++;
console.log('PASS: ' + checks + ' sequence contract checks (no parser/render integration)');
