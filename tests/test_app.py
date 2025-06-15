import unittest
import json
from unittest.mock import patch, MagicMock # MagicMock can mock .get on dicts easily

# Assuming app.py is in the parent directory or accessible via PYTHONPATH
from app import app

# For mocking exceptions from external services if needed
import requests

class TestApiEndpoints(unittest.TestCase):

    def setUp(self):
        app.config['TESTING'] = True
        self.client = app.test_client()
        # It's good practice to have a dummy API key for tests if get_alpha_vantage_api_key is NOT mocked
        # However, for most of these tests, we'll mock the functions that use the key.
        # For test_trading_suggestions_success, we will mock get_alpha_vantage_api_key itself.

    # --- Mocks for external services and internal logic functions ---
    MOCK_GET_API_KEY_PATH = 'app.get_alpha_vantage_api_key' # app.py imports it directly
    MOCK_HISTORICAL_DATA_PATH = 'app.get_historical_stock_data'
    MOCK_NEWS_SENTIMENT_PATH = 'app.get_news_sentiment'
    MOCK_ANALYZE_DATA_PATH = 'app.analyze_stock_data'
    MOCK_SUGGEST_STRATEGY_PATH = 'app.suggest_option_strategy'

    @patch(MOCK_SUGGEST_STRATEGY_PATH)
    @patch(MOCK_ANALYZE_DATA_PATH)
    @patch(MOCK_NEWS_SENTIMENT_PATH)
    @patch(MOCK_HISTORICAL_DATA_PATH)
    @patch(MOCK_GET_API_KEY_PATH) # Mocking this as it's called in the app route
    def test_trading_suggestions_success(self,
                                         mock_get_api_key,
                                         mock_get_historical,
                                         mock_get_news,
                                         mock_analyze_data,
                                         mock_suggest_strategy):
        # Configure mocks
        mock_get_api_key.return_value = "DUMMY_API_KEY" # Not strictly needed if others are mocked well

        mock_historical_data_output = {"2023-01-01": {"4. close": "150.00"}} # Simplified
        mock_get_historical.return_value = mock_historical_data_output

        mock_news_data_output = [{"title": "Test News"}]
        mock_get_news.return_value = mock_news_data_output

        mock_analysis_output = {
            "current_price": 150.00,
            "latest_data_date": "2023-01-01",
            "trend_analysis": {"direction": "UP", "details": "Mocked trend"},
            "sentiment_analysis": {"score": 0.5, "label": "POSITIVE", "details": "Mocked sentiment"},
            "predicted_change_percent": 2.5,
            "predicted_short_term_target": 153.75,
            "analysis_summary": "Mocked analysis summary."
        }
        mock_analyze_data.return_value = mock_analysis_output

        mock_strategy_output = [{
            "strategy": "Buy Call", "option_type": "Call", "action": "Buy",
            "strike_price": 155.00, "expiration_date": "2023-03-01",
            "rationale": "Mocked rationale", "confidence_level": "High"
        }]
        mock_suggest_strategy.return_value = mock_strategy_output

        payload = {"stock_symbol": "TEST", "risk_tolerance": "moderate"}
        response = self.client.post('/v1/trading-suggestions',
                                    data=json.dumps(payload),
                                    content_type='application/json')

        self.assertEqual(response.status_code, 200)
        response_json = json.loads(response.data)

        self.assertEqual(response_json["stock_symbol"], "TEST")
        self.assertEqual(response_json["current_price"], 150.00)
        self.assertEqual(response_json["analysis_summary"], "Mocked analysis summary.")
        self.assertIn("predicted_price_targets", response_json)
        self.assertEqual(response_json["predicted_price_targets"]["short_term_target"], 153.75)
        self.assertEqual(response_json["suggested_options_strategies"], mock_strategy_output)
        self.assertIn("data_sources", response_json)
        self.assertIn("timestamp", response_json)
        self.assertIn("disclaimer", response_json)

        # Check that mocks were called with expected args (simplified checks)
        mock_get_historical.assert_called_once_with(api_key=None, stock_symbol="TEST", output_size='compact')
        mock_get_news.assert_called_once_with(api_key=None, tickers="TEST", limit=50)
        mock_analyze_data.assert_called_once_with(historical_data=mock_historical_data_output,
                                                news_sentiment_data=mock_news_data_output)
        mock_suggest_strategy.assert_called_once_with(analysis_output=mock_analysis_output,
                                                    risk_tolerance="moderate")


    def test_missing_json_payload(self):
        response = self.client.post('/v1/trading-suggestions',
                                    data=None,
                                    content_type='application/json')
        self.assertEqual(response.status_code, 400)
        response_json = json.loads(response.data)
        self.assertIn("error", response_json)
        self.assertEqual(response_json["error"], "Invalid or malformed JSON payload.") # Exact match

    def test_empty_json_payload(self): # Different from None data
        response = self.client.post('/v1/trading-suggestions',
                                    data=json.dumps({}),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 400)
        response_json = json.loads(response.data)
        self.assertIn("error", response_json)
        self.assertIn("stock_symbol is required", response_json["error"])

    def test_missing_stock_symbol(self):
        payload = {"risk_tolerance": "moderate"}
        response = self.client.post('/v1/trading-suggestions',
                                    data=json.dumps(payload),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 400)
        response_json = json.loads(response.data)
        self.assertIn("error", response_json)
        self.assertIn("stock_symbol is required", response_json["error"])

    def test_invalid_risk_tolerance(self):
        payload = {"stock_symbol": "TEST", "risk_tolerance": "very_high"}
        response = self.client.post('/v1/trading-suggestions',
                                    data=json.dumps(payload),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 400)
        response_json = json.loads(response.data)
        self.assertIn("error", response_json)
        self.assertIn("Invalid risk_tolerance", response_json["error"])

    @patch(MOCK_HISTORICAL_DATA_PATH)
    @patch(MOCK_GET_API_KEY_PATH, return_value="DUMMY_KEY")
    def test_historical_data_returns_none_or_empty(self, mock_get_api_key, mock_get_historical):
        mock_get_historical.return_value = None # Simulate stock not found or empty data
        payload = {"stock_symbol": "INVALID"}
        response = self.client.post('/v1/trading-suggestions',
                                    data=json.dumps(payload),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 404)
        response_json = json.loads(response.data)
        self.assertIn("error", response_json)
        self.assertIn("Could not fetch historical data for INVALID", response_json["error"])

    @patch(MOCK_HISTORICAL_DATA_PATH, side_effect=ValueError("Alpha Vantage API Error: Invalid symbol"))
    @patch(MOCK_GET_API_KEY_PATH, return_value="DUMMY_KEY")
    def test_historical_data_raises_value_error_api(self, mock_get_api_key, mock_get_historical):
        payload = {"stock_symbol": "ERROR"}
        response = self.client.post('/v1/trading-suggestions',
                                    data=json.dumps(payload),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 502) # Bad Gateway for upstream API errors
        response_json = json.loads(response.data)
        self.assertIn("error", response_json)
        self.assertIn("External API error: Alpha Vantage API Error: Invalid symbol", response_json["error"])

    @patch(MOCK_HISTORICAL_DATA_PATH, side_effect=requests.exceptions.RequestException("Network timeout"))
    @patch(MOCK_GET_API_KEY_PATH, return_value="DUMMY_KEY")
    def test_historical_data_raises_request_exception(self, mock_get_api_key, mock_get_historical):
        payload = {"stock_symbol": "NETERROR"}
        response = self.client.post('/v1/trading-suggestions',
                                    data=json.dumps(payload),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 503) # Service unavailable
        response_json = json.loads(response.data)
        self.assertIn("error", response_json)
        self.assertIn("Could not connect to external data provider: Network timeout", response_json["error"])

    @patch(MOCK_NEWS_SENTIMENT_PATH, side_effect=ValueError("News fetch error"))
    @patch(MOCK_HISTORICAL_DATA_PATH, return_value={"2023-01-01": {"4. close": "100"}}) # Hist data must succeed
    @patch(MOCK_GET_API_KEY_PATH, return_value="DUMMY_KEY")
    def test_news_sentiment_failure(self, mock_get_api_key, mock_get_historical, mock_get_news):
        payload = {"stock_symbol": "TEST"}
        response = self.client.post('/v1/trading-suggestions',
                                    data=json.dumps(payload),
                                    content_type='application/json')
        # Assuming general ValueError from news is treated as a 400 or 500 if not API specific
        self.assertTrue(response.status_code == 400 or response.status_code == 502)
        response_json = json.loads(response.data)
        self.assertIn("error", response_json)
        # Check if it's an API error or a generic one
        if "External API error" in response_json["error"]:
             self.assertIn("News fetch error", response_json["error"])
        else:
            self.assertIn("Input or data processing error: News fetch error", response_json["error"])


    @patch(MOCK_ANALYZE_DATA_PATH, side_effect=Exception("Core analysis failed"))
    @patch(MOCK_NEWS_SENTIMENT_PATH, return_value=[]) # News must succeed
    @patch(MOCK_HISTORICAL_DATA_PATH, return_value={"2023-01-01": {"4. close": "100"}}) # Hist data must succeed
    @patch(MOCK_GET_API_KEY_PATH, return_value="DUMMY_KEY")
    def test_analysis_failure(self, mock_get_api_key, mock_get_historical, mock_get_news, mock_analyze_data):
        payload = {"stock_symbol": "TEST"}
        response = self.client.post('/v1/trading-suggestions',
                                    data=json.dumps(payload),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 500)
        response_json = json.loads(response.data)
        self.assertIn("error", response_json)
        self.assertIn("An unexpected internal server error occurred.", response_json["error"])

    @patch(MOCK_SUGGEST_STRATEGY_PATH, side_effect=Exception("Strategy suggestion error"))
    @patch(MOCK_ANALYZE_DATA_PATH, return_value={"current_price": 100}) # Analysis must succeed
    @patch(MOCK_NEWS_SENTIMENT_PATH, return_value=[])
    @patch(MOCK_HISTORICAL_DATA_PATH, return_value={"2023-01-01": {"4. close": "100"}})
    @patch(MOCK_GET_API_KEY_PATH, return_value="DUMMY_KEY")
    def test_strategy_suggestion_failure(self, mock_get_api_key, mock_get_historical, mock_get_news,
                                         mock_analyze_data, mock_suggest_strategy):
        payload = {"stock_symbol": "TEST"}
        response = self.client.post('/v1/trading-suggestions',
                                    data=json.dumps(payload),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 500)
        response_json = json.loads(response.data)
        self.assertIn("error", response_json)
        self.assertIn("An unexpected internal server error occurred.", response_json["error"])


if __name__ == '__main__':
    unittest.main()
