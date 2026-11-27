import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import fit_msg233_neighbourhood as n  # noqa: E402


class Msg233NeighbourhoodTest(unittest.TestCase):
    def test_next_other_skips_233(self):
        msgs = [(233, 0xC8, b''), (233, 0x00, b''), (21, None, None)]
        self.assertEqual(21, n.next_other(msgs, 0))
        self.assertIsNone(n.next_other([(233, 0xC8, b'')], 0))

    def test_c8_rate_against_other_tags(self):
        per = [[(0xC8, True), (0xC8, True), (0x00, False), (0x08, False)]]
        count, rate, other, other_rate = n.c8_stat(per)
        self.assertEqual((2, 1.0, 2, 0.0), (count, rate, other, other_rate))

    def test_permutation_is_deterministic_and_bounded(self):
        per = [[(0xC8, True)] + [(0x00, False)] * 20]
        p1 = n.permutation_p(per, 200, 7)
        self.assertEqual(p1, n.permutation_p(per, 200, 7))
        self.assertTrue(0 < p1 <= 1)

    def test_structure_counts_variants_per_byte(self):
        files = [([(233, 0xE0, bytes([1, 2, 3, 0xE0])), (233, 0xE0, bytes([1, 5, 3, 0xE0]))], 4)]
        self.assertEqual((2, 2, [1, 2, 1]), n.structure(files, 0xE0))


if __name__ == '__main__':
    unittest.main()
