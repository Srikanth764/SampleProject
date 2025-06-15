import os
import datetime
from flask import Flask, request, jsonify
from dotenv import load_dotenv
import requests # For requests.exceptions.RequestException

# Load environment variables from .env file
load_dotenv()

from utils import get_alpha_vantage_api_key, get_historical_stock_data, get_news_sentiment
from analysis import analyze_stock_data, suggest_option_strategy

app = Flask(__name__)

# Store the original main function if needed for CLI testing, or remove if API-only
def cli_main():
    print("Hello, world from CLI!")

    api_key = get_alpha_vantage_api_key()
    if not api_key:
        print("ALPHA_VANTAGE_API_KEY not found. Make sure to set it in your .env file.")
        return

    print("Alpha Vantage API Key found (CLI mode).")

    # --- Example usage of analyze_stock_data and suggest_option_strategy ---
    analysis_stock_symbol = "IBM" # Example: Microsoft
    print(f"\nPerforming analysis for {analysis_stock_symbol} (CLI mode)...")
    try:
        hist_data = get_historical_stock_data(stock_symbol=analysis_stock_symbol, output_size='compact')
        if not hist_data:
            print(f"Could not fetch historical data for {analysis_stock_symbol}. Skipping analysis.")
            return

        news_sent_data = get_news_sentiment(tickers=analysis_stock_symbol, limit=10)

        print(f"Historical data (latest date): {list(hist_data.keys())[0] if hist_data else 'N/A'}")
        print(f"News articles fetched: {len(news_sent_data)}")

        if hist_data:
            analysis_result = analyze_stock_data(historical_data=hist_data, news_sentiment_data=news_sent_data)

            print("\n--- Stock Analysis Result (CLI mode) ---")
            # ... (rest of the printing logic from original main)
            print(f"Summary: {analysis_result.get('analysis_summary')}")
            print("--- End of Analysis (CLI mode) ---")

            if analysis_result:
                risk = "moderate"
                print(f"\nSuggesting option strategies for {analysis_stock_symbol} with {risk} risk tolerance (CLI mode)...")
                option_suggestions = suggest_option_strategy(analysis_output=analysis_result, risk_tolerance=risk)
                # ... (rest of the printing logic for options)
                if option_suggestions:
                    print(f"Strategy: {option_suggestions[0].get('strategy')}") # Print first suggestion
                print("--- End of Option Suggestions (CLI mode) ---")
        else:
            print(f"Skipping analysis for {analysis_stock_symbol} due to missing historical data.")

    except ValueError as e:
        print(f"ValueError during analysis steps (CLI mode): {e}")
    except Exception as e:
        print(f"An unexpected error occurred during analysis steps (CLI mode): {e}")


