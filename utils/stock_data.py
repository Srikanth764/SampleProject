from .api_client import fetch_alpha_vantage_data, get_alpha_vantage_api_key # Added get_alpha_vantage_api_key for default

def get_historical_stock_data(stock_symbol: str, api_key: str = None, output_size: str = 'compact'):
    """
    Fetches historical daily stock data for a given symbol from Alpha Vantage.

    Args:
        stock_symbol (str): The stock symbol (e.g., "IBM", "AAPL").
        api_key (str, optional): The Alpha Vantage API key. If None, it will be fetched
                                 from environment variables.
        output_size (str, optional): The size of the output.
                                     'compact' returns the latest 100 data points.
                                     'full' returns the full-length time series.
                                     Defaults to 'compact'.

    Returns:
        dict: A dictionary containing the daily time series data,
              or an empty dictionary if an error occurs or data is not in the expected format.

    Raises:
        ValueError: If the API response does not contain the expected data format,
                    if the API key is missing, or if fetch_alpha_vantage_data raises an error.
        requests.exceptions.RequestException: If fetch_alpha_vantage_data raises a network error.
    """
    if not api_key:
        api_key = get_alpha_vantage_api_key() # Get key from env if not provided

    if not api_key:
         raise ValueError("API key must be provided either as an argument or set as ALPHA_VANTAGE_API_KEY environment variable.")

    params = {
        "function": "TIME_SERIES_DAILY_ADJUSTED",
        "symbol": stock_symbol,
        "outputsize": output_size
        # 'apikey' is now passed to fetch_alpha_vantage_data
    }

    try:
        # Pass the api_key to fetch_alpha_vantage_data
        data = fetch_alpha_vantage_data(params, api_key=api_key)

        if not data or "Time Series (Daily)" not in data:
            raise ValueError(f"Invalid or empty response received from Alpha Vantage for {stock_symbol}. "
                             "Expected 'Time Series (Daily)' key.")

        return data["Time Series (Daily)"]

    except ValueError as e:
        # Re-raise ValueError to be handled by the caller
        raise ValueError(f"Error processing stock data for {stock_symbol}: {e}")
    except Exception as e:
        # Catch any other unexpected errors from fetch_alpha_vantage_data or elsewhere
        # This includes requests.exceptions.RequestException
        raise Exception(f"An unexpected error occurred while fetching stock data for {stock_symbol}: {e}")


def get_news_sentiment(tickers: str = None, topics: str = None, api_key: str = None,
                       time_from: str = None, time_to: str = None,
                       sort: str = 'LATEST', limit: int = 50):
    """
    Fetches news and sentiment data for given tickers and/or topics from Alpha Vantage.

    Args:
        tickers (str, optional): A comma-separated string of stock tickers (e.g., "AAPL,MSFT").
                                 Required if topics is not provided.
        topics (str, optional): A comma-separated string of topics (e.g., "technology,ipo").
                                Required if tickers is not provided.
        api_key (str, optional): The Alpha Vantage API key. If None, it will be fetched
                                 from environment variables.
        time_from (str, optional): The start time for the news query (YYYYMMDDTHHMM).
        time_to (str, optional): The end time for the news query (YYYYMMDDTHHMM).
        sort (str, optional): The sort order for results. 'LATEST' or 'RELEVANCE'.
                              Defaults to 'LATEST'.
        limit (int, optional): The number of results to return (1-1000). Defaults to 50.

    Returns:
        list: A list of news sentiment data items (articles), or an empty list if an error occurs
              or data is not in the expected format.
              Each item is a dictionary representing an article/sentiment.

    Raises:
        ValueError: If neither tickers nor topics are provided, if the API key is missing,
                    if the API response is invalid, or if fetch_alpha_vantage_data raises an error.
        requests.exceptions.RequestException: If fetch_alpha_vantage_data raises a network error.
    """
    if not tickers and not topics:
        raise ValueError("Either 'tickers' or 'topics' must be provided.")

    resolved_api_key = api_key if api_key else get_alpha_vantage_api_key()
    if not resolved_api_key:
        raise ValueError("API key must be provided or set as ALPHA_VANTAGE_API_KEY environment variable.")

    params = {
        "function": "NEWS_SENTIMENT",
        "sort": sort,
        "limit": str(limit) # API expects limit as a string
    }
    if tickers:
        params["tickers"] = tickers
    if topics:
        params["topics"] = topics
    if time_from:
        params["time_from"] = time_from
    if time_to:
        params["time_to"] = time_to

    try:
        data = fetch_alpha_vantage_data(params, api_key=resolved_api_key)

        # Alpha Vantage returns 'items' which can be "0" if no results, or a list of articles in 'feed'.
        # It can also return an "Information" key for certain non-error messages.
        if not data:
             raise ValueError("No data received from Alpha Vantage.")

        if "Information" in data and data.get("feed") is None : # If only "Information" and no "feed"
            # This could be a valid scenario like "No articles found for <ticker>"
            # Or it could be an actual issue like a rate limit message not caught by fetch_alpha_vantage_data
            # For now, we'll treat it as "no articles found" and return empty list.
            # A more specific error from fetch_alpha_vantage_data for rate limits is preferred.
            print(f"Alpha Vantage Information: {data['Information']}") # Log it
            return []


        if "feed" not in data or not isinstance(data["feed"], list):
            # Check if 'items' is '0' which means no articles found.
            if data.get("items") == "0":
                return [] # No articles found is a valid empty result

            error_message = f"Invalid response structure from Alpha Vantage for news sentiment. Expected 'feed' key with a list."
            if "Error Message" in data:
                 error_message = f"Alpha Vantage API Error: {data['Error Message']}"
            raise ValueError(error_message)

        return data["feed"]

    except ValueError as e:
        raise ValueError(f"Error processing news sentiment data: {e}")
    except Exception as e:
        raise Exception(f"An unexpected error occurred while fetching news sentiment: {e}")
