#!/usr/bin/env python3
"""Regression tests for the RSS/Atom news collector."""
import io
import unittest
from datetime import datetime, timedelta, timezone
from unittest.mock import patch

from scripts.fetch_news import clean, fetch_feed, parse_date


class FakeResponse:
    def __init__(self, payload):
        self.payload = payload

    def __enter__(self):
        return self

    def __exit__(self, *args):
        return False

    def read(self, _limit):
        return self.payload


class NewsFeedTests(unittest.TestCase):
    def setUp(self):
        self.cutoff = datetime.now(timezone.utc) - timedelta(days=2)

    def test_rss_item_extracts_summary_link_and_image(self):
        xml = b"""<?xml version="1.0"?>
        <rss><channel><item>
          <title>Обзор смартфона Android: новая модель</title>
          <link>https://example.com/article</link>
          <pubDate>Thu, 12 Nov 2026 10:00:00 +0000</pubDate>
          <description><![CDATA[<p>Тестовый материал об Android</p><img src="https://example.com/phone.jpg">]]></description>
        </item></channel></rss>"""
        with patch("scripts.fetch_news.urllib.request.urlopen", return_value=FakeResponse(xml)):
            items = fetch_feed("Тест", "https://example.com/rss", self.cutoff)
        self.assertEqual(len(items), 1)
        self.assertEqual(items[0]["url"], "https://example.com/article")
        self.assertIn("Android", items[0]["title"])
        self.assertEqual(items[0]["image"], "https://example.com/phone.jpg")

    def test_atom_entry_uses_href_and_media_thumbnail(self):
        xml = b"""<?xml version="1.0"?>
        <feed xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">
          <entry>
            <title>Новый смартфон на Android</title>
            <link rel="alternate" href="https://example.com/atom-article"/>
            <updated>2026-11-12T12:30:00Z</updated>
            <summary>Android смартфон и обзор камеры</summary>
            <media:thumbnail url="https://example.com/thumb.webp"/>
          </entry>
        </feed>"""
        with patch("scripts.fetch_news.urllib.request.urlopen", return_value=FakeResponse(xml)):
            items = fetch_feed("Atom", "https://example.com/atom", self.cutoff)
        self.assertEqual(len(items), 1)
        self.assertEqual(items[0]["url"], "https://example.com/atom-article")
        self.assertEqual(items[0]["image"], "https://example.com/thumb.webp")

    def test_old_and_unrelated_items_are_filtered(self):
        xml = b"""<rss><channel>
          <item><title>Смартфон прошлого месяца</title><link>https://example.com/old</link>
            <pubDate>Mon, 01 Jan 2024 10:00:00 +0000</pubDate></item>
          <item><title>Кулинарный рецепт</title><link>https://example.com/food</link>
            <pubDate>Thu, 12 Nov 2026 10:00:00 +0000</pubDate></item>
        </channel></rss>"""
        with patch("scripts.fetch_news.urllib.request.urlopen", return_value=FakeResponse(xml)):
            items = fetch_feed("Тест", "https://example.com/rss", self.cutoff)
        self.assertEqual(items, [])

    def test_clean_removes_markup_and_normalizes_whitespace(self):
        self.assertEqual(clean("<p>  Тест&nbsp; <b>RSS</b> </p>"), "Тест RSS")

    def test_parse_date_handles_iso_and_rfc822(self):
        self.assertIsNotNone(parse_date("2026-11-12T12:30:00Z"))
        self.assertIsNotNone(parse_date("Thu, 12 Nov 2026 10:00:00 +0000"))
        self.assertIsNone(parse_date("not a date"))


if __name__ == "__main__":
    unittest.main()