@app.route('/v1/trading-suggestions', methods=['POST'])
def get_trading_suggestions():
    timestamp_start = datetime.datetime.utcnow()

    try:
        data = request.get_json()
    except Exception as e: # Catches werkzeug.exceptions.BadRequest if JSON is malformed/empty
        app.logger.warn(f"Failed to parse JSON payload: {e}")
        return jsonify({"error": "Invalid or malformed JSON payload."}), 400

    if data is None: # Explicitly check for None if request.get_json(silent=True) was used or if no JSON body.
        return jsonify({"error": "Empty JSON payload or incorrect Content-Type."}), 400

    stock_symbol = data.get('stock_symbol')
    if not stock_symbol:
        return jsonify({"error": "stock_symbol is required."}), 400

    # Optional parameters from API design (timeframe_days is not directly used by Alpha Vantage TIME_SERIES_DAILY_ADJUSTED)
    # output_size for historical data can be 'compact' or 'full'. Defaulting to 'compact'.
    output_size = data.get('output_size', 'compact')
    # news_limit for news sentiment. Defaulting to a reasonable number like 50.
    news_limit = data.get('news_limit', 50)
    risk_tolerance = data.get('risk_tolerance', 'moderate').lower()

    if risk_tolerance not in ["low", "moderate", "high"]:
        return jsonify({"error": "Invalid risk_tolerance. Must be 'low', 'moderate', or 'high'."}), 400

    # Timeframe days doesn't directly map to current functions but could be used for news fetching time range later
    # For now, we use news_limit for news items and output_size for historical data points.
    # timeframe_days = data.get('timeframe_days', 30) # Not used in current core logic directly

    try:
        # API key is fetched by the underlying functions if not passed directly
        # For security, ensure API key is not expected in request body in a real prod app

        # 1. Fetch historical data
        # The 'api_key=None' here relies on get_historical_stock_data and fetch_alpha_vantage_data
        # to pick up the key from the environment.
        historical_data = get_historical_stock_data(api_key=None, stock_symbol=stock_symbol, output_size=output_size)
        if not historical_data:
            # This case might occur if Alpha Vantage returns an empty response for a valid symbol,
            # or if the symbol is invalid (though AV often returns an error message for that).
            return jsonify({"error": f"Could not fetch historical data for {stock_symbol}. It might be an invalid symbol or no data available."}), 404

        # 2. Fetch news sentiment
        news_sentiment_data = get_news_sentiment(api_key=None, tickers=stock_symbol, limit=news_limit)
        # news_sentiment_data can be an empty list, which is acceptable.

        # 3. Perform analysis
        analysis_result = analyze_stock_data(historical_data=historical_data, news_sentiment_data=news_sentiment_data)
        if not analysis_result or analysis_result.get("current_price") == 0.0 or analysis_result.get("current_price") == "N/A":
             return jsonify({"error": f"Analysis could not be performed for {stock_symbol}. Current price might be unavailable."}), 500


        # 4. Suggest option strategies
        option_suggestions = suggest_option_strategy(analysis_output=analysis_result, risk_tolerance=risk_tolerance)

        # 5. Construct response
        response_data = {
            "request_id": None, # Placeholder for request ID generation if needed
            "stock_symbol": stock_symbol,
            "current_price": analysis_result.get("current_price"),
            "analysis_summary": analysis_result.get("analysis_summary"),
            "trend_analysis": analysis_result.get("trend_analysis"),
            "sentiment_analysis": analysis_result.get("sentiment_analysis"),
            "predicted_price_targets": { # As per API design
                "short_term_target": analysis_result.get("predicted_short_term_target"),
                "change_percent": analysis_result.get("predicted_change_percent")
            },
            "suggested_options_strategies": option_suggestions, # Changed key to match API design
            "data_sources": [
                {"name": "Alpha Vantage", "type": "Market Data & News Sentiment API"}
            ],
            "timestamp": timestamp_start.isoformat() + "Z",
            "disclaimer": "This information is for educational purposes only and not financial advice. Options trading involves significant risk."
        }
        return jsonify(response_data), 200

    except ValueError as ve:
        # This can be from our internal validations (e.g., API key missing, bad params to functions)
        # or from Alpha Vantage returning an error message that we parse into a ValueError.
        app.logger.error(f"ValueError for symbol {stock_symbol}: {str(ve)}")
        # Check if it's an API error message we raised
        if "Alpha Vantage API Error" in str(ve) or "Alpha Vantage API Info" in str(ve):
            return jsonify({"error": f"External API error: {str(ve)}"}), 502 # Bad Gateway
        return jsonify({"error": f"Input or data processing error: {str(ve)}"}), 400 # Bad Request for other ValueErrors

    except requests.exceptions.RequestException as re: # More specific than general Exception for network issues
        app.logger.error(f"RequestException for symbol {stock_symbol}: {str(re)}")
        return jsonify({"error": f"Could not connect to external data provider: {str(re)}"}), 503 # Service Unavailable

    except Exception as e:
        app.logger.error(f"Unexpected error for symbol {stock_symbol}: {str(e)}", exc_info=True)
        return jsonify({"error": "An unexpected internal server error occurred."}), 500


if __name__ == '__main__':
    # You can choose to run cli_main() for testing or app.run()
    # For this task, we are implementing the Flask API, so app.run() is primary.
    # To test CLI: cli_main()
    app.run(debug=True, host='0.0.0.0', port=5000)
