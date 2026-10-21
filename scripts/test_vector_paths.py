"""Catch SVG arc flag compaction that compiles but crashes Android 9 PathParser."""
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ARITY = dict(M=2, L=2, H=1, V=1, C=6, S=4, Q=4, T=2, A=7, Z=0)
TOKEN = re.compile(r'[MmLlHhVvCcSsQqTtAaZz]|[-+]?(?:\d*\.\d+|\d+\.?\d*)(?:[eE][-+]?\d+)?')

class VectorPathTest(unittest.TestCase):
    def test_all_vector_command_parameter_counts(self):
        root = Path(__file__).resolve().parents[1] / 'app/src/main/res/drawable'
        tested = 0
        for source in root.glob('*.xml'):
            for element in ET.parse(source).iter():
                value = element.attrib.get('{http://schemas.android.com/apk/res/android}pathData')
                if not value:
                    continue
                tested += 1
                command, args = None, []
                def check():
                    if command is None:
                        return
                    arity = ARITY[command.upper()]
                    self.assertTrue(len(args) == 0 if arity == 0 else len(args) > 0 and len(args) % arity == 0,
                                    f'{source.name}: {command} expects multiples of {arity}, got {args}')
                    if command.upper() == 'A' and len(args) % 7 == 0:
                        for i in range(0, len(args), 7):
                            self.assertIn(float(args[i+3]), (0, 1), source.name)
                            self.assertIn(float(args[i+4]), (0, 1), source.name)
                for token in TOKEN.findall(value):
                    if token in 'MmLlHhVvCcSsQqTtAaZz':
                        check(); command, args = token, []
                    else:
                        args.append(token)
                check()
        self.assertGreater(tested, 15)

if __name__ == '__main__':
    unittest.main()
