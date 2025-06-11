import unittest
from unittest.mock import patch, Mock
import os
import requests # For exceptions

# Modules to test
from utils.api_client import get_alpha_vantage_api_key, fetch_alpha_vantage_data

class TestAlphaVantageAPIClient(unittest.TestCase):

    @patch.dict(os.environ, {"ALPHA_VANTAGE_API_KEY": "TESTKEY123"})
    def test_get_api_key_success(self):
        self.assertEqual(get_alpha_vantage_api_key(), "TESTKEY123")

    @patch.dict(os.environ, {}, clear=True) # Clear existing env vars for this test
    def test_get_api_key_not_set(self):
        # This function itself doesn't raise ValueError, fetch_alpha_vantage_data does.
        # get_alpha_vantage_api_key should return None if not set.
        self.assertIsNone(get_alpha_vantage_api_key())

    @patch('utils.api_client.requests.get')
    @patch.dict(os.environ, {"ALPHA_VANTAGE_API_KEY": "TESTKEY123"}) # Ensure key is available via env
    def test_fetch_data_success(self, mock_requests_get):
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {"success": True, "data": "some data"}
        mock_requests_get.return_value = mock_response

        params = {"function": "TIME_SERIES_INTRADAY", "symbol": "IBM"}
        result = fetch_alpha_vantage_data(params)

        self.assertEqual(result, {"success": True, "data": "some data"})
        mock_requests_get.assert_called_once()
        # Check if apikey was added to params
        args, kwargs = mock_requests_get.call_args
        self.assertIn("apikey", kwargs['params'])
        self.assertEqual(kwargs['params']['apikey'], "TESTKEY123")

    @patch('utils.api_client.requests.get')
    def test_fetch_data_success_with_direct_api_key(self, mock_requests_get):
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {"success": True, "data": "direct key data"}
        mock_requests_get.return_value = mock_response

        params = {"function": "TIME_SERIES_INTRADAY", "symbol": "IBM"}
        # Pass API key directly
        result = fetch_alpha_vantage_data(params, api_key="DIRECTKEY")

        self.assertEqual(result, {"success": True, "data": "direct key data"})
        mock_requests_get.assert_called_once()
        args, kwargs = mock_requests_get.call_args
        self.assertEqual(kwargs['params']['apikey'], "DIRECTKEY")


    @patch('utils.api_client.requests.get')
    @patch.dict(os.environ, {}, clear=True) # Ensure API key is NOT in env
    def test_fetch_data_no_api_key(self, mock_requests_get):
        params = {"function": "TIME_SERIES_INTRADAY", "symbol": "IBM"}
        with self.assertRaisesRegex(ValueError, "Alpha Vantage API key not provided and not found in environment variables."):
            fetch_alpha_vantage_data(params)
        mock_requests_get.assert_not_called()


    @patch('utils.api_client.requests.get')
    @patch.dict(os.environ, {"ALPHA_VANTAGE_API_KEY": "TESTKEY123"})
    def test_fetch_data_api_error_message(self, mock_requests_get):
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {"Error Message": "Invalid API call or symbol."}
        mock_requests_get.return_value = mock_response

        params = {"function": "TIME_SERIES_INTRADAY", "symbol": "INVALID"}
        with self.assertRaisesRegex(ValueError, "Alpha Vantage API Error: Invalid API call or symbol."):
            fetch_alpha_vantage_data(params)

    @patch('utils.api_client.requests.get')
    @patch.dict(os.environ, {"ALPHA_VANTAGE_API_KEY": "TESTKEY123"})
    def test_fetch_data_api_information_rate_limit(self, mock_requests_get):
        mock_response = Mock()
        mock_response.status_code = 200
        # Example of rate limit message
        mock_response.json.return_value = {"Information": "API call frequency is 5 calls per minute and 100 calls per day."}
        mock_requests_get.return_value = mock_response

        params = {"function": "TIME_SERIES_INTRADAY", "symbol": "IBM"}
        # Making regex more general to check if ValueError is raised with *any* related message
        with self.assertRaisesRegex(ValueError, "Alpha Vantage API Info:"):
            fetch_alpha_vantage_data(params)

    @patch('utils.api_client.requests.get')
    @patch.dict(os.environ, {"ALPHA_VANTAGE_API_KEY": "TESTKEY123"})
    def test_fetch_data_api_information_premium_endpoint(self, mock_requests_get):
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {"Information": "Thank you for using Alpha Vantage! Our standard API call frequency is 5 calls per minute and 100 calls per day. Please visit https://www.alphavantage.co/premium/ if you would like to target a higher API call frequency. This endpoint is part of our premium plan, please visit ..."}
        mock_requests_get.return_value = mock_response

        params = {"function": "PREMIUM_FUNCTION", "symbol": "IBM"}
        with self.assertRaisesRegex(ValueError, "Alpha Vantage API Info: Thank you for using Alpha Vantage!"): # Check partial match
            fetch_alpha_vantage_data(params)


    @patch('utils.api_client.requests.get')
    @patch.dict(os.environ, {"ALPHA_VANTAGE_API_KEY": "TESTKEY123"})
    def test_fetch_data_http_error(self, mock_requests_get):
        mock_response = Mock()
        mock_response.status_code = 404 # Not Found
        mock_response.raise_for_status.side_effect = requests.exceptions.HTTPError("404 Client Error: Not Found for url")
        mock_requests_get.return_value = mock_response

        params = {"function": "TIME_SERIES_INTRADAY", "symbol": "IBM"}
        with self.assertRaises(requests.exceptions.HTTPError):
            fetch_alpha_vantage_data(params)

    @patch('utils.api_client.requests.get')
    @patch.dict(os.environ, {"ALPHA_VANTAGE_API_KEY": "TESTKEY123"})
    def test_fetch_data_network_error(self, mock_requests_get):
        mock_requests_get.side_effect = requests.exceptions.RequestException("Network error")

        params = {"function": "TIME_SERIES_INTRADAY", "symbol": "IBM"}
        with self.assertRaisesRegex(requests.exceptions.RequestException, "RequestException: Error connecting to Alpha Vantage: Network error"):
            fetch_alpha_vantage_data(params)

    @patch('utils.api_client.requests.get')
    @patch.dict(os.environ, {"ALPHA_VANTAGE_API_KEY": "TESTKEY123"})
    def test_fetch_data_non_error_information_message(self, mock_requests_get):
        # Test that an "Information" message that isn't a rate limit or premium endpoint error
        # does NOT raise a ValueError, but returns the data.
        mock_response = Mock()
        mock_response.status_code = 200
        mock_data_with_info = {
            "Information": "This is just some general info, not an error.",
            "Time Series (Daily)": {"2023-01-01": {"open": "100"}}
        }
        mock_response.json.return_value = mock_data_with_info
        mock_requests_get.return_value = mock_response

        params = {"function": "TIME_SERIES_DAILY", "symbol": "IBM"}
        result = fetch_alpha_vantage_data(params)
        self.assertEqual(result, mock_data_with_info)


if __name__ == '__main__':
    unittest.main()
