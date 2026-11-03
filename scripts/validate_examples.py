"""Validate saved model examples; this does not parse or render Mermaid."""
import json
from pathlib import Path

from jsonschema import Draft202012Validator

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = json.loads((ROOT / 'schemas/diagram-document.schema.json').read_text(encoding='utf-8'))
Draft202012Validator.check_schema(SCHEMA)
VALIDATOR = Draft202012Validator(SCHEMA)


def require(condition, message):
    if not condition:
        raise ValueError(message)


def validate(document):
    VALIDATOR.validate(document)
    model, layout = document['model'], document['layout']
    ids = [document['id']]

    def collect(value):
        if isinstance(value, dict):
            if 'id' in value:
                ids.append(value['id'])
            for child in value.values():
                collect(child)
        elif isinstance(value, list):
            for child in value:
                collect(child)

    collect(model)
    require(len(ids) == len(set(ids)), 'Duplicate document/element ID')
    is_class = document['kind'] == 'class'
    nodes = {x['id'] for x in model['classes' if is_class else 'participants']}
    edges = {x['id'] for x in model['relationships' if is_class else 'messages']}
    for key, placements in layout.items():
        allowed = edges if key in ('edges', 'messageSpacing') else nodes
        require(len({x['id'] for x in placements}) == len(placements), f'Duplicate {key} entry')
        require(all(x['id'] in allowed for x in placements), f'Invalid {key} reference')
    styles = document['styles']['elements']
    require(len({x['id'] for x in styles}) == len(styles), 'Duplicate style')
    require(all(x['id'] in nodes for x in styles), 'Invalid style target')
    if is_class:
        for edge in model['relationships']:
            require(edge['source'] in nodes and edge['target'] in nodes, 'Invalid relationship endpoint')
    else:
        messages = {x['id']: x for x in model['messages']}
        order = {x['id']: i for i, x in enumerate(model['messages'])}
        for message in messages.values():
            require(message['from'] in nodes and message['to'] in nodes, 'Invalid message endpoint')
            if 'replyTo' in message:
                call = messages.get(message['replyTo'])
                require(call is not None, 'Missing reply target')
                require(call['kind'] in ('sync', 'async') and order[call['id']] < order[message['id']], 'Invalid reply order')
                require((call['from'], call['to']) == (message['to'], message['from']), 'Invalid reply direction')
        intervals = {}
        for activation in model['activations']:
            participant = activation['participantId']
            start, end = activation['startMessageId'], activation['endMessageId']
            require(participant in nodes and start in messages and end in messages, 'Invalid activation reference')
            require(order[start] <= order[end], 'Reversed activation')
            for boundary in (start, end):
                require(participant in (messages[boundary]['from'], messages[boundary]['to']), 'Unrelated activation boundary')
            interval = order[start], order[end]
            for a, b in intervals.get(participant, []):
                c, d = interval
                require((a, b) != (c, d), 'Duplicate activation interval')
                require(not (a < c <= b < d or c < a <= d < b), 'Crossing activations')
            intervals.setdefault(participant, []).append(interval)


if __name__ == '__main__':
    for path in sorted((ROOT / 'examples').glob('*.json')):
        document = json.loads(path.read_text(encoding='utf-8'))
        validate(document)
        require(document['source']['text'] == path.with_suffix('.mmd').read_text(encoding='utf-8'), 'Mermaid fixture differs')
        require(json.loads(json.dumps(document, ensure_ascii=False)) == document, 'JSON round trip failed')
        print(f'PASS {path.name}: schema, references, layout, styles, source fixture, JSON round trip')
