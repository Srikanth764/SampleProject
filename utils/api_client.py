import os
import requests
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

def get_alpha_vantage_api_key():
    """
    Retrieves the Alpha Vantage API key from the environment variable.

    Returns:
        str: The API key, or None if not found.
    """
    return os.getenv("ALPHA_VANTAGE_API_KEY")

def fetch_alpha_vantage_data(params: dict, api_key: str = None):
    """
    Fetches data from the Alpha Vantage API.

    Args:
        params (dict): A dictionary of query parameters for the Alpha Vantage API.
        api_key (str, optional): The Alpha Vantage API key. If not provided,
                                 it will be fetched using get_alpha_vantage_api_key().

    Returns:
        dict: The JSON response data from the API, or None if an error occurs.
              In case of an API error message, it returns the error dictionary.

    Raises:
        requests.exceptions.RequestException: For network-related errors.
        ValueError: If the API returns a non-200 status code, an error message,
                    or if the API key is not found.
    """
    key_to_use = api_key if api_key else get_alpha_vantage_api_key()

    if not key_to_use:
        raise ValueError("Alpha Vantage API key not provided and not found in environment variables.")

    params_with_key = params.copy()
    params_with_key['apikey'] = key_to_use

    base_url = "https://www.alphavantage.co/query"

    try:
        response = requests.get(base_url, params=params_with_key)
        response.raise_for_status()  # Raises HTTPError for bad responses (4XX or 5XX)

        data = response.json()

        if "Error Message" in data:
            raise ValueError(f"Alpha Vantage API Error: {data['Error Message']}")
        if "Information" in data: # Specific check for rate limit or other info messages
            info_text_lower = data["Information"].lower()
            # Check if it's truly an error or just an informational message that implies no data
            if "api call frequency" in info_text_lower or \
               "premium endpoint" in info_text_lower or \
               "our premium plan" in info_text_lower:
                raise ValueError(f"Alpha Vantage API Info: {data['Information']}")

        return data

    except requests.exceptions.HTTPError as e: # Catch HTTPError before RequestException
        # Let HTTPError propagate as is, or handle specifically if needed
        raise e
    except requests.exceptions.RequestException as e:
        # Handle other network errors, timeouts, etc.
        # Avoid re-wrapping HTTPError here
        raise requests.exceptions.RequestException(f"RequestException: Error connecting to Alpha Vantage: {e}")
    except ValueError as e:
        # Handle JSON parsing errors or specific API errors raised above
        raise e
