# Python Project Setup

This project is structured with a main application file (`app.py`), a directory for utility functions (`utils`), and a `requirements.txt` file.

## Virtual Environment

It is highly recommended to use a virtual environment for this project to manage dependencies and isolate the project from your global Python installation.

To create and activate a virtual environment:

1.  **Create the virtual environment:**
    ```bash
    python -m venv venv
    ```

2.  **Activate the virtual environment:**

    *   On Windows:
        ```bash
        .\venv\Scripts\activate
        ```
    *   On macOS and Linux:
        ```bash
        source venv/bin/activate
        ```

3.  **Install dependencies:**
    Once the virtual environment is activated, install the required packages:
    ```bash
    pip install -r requirements.txt
    ```

## Running the Application (Flask API)

The application now runs as a Flask web API.

1.  **Set up your `.env` file:**
    Ensure you have a `.env` file in the project root (you can copy and rename `.env.example`). This file must contain your Alpha Vantage API key:
    ```dotenv
    ALPHA_VANTAGE_API_KEY="YOUR_ACTUAL_API_KEY"
    ```
    The application uses `python-dotenv` to load this key.

2.  **Run the Flask development server:**
    ```bash
    python app.py
    ```
    This will typically start the server on `http://127.0.0.1:5000/`. The console output will confirm the address.
    The API is served with `debug=True`, which is suitable for development but should be changed for production.

## API Endpoint

The main (and currently only) endpoint is:

### `POST /v1/trading-suggestions`

This endpoint expects a JSON payload and returns trading suggestions based on stock analysis and news sentiment.

**Request Body (JSON):**

```json
{
    "stock_symbol": "MSFT",
    "risk_tolerance": "moderate",
    "output_size": "compact",
    "news_limit": 50
}
```

*   `stock_symbol` (string, required): The stock ticker symbol (e.g., "AAPL", "MSFT").
*   `risk_tolerance` (string, optional, default: "moderate"): User's risk appetite. Accepted values: "low", "moderate", "high".
*   `output_size` (string, optional, default: "compact"): For historical data. "compact" for latest 100 points, "full" for complete history.
*   `news_limit` (integer, optional, default: 50): Number of news articles to fetch for sentiment analysis.

**Success Response (200 OK):**

A JSON object containing the analysis and suggestions. See the API design documentation for the full structure. Example snippet:

```json
{
    "stock_symbol": "MSFT",
    "current_price": 330.50,
    "analysis_summary": "Stock shows a slight upward trend with neutral news sentiment...",
    "predicted_price_targets": {
        "short_term_target": 335.00,
        "change_percent": 1.36
    },
    "suggested_options_strategies": [
        {
            "strategy": "Buy Call",
            "option_type": "Call",
            "action": "Buy",
            "strike_price": 335.00,
            "expiration_date": "YYYY-MM-DD",
            "rationale": "Based on positive price prediction...",
            "confidence_level": "Medium"
        }
    ],
    "timestamp": "YYYY-MM-DDTHH:MM:SS.ffffffZ"
    // ... other fields like trend_analysis, sentiment_analysis, data_sources, disclaimer
}
```

**Error Responses:**

*   `400 Bad Request`: If the JSON payload is invalid, `stock_symbol` is missing, or `risk_tolerance` is invalid.
*   `404 Not Found`: If historical data for the stock symbol cannot be found.
*   `500 Internal Server Error`: For unexpected server-side issues.
*   `502 Bad Gateway`: If there's an issue with the external Alpha Vantage API.
*   `503 Service Unavailable`: If the application cannot connect to the external data provider.


## Running the Original CLI (Optional)

The original `main()` function, which prints analysis to the console, has been renamed to `cli_main()` in `app.py`. If you wish to run this version for testing:
1. Open `app.py`.
2. At the very bottom, change `app.run(debug=True, host='0.0.0.0', port=5000)` to `cli_main()`.
3. Then run `python app.py` from your terminal.
4. Remember to change it back to `app.run(...)` to use the Flask API.

Make sure to create a `.env` file in the root directory (you can copy and rename the `.env.example` file). Add your Alpha Vantage API key to this file:

```dotenv
ALPHA_VANTAGE_API_KEY="YOUR_ACTUAL_API_KEY"
```
The application uses `python-dotenv` to load this key from the `.env` file.
See `.env.example` for the expected structure.

## Running Tests

Unit tests are located in the `tests/` directory and can be run using Python's `unittest` module.

1.  **Ensure your virtual environment is activated** and dependencies (including any test-specific ones, though none are special for now) are installed.
2.  **Run tests from the project root directory:**
    ```bash
    python -m unittest discover -s tests
    ```
    Or, to run a specific test file:
    ```bash
    python -m unittest tests.test_api_client
    ```
    (Replace `test_api_client` with the specific file if needed, without the `.py` extension).
