import unittest
from unittest.mock import patch, call # Import call for checking multiple calls or specific args
import requests # Required if any functions raise requests.exceptions explicitly

# Functions to test
from utils.stock_data import get_historical_stock_data, get_news_sentiment
# Path to the function we need to mock
FETCH_DATA_PATH = "utils.stock_data.fetch_alpha_vantage_data"
# Path to api_key getter if we need to ensure it's NOT called when key is provided
GET_API_KEY_PATH = "utils.stock_data.get_alpha_vantage_api_key"


class TestStockDataUtilities(unittest.TestCase):

    @patch(FETCH_DATA_PATH)
    def test_get_historical_success(self, mock_fetch_alpha_vantage_data):
        mock_timeseries = {"2023-01-01": {"1. open": "100"}}
        mock_fetch_alpha_vantage_data.return_value = {
            "Meta Data": {},
            "Time Series (Daily)": mock_timeseries
        }

        result = get_historical_stock_data(stock_symbol="IBM", api_key="TESTKEY")
        self.assertEqual(result, mock_timeseries)
        mock_fetch_alpha_vantage_data.assert_called_once_with(
            {
                "function": "TIME_SERIES_DAILY_ADJUSTED",
                "symbol": "IBM",
                "outputsize": "compact"
            },
            api_key="TESTKEY"
        )

    @patch(FETCH_DATA_PATH)
    def test_get_historical_data_validation_error(self, mock_fetch_alpha_vantage_data):
        mock_fetch_alpha_vantage_data.return_value = {"Error Message": "Something is wrong"} # Missing "Time Series (Daily)"
        with self.assertRaisesRegex(ValueError, "Invalid or empty response received from Alpha Vantage for MSFT"):
            get_historical_stock_data(stock_symbol="MSFT", api_key="TESTKEY")

    @patch(FETCH_DATA_PATH)
    @patch(GET_API_KEY_PATH, return_value="MOCKED_ENV_KEY") # Mock env key getter
    def test_get_historical_uses_env_api_key(self, mock_get_env_key, mock_fetch_alpha_vantage_data):
        mock_timeseries = {"2023-01-01": {"1. open": "100"}}
        mock_fetch_alpha_vantage_data.return_value = {"Time Series (Daily)": mock_timeseries}

        # Call without explicit api_key
        result = get_historical_stock_data(stock_symbol="AAPL", output_size="full")
        self.assertEqual(result, mock_timeseries)
        mock_get_env_key.assert_called_once() # Ensure it tried to get key from env
        mock_fetch_alpha_vantage_data.assert_called_once_with(
            {
                "function": "TIME_SERIES_DAILY_ADJUSTED",
                "symbol": "AAPL",
                "outputsize": "full"
            },
            api_key="MOCKED_ENV_KEY" # Assert that the env key was used
        )

    @patch(FETCH_DATA_PATH)
    @patch(GET_API_KEY_PATH, return_value=None) # Mock env key getter to return None
    def test_get_historical_no_api_key_at_all(self, mock_get_env_key, mock_fetch_alpha_vantage_data):
        with self.assertRaisesRegex(ValueError, "API key must be provided either as an argument or set as ALPHA_VANTAGE_API_KEY"):
            get_historical_stock_data(stock_symbol="TSLA") # No key provided, env mock returns None
        mock_get_env_key.assert_called_once()
        mock_fetch_alpha_vantage_data.assert_not_called()


    # --- Tests for get_news_sentiment ---

    @patch(FETCH_DATA_PATH)
    def test_get_news_sentiment_success(self, mock_fetch_alpha_vantage_data):
        mock_feed = [{"title": "Article 1"}, {"title": "Article 2"}]
        mock_fetch_alpha_vantage_data.return_value = {"feed": mock_feed, "items": "2"}

        result = get_news_sentiment(tickers="MSFT", api_key="TESTKEY", limit=10)
        self.assertEqual(result, mock_feed)
        mock_fetch_alpha_vantage_data.assert_called_once_with(
            {
                "function": "NEWS_SENTIMENT",
                "tickers": "MSFT",
                "limit": "10", # Ensure limit is stringified
                "sort": "LATEST" # Default sort
            },
            api_key="TESTKEY"
        )

    @patch(FETCH_DATA_PATH)
    def test_get_news_sentiment_all_params(self, mock_fetch_alpha_vantage_data):
        mock_feed = [{"title": "Article Topic"}]
        mock_fetch_alpha_vantage_data.return_value = {"feed": mock_feed, "items": "1"}

        result = get_news_sentiment(
            tickers="GOOG",
            topics="technology",
            api_key="KEY123",
            time_from="20230101T0000",
            time_to="20230102T0000",
            sort="RELEVANCE",
            limit=5
        )
        self.assertEqual(result, mock_feed)
        mock_fetch_alpha_vantage_data.assert_called_once_with(
            {
                "function": "NEWS_SENTIMENT",
                "tickers": "GOOG",
                "topics": "technology",
                "time_from": "20230101T0000",
                "time_to": "20230102T0000",
                "sort": "RELEVANCE",
                "limit": "5"
            },
            api_key="KEY123"
        )

    @patch(FETCH_DATA_PATH)
    def test_get_news_sentiment_no_articles_items_zero(self, mock_fetch_alpha_vantage_data):
        mock_fetch_alpha_vantage_data.return_value = {"items": "0", "feed": []} # API might return empty feed too
        result = get_news_sentiment(tickers="NOSTOCK", api_key="TESTKEY")
        self.assertEqual(result, [])

    @patch(FETCH_DATA_PATH)
    def test_get_news_sentiment_no_articles_information_message(self, mock_fetch_alpha_vantage_data):
        # This case assumes fetch_alpha_vantage_data itself doesn't raise an error for this info message
        # and get_news_sentiment handles it by returning empty list.
        mock_fetch_alpha_vantage_data.return_value = {
            "Information": "No articles found for specified tickers/topics.",
            "feed": None # Or empty list
        }
        result = get_news_sentiment(tickers="UNKNOWN", api_key="TESTKEY")
        self.assertEqual(result, [])

    @patch(FETCH_DATA_PATH)
    def test_get_news_sentiment_missing_feed_key(self, mock_fetch_alpha_vantage_data):
        mock_fetch_alpha_vantage_data.return_value = {"items": "1"} # Has items, but no feed
        with self.assertRaisesRegex(ValueError, "Invalid response structure from Alpha Vantage for news sentiment"):
            get_news_sentiment(tickers="ANY", api_key="TESTKEY")

    def test_get_news_sentiment_no_tickers_or_topics(self):
        with self.assertRaisesRegex(ValueError, "Either 'tickers' or 'topics' must be provided."):
            get_news_sentiment(api_key="TESTKEY")

    @patch(FETCH_DATA_PATH)
    @patch(GET_API_KEY_PATH, return_value="MOCKED_NEWS_KEY")
    def test_get_news_sentiment_uses_env_api_key(self, mock_get_env_key, mock_fetch_alpha_vantage_data):
        mock_feed = [{"title": "News Item"}]
        mock_fetch_alpha_vantage_data.return_value = {"feed": mock_feed, "items": "1"}

        result = get_news_sentiment(tickers="ANY") # No explicit API key
        self.assertEqual(result, mock_feed)
        mock_get_env_key.assert_called_once()
        mock_fetch_alpha_vantage_data.assert_called_once_with(
            {
                "function": "NEWS_SENTIMENT",
                "tickers": "ANY",
                "limit": "50", # Default limit
                "sort": "LATEST"  # Default sort
            },
            api_key="MOCKED_NEWS_KEY"
        )

    @patch(FETCH_DATA_PATH)
    @patch(GET_API_KEY_PATH, return_value=None)
    def test_get_news_sentiment_no_api_key_at_all(self, mock_get_env_key, mock_fetch_alpha_vantage_data):
        with self.assertRaisesRegex(ValueError, "API key must be provided or set as ALPHA_VANTAGE_API_KEY"):
            get_news_sentiment(tickers="ANY")
        mock_get_env_key.assert_called_once()
        mock_fetch_alpha_vantage_data.assert_not_called()


if __name__ == '__main__':
    unittest.main()
