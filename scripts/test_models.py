import json
import unittest
from copy import deepcopy

from jsonschema import ValidationError
from validate_examples import ROOT, validate


class ModelContractTests(unittest.TestCase):
    def setUp(self):
        self.cls = json.loads((ROOT / 'examples/class-diagram.json').read_text(encoding='utf-8'))
        self.seq = json.loads((ROOT / 'examples/sequence-diagram.json').read_text(encoding='utf-8'))

    def rejected(self, document):
        with self.assertRaises((ValueError, ValidationError)):
            validate(document)

    def test_node_limit(self):
        for original, key in ((self.cls, 'classes'), (self.seq, 'participants')):
            d = deepcopy(original)
            while len(d['model'][key]) < 30:
                node = deepcopy(d['model'][key][-1])
                node['id'] = f'new-{len(d["model"][key])}'
                if key == 'classes':
                    node['attributes'], node['methods'] = [], []
                d['model'][key].append(node)
            validate(d)
            node = deepcopy(d['model'][key][-1])
            node['id'] = 'node-31'
            d['model'][key].append(node)
            self.rejected(d)

    def test_missing_and_duplicate_references(self):
        self.cls['layout']['edges'][0]['id'] = 'missing'
        self.rejected(self.cls)
        self.seq['model']['messages'][0]['to'] = 'missing'
        self.rejected(self.seq)

    def test_duplicate_layout(self):
        self.cls['layout']['nodes'].append(deepcopy(self.cls['layout']['nodes'][0]))
        self.rejected(self.cls)

    def test_invalid_sizes_and_font(self):
        d = deepcopy(self.cls)
        d['layout']['sizes'][0]['width'] = 0
        self.rejected(d)
        self.cls['styles']['fontSize'] = 31
        self.rejected(self.cls)

    def test_reply_and_activation_order(self):
        d = deepcopy(self.seq)
        d['model']['messages'][2]['replyTo'] = 'result'
        self.rejected(d)
        self.seq['model']['activations'][0]['startMessageId'] = 'result'
        self.seq['model']['activations'][0]['endMessageId'] = 'place'
        self.rejected(self.seq)

    def test_serialized_document_preserves_edits(self):
        self.cls['layout']['nodes'][0]['x'] = -125
        self.cls['styles']['elements'][0]['fill'] = '#EF4444'
        restored = json.loads(json.dumps(self.cls))
        validate(restored)
        self.assertEqual(restored, self.cls)

    def test_unsupported_version_and_kind(self):
        d = deepcopy(self.cls)
        d['schemaVersion'] = '2.0'
        self.rejected(d)
        self.cls['kind'] = 'flowchart'
        self.rejected(self.cls)


if __name__ == '__main__':
    unittest.main()
