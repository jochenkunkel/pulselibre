import unittest
from java21_bytecode_experiment import transform, JAVA_17, JAVA_21, KOTLIN_17, KOTLIN_21


class Java21ExperimentTransformTest(unittest.TestCase):
    def setUp(self):
        self.source = f"compileOptions {{ {JAVA_17} }}\nkotlin {{ {KOTLIN_17} }}\n"

    def test_transforms_java_and_kotlin(self):
        result = transform(self.source)
        self.assertIn(JAVA_21, result)
        self.assertIn(KOTLIN_21, result)
        self.assertNotIn(JAVA_17, result)
        self.assertNotIn(KOTLIN_17, result)

    def test_rejects_old_kotlin_dsl(self):
        with self.assertRaises(ValueError):
            transform(self.source.replace(KOTLIN_17, 'jvmTarget = "17"'))

    def test_rejects_duplicate_targets(self):
        with self.assertRaises(ValueError):
            transform(self.source + KOTLIN_17)

    def test_rejects_already_migrated_target(self):
        with self.assertRaises(ValueError):
            transform(self.source + KOTLIN_21)

    def test_preserves_unrelated_configuration(self):
        self.assertIn("compileOptions", transform(self.source))


if __name__ == "__main__":
    unittest.main()
