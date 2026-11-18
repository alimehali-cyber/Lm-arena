import copy
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import audit


def empty_manifest():
    return {"schemaVersion": 1, "spacecraft": {key: {"status": "TODO", "images": []} for key in audit.REQUIRED}}


def candidate():
    # Test data, not a claim that this page or image exists or is licensed.
    return {
        "uri": audit.BUNDLED_PREFIX + "sample.webp", "kind": "PHOTO", "credit": "Agency",
        "creditFa": "آژانس", "author": "Example", "license": "CC BY 4.0",
        "sourceUrl": "https://commons.wikimedia.org/wiki/File:Example.webp", "focal": [0.5, 0.5],
        "placeholderColor": "#123456", "altEn": "Example", "altFa": "نمونه", "verified": True,
    }


class AuditTest(unittest.TestCase):
    def test_empty_todo_manifest_passes_but_is_not_a_licence_verification(self):
        self.assertEqual([], audit.validate(empty_manifest(), Path(".")))

    def test_verified_candidate_checks_actual_bundle_and_fields(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / "sample.webp").write_bytes(b"example")
            manifest = empty_manifest()
            manifest["spacecraft"]["iss"] = {"status": "VERIFIED", "images": [candidate()]}
            with patch.object(audit, "dimensions", return_value=(1200, 600)):
                self.assertEqual([], audit.validate(manifest, root))
                bad = copy.deepcopy(manifest)
                bad["spacecraft"]["iss"]["images"][0]["license"] = "CC BY-NC 4.0"
                self.assertTrue(any("licence" in e for e in audit.validate(bad, root)))
                bad = copy.deepcopy(manifest)
                bad["spacecraft"]["iss"]["images"][0]["uri"] = audit.BUNDLED_PREFIX + "../sample.webp"
                self.assertTrue(any("filename" in e for e in audit.validate(bad, root)))

    def test_fail_closed_on_unreviewed_missing_or_ineligible_metadata(self):
        manifest = empty_manifest()
        manifest["spacecraft"]["starlink"] = {"status": "TODO", "images": [candidate()]}
        errors = audit.validate(manifest, Path("/nonexistent"))
        self.assertTrue(any("TODO entries" in e for e in errors))
        self.assertTrue(any("missing" in e for e in errors))
        self.assertFalse(audit.valid_source("https://example.com/search?q=iss"))
        self.assertFalse(audit.valid_source("https://evilnasa.gov/file"))
        self.assertFalse(audit.valid_source("https://commons.wikimedia.org/wiki/Main_Page"))
        self.assertTrue(audit.valid_source("https://www.flickr.com/photos/spacex/1234"))

    def test_focal_shared_starlink_and_generic_key_rules(self):
        manifest = empty_manifest()
        image = candidate()
        image["focal"] = [float("nan"), 2]
        manifest["spacecraft"]["starlink"] = {"status": "VERIFIED", "images": [image, image]}
        manifest["spacecraft"]["eo"] = {"status": "TODO", "images": []}
        errors = audit.validate(manifest, Path("/nonexistent"))
        self.assertTrue(any("1–1 images" in e for e in errors))
        self.assertTrue(any("focal" in e for e in errors))
        self.assertTrue(any("generic shape" in e for e in errors))


if __name__ == "__main__":
    unittest.main()
