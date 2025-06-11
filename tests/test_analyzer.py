import unittest
import datetime
from datetime import date, timedelta

# Functions to test
from analysis.analyzer import analyze_stock_data, suggest_option_strategy

class TestStockAnalyzer(unittest.TestCase):

    def _generate_historical_data(self, start_date_str, prices):
        """Helper to generate historical data for tests."""
        data = {}
        current_date = datetime.datetime.strptime(start_date_str, "%Y-%m-%d").date()
        for i, price in enumerate(prices):
            date_str = (current_date - timedelta(days=len(prices) - 1 - i)).strftime("%Y-%m-%d")
            data[date_str] = {
                "1. open": str(price - 0.5), "2. high": str(price + 0.5),
                "3. low": str(price - 1.0), "4. close": str(price),
                "5. adjusted close": str(price), "6. volume": str(100000 + i*1000),
                "7. dividend amount": "0.00", "8. split coefficient": "1.0"
            }
        return data

    def _generate_news_sentiment(self, scores):
        """Helper to generate news sentiment data."""
        news_list = []
        for i, score in enumerate(scores):
            news_list.append({
                "title": f"Article {i+1}",
                "url": f"http://example.com/article{i+1}",
                "overall_sentiment_score": score,
                "overall_sentiment_label": "Positive" if score > 0.1 else ("Negative" if score < -0.1 else "Neutral"),
                "ticker_sentiment": [{"ticker": "TEST", "relevance_score": "0.9", "ticker_sentiment_score": score, "ticker_sentiment_label": "Positive"}]
            })
        return news_list

    # --- Tests for analyze_stock_data ---

    def test_analyze_upward_trend_positive_news(self):
        hist_data = self._generate_historical_data("2023-10-10", [100, 101, 102, 103, 104, 105, 106])
        news_data = self._generate_news_sentiment([0.3, 0.4, 0.5]) # Positive

        result = analyze_stock_data(hist_data, news_data)

        self.assertEqual(result["current_price"], 106.0)
        self.assertEqual(result["latest_data_date"], "2023-10-10")
        self.assertEqual(result["trend_analysis"]["direction"], "UP")
        self.assertEqual(result["sentiment_analysis"]["label"], "POSITIVE")
        self.assertAlmostEqual(result["sentiment_analysis"]["score"], 0.4)
        self.assertEqual(result["predicted_change_percent"], 3.0) # 3% for UP + POSITIVE
        self.assertIn("upward trend with positive news", result["analysis_summary"])

    def test_analyze_downward_trend_negative_news(self):
        hist_data = self._generate_historical_data("2023-10-10", [110, 109, 108, 107, 106, 105, 104])
        news_data = self._generate_news_sentiment([-0.3, -0.4, -0.5]) # Negative

        result = analyze_stock_data(hist_data, news_data)
        self.assertEqual(result["current_price"], 104.0)
        self.assertEqual(result["trend_analysis"]["direction"], "DOWN")
        self.assertEqual(result["sentiment_analysis"]["label"], "NEGATIVE")
        self.assertAlmostEqual(result["sentiment_analysis"]["score"], -0.4)
        self.assertEqual(result["predicted_change_percent"], -3.0) # -3% for DOWN + NEGATIVE
        self.assertIn("downward trend with negative news", result["analysis_summary"])

    def test_analyze_flat_trend_neutral_news(self):
        hist_data = self._generate_historical_data("2023-10-10", [100, 100, 100, 100, 100, 100, 100])
        news_data = self._generate_news_sentiment([0.0, 0.05, -0.05]) # Neutral

        result = analyze_stock_data(hist_data, news_data)
        self.assertEqual(result["current_price"], 100.0)
        self.assertEqual(result["trend_analysis"]["direction"], "FLAT")
        self.assertEqual(result["sentiment_analysis"]["label"], "NEUTRAL")
        self.assertAlmostEqual(result["sentiment_analysis"]["score"], 0.0)
        self.assertEqual(result["predicted_change_percent"], 0.0)
        self.assertIn("Mixed signals", result["analysis_summary"])

    def test_analyze_insufficient_historical_data(self):
        hist_data = self._generate_historical_data("2023-10-10", [100]) # Only one day
        news_data = self._generate_news_sentiment([0.5])
        result = analyze_stock_data(hist_data, news_data)
        self.assertEqual(result["current_price"], 100.0)
        # Trend should be FLAT or some default if only one point, current logic makes it FLAT.
        self.assertEqual(result["trend_analysis"]["direction"], "FLAT")
        self.assertIn("Trend analysis not fully implemented", result["trend_analysis"]["details"]) # Default detail

    def test_analyze_empty_historical_data(self):
        hist_data = {}
        news_data = self._generate_news_sentiment([0.5])
        result = analyze_stock_data(hist_data, news_data)
        self.assertEqual(result["current_price"], 0.0) # Default current price
        self.assertEqual(result["latest_data_date"], "Unknown")
        self.assertEqual(result["trend_analysis"]["direction"], "FLAT") # Default
        # Prediction should likely be 0 or N/A
        self.assertTrue(result["predicted_short_term_target"] == "N/A" or result["predicted_short_term_target"] == 0.0)


    def test_analyze_empty_news_data(self):
        hist_data = self._generate_historical_data("2023-10-10", [100, 101, 102])
        news_data = []
        result = analyze_stock_data(hist_data, news_data)

        actual_details = result["sentiment_analysis"]["details"]
        # print(f"\nDEBUG test_analyze_empty_news_data: sentiment_analysis['details'] = '{actual_details}'") # Debug print removed

        self.assertEqual(result["sentiment_analysis"]["label"], "NEUTRAL")
        self.assertEqual(result["sentiment_analysis"]["score"], 0.0)
        self.assertEqual(actual_details, "No valid sentiment scores found in news data.")

    def test_analyze_news_with_no_scores(self):
        hist_data = self._generate_historical_data("2023-10-10", [100, 101, 102])
        news_data = [{"title": "article"}, {"title": "another article"}] # No overall_sentiment_score
        result = analyze_stock_data(hist_data, news_data)
        self.assertEqual(result["sentiment_analysis"]["label"], "NEUTRAL")
        self.assertEqual(result["sentiment_analysis"]["score"], 0.0)
        self.assertIn("No valid sentiment scores found", result["sentiment_analysis"]["details"])


    # --- Tests for suggest_option_strategy ---

    def _get_base_analysis_output(self, current_price, predicted_change_percent, latest_date_str="2023-10-10"):
        return {
            "current_price": current_price,
            "latest_data_date": latest_date_str,
            "trend_analysis": {"direction": "UP" if predicted_change_percent > 0 else ("DOWN" if predicted_change_percent < 0 else "FLAT")},
            "sentiment_analysis": {"score": 0.2 * (1 if predicted_change_percent > 0 else (-1 if predicted_change_percent < 0 else 0))},
            "predicted_change_percent": predicted_change_percent,
            "predicted_short_term_target": current_price * (1 + predicted_change_percent / 100.0),
            "analysis_summary": "Some summary."
        }

    def test_suggest_buy_call_moderate_risk(self):
        analysis = self._get_base_analysis_output(current_price=100.0, predicted_change_percent=3.0)
        suggestions = suggest_option_strategy(analysis, risk_tolerance="moderate")
        self.assertEqual(len(suggestions), 1)
        s = suggestions[0]
        self.assertEqual(s["strategy"], "Buy Call")
        self.assertEqual(s["option_type"], "Call")
        self.assertEqual(s["action"], "Buy")
        self.assertAlmostEqual(s["strike_price"], 100.0 * 1.02) # Moderate risk OTM_CALL_FACTOR = 1.02
        self.assertIn("positive price prediction", s["rationale"])
        self.assertEqual(s["confidence_level"], "Medium") # 3% change is Medium

        # Check expiration date is roughly 40 days out
        expected_exp_date = (datetime.datetime.strptime(analysis["latest_data_date"], "%Y-%m-%d").date() + timedelta(days=40)).strftime("%Y-%m-%d")
        self.assertEqual(s["expiration_date"], expected_exp_date)


    def test_suggest_buy_put_high_risk(self):
        analysis = self._get_base_analysis_output(current_price=100.0, predicted_change_percent=-5.0)
        suggestions = suggest_option_strategy(analysis, risk_tolerance="high")
        self.assertEqual(len(suggestions), 1)
        s = suggestions[0]
        self.assertEqual(s["strategy"], "Buy Put")
        self.assertEqual(s["option_type"], "Put")
        self.assertAlmostEqual(s["strike_price"], 100.0 * 0.95) # High risk OTM_PUT_FACTOR = 0.95
        self.assertIn("negative price prediction", s["rationale"])
        self.assertEqual(s["confidence_level"], "Medium") # 5% change is Medium, >5% is High

    def test_suggest_buy_call_high_confidence(self): # Test high confidence level
        analysis = self._get_base_analysis_output(current_price=100.0, predicted_change_percent=6.0)
        suggestions = suggest_option_strategy(analysis, risk_tolerance="moderate")
        self.assertEqual(suggestions[0]["confidence_level"], "High")


    def test_suggest_hold_neutral_prediction(self):
        analysis = self._get_base_analysis_output(current_price=100.0, predicted_change_percent=0.5) # Between -1% and 1%
        suggestions = suggest_option_strategy(analysis, risk_tolerance="moderate")
        self.assertEqual(len(suggestions), 1)
        s = suggestions[0]
        self.assertEqual(s["strategy"], "Hold / Neutral")
        self.assertIsNone(s["option_type"])
        self.assertIn("prediction (0.5%) is close to zero", s["rationale"])
        self.assertEqual(s["confidence_level"], "Low")


    def test_suggest_strike_price_low_risk_call(self):
        analysis = self._get_base_analysis_output(current_price=100.0, predicted_change_percent=3.0)
        suggestions = suggest_option_strategy(analysis, risk_tolerance="low")
        self.assertAlmostEqual(suggestions[0]["strike_price"], 100.0 * 1.005) # Low risk OTM_CALL_FACTOR = 1.005

    def test_suggest_strike_price_low_risk_put(self):
        analysis = self._get_base_analysis_output(current_price=100.0, predicted_change_percent=-3.0)
        suggestions = suggest_option_strategy(analysis, risk_tolerance="low")
        self.assertAlmostEqual(suggestions[0]["strike_price"], 100.0 * 0.995) # Low risk OTM_PUT_FACTOR = 0.995

    def test_suggest_invalid_current_price_in_analysis(self):
        # Manually create analysis output for this specific case
        analysis = {
            "current_price": "N/A", # Invalid price
            "latest_data_date": "2023-10-10",
            "trend_analysis": {"direction": "UP"},
            "sentiment_analysis": {"score": 0.2, "label": "POSITIVE"},
            "predicted_change_percent": 3.0,
            "predicted_short_term_target": "N/A", # Target would also be N/A
            "analysis_summary": "Summary with N/A price."
        }
        suggestions = suggest_option_strategy(analysis, risk_tolerance="moderate")
        self.assertEqual(len(suggestions), 1)
        self.assertEqual(suggestions[0]["strategy"], "Hold")
        self.assertIn("invalid current price", suggestions[0]["rationale"])

    def test_suggest_missing_fields_in_analysis(self):
        # Test with missing predicted_change_percent
        analysis_missing_pred = {"current_price": 100.0, "latest_data_date": "2023-10-10"}
        suggestions = suggest_option_strategy(analysis_missing_pred, risk_tolerance="moderate")
        # Should default to Hold or similar if critical info is missing
        self.assertEqual(suggestions[0]["strategy"], "Hold / Neutral")
        self.assertIn("prediction (0.0%) is close to zero", suggestions[0]["rationale"]) # predicted_change_percent defaults to 0.0


if __name__ == '__main__':
    unittest.main()
