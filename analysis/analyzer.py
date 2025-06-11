import datetime
from datetime import timedelta # Added for expiration date calculation

def analyze_stock_data(historical_data: dict, news_sentiment_data: list) -> dict:
    """
    Performs a basic analysis of stock data based on historical trends and news sentiment.

    Args:
        historical_data (dict): Historical stock data from get_historical_stock_data.
                                (Assumes 'Time Series (Daily)' format from Alpha Vantage).
        news_sentiment_data (list): News sentiment data from get_news_sentiment.
                                    (Assumes a list of articles with sentiment scores).

    Returns:
        dict: A dictionary containing the analysis results.
    """

    # --- 1. Current Price ---
    latest_date_str = "Unknown" # Default to Unknown
    current_price = 0.0
    if historical_data:
        try:
            # Sort dates to find the most recent one
            sorted_dates = sorted(historical_data.keys(), reverse=True)
            if sorted_dates:
                latest_date_str = sorted_dates[0] # Overwrite if data found
                current_price = float(historical_data[latest_date_str].get("4. close", 0.0))
            # If sorted_dates is empty (e.g. historical_data was there but malformed), latest_date_str remains "Unknown"
        except Exception as e:
            print(f"Error extracting current price: {e}")
            latest_date_str = "Unknown" # Explicitly set on error too

    # --- 2. Historical Trend (Placeholder) ---
    trend_direction = "FLAT"
    trend_details = "Trend analysis not fully implemented."

    # Basic Trend: Compare last 2 available data points if possible
    if historical_data and len(historical_data) >= 2:
        try:
            sorted_dates = sorted(historical_data.keys(), reverse=True)
            price_latest = float(historical_data[sorted_dates[0]].get("4. close", 0.0))
            price_previous = float(historical_data[sorted_dates[1]].get("4. close", 0.0))

            if price_latest > price_previous:
                trend_direction = "UP"
                trend_details = "Price increased from previous day."
            elif price_latest < price_previous:
                trend_direction = "DOWN"
                trend_details = "Price decreased from previous day."
            else:
                trend_direction = "FLAT"
                trend_details = "Price remained same as previous day."
        except Exception as e:
            print(f"Error calculating basic trend: {e}")


    # --- 3. News Sentiment Score (Placeholder) ---
    sentiment_score_avg = 0.0
    sentiment_label = "NEUTRAL"
    sentiment_details = "Sentiment analysis not fully implemented." # Default if all else fails (should be overwritten)
    num_articles_with_sentiment = 0
    total_sentiment_score = 0.0 # Initialize here so it's defined for the final if/else

    if news_sentiment_data:
        for article in news_sentiment_data:
            # Using 'overall_sentiment_score' if available
            article_score = article.get('overall_sentiment_score')
            if article_score is not None:
                try:
                    total_sentiment_score += float(article_score)
                    num_articles_with_sentiment += 1
                except ValueError:
                    print(f"Warning: Could not parse sentiment score: {article_score}")

    # This logic is now OUTSIDE and AFTER the 'if news_sentiment_data:' block
    if num_articles_with_sentiment > 0:
        sentiment_score_avg = total_sentiment_score / num_articles_with_sentiment
        if sentiment_score_avg > 0.15: # Alpha Vantage scores range roughly -1 to 1
            sentiment_label = "POSITIVE"
        elif sentiment_score_avg < -0.15:
            sentiment_label = "NEGATIVE"
        else:
            sentiment_label = "NEUTRAL" # Default is already NEUTRAL, but explicit if score is between -0.15 and 0.15
        sentiment_details = f"Average sentiment score of {sentiment_score_avg:.2f} from {num_articles_with_sentiment} articles."
    else:
        # This will be hit if news_sentiment_data was empty OR if it had items but none with scores.
        sentiment_label = "NEUTRAL" # Ensure label is neutral
        sentiment_score_avg = 0.0   # Ensure score is 0.0
        sentiment_details = "No valid sentiment scores found in news data."


    # --- 4. Combined Analysis & Prediction (Placeholder) ---
    predicted_target_change_percent = 0.0
    analysis_summary = "Combined analysis not fully implemented."

    if trend_direction == "UP" and sentiment_label == "POSITIVE":
        predicted_target_change_percent = 0.03 # +3%
        analysis_summary = "Stock shows an upward trend with positive news sentiment, suggesting a potential short-term price increase."
    elif trend_direction == "DOWN" and sentiment_label == "NEGATIVE":
        predicted_target_change_percent = -0.03 # -3%
        analysis_summary = "Stock shows a downward trend with negative news sentiment, suggesting a potential short-term price decrease."
    elif trend_direction == "UP" and sentiment_label == "NEGATIVE":
        analysis_summary = "Upward trend but negative news sentiment warrant caution. Potential volatility."
        predicted_target_change_percent = 0.005 # Slight increase due to trend
    elif trend_direction == "DOWN" and sentiment_label == "POSITIVE":
        analysis_summary = "Downward trend despite positive news. Market may be reacting to other factors or news effect delayed."
        predicted_target_change_percent = -0.005 # Slight decrease due to trend
    else: # Other combinations (FLAT trend, NEUTRAL sentiment etc.)
        analysis_summary = "Mixed signals. Trend is flat or sentiment is neutral. Hold recommended."
        predicted_target_change_percent = 0.0

    predicted_short_term_target = current_price * (1 + predicted_target_change_percent) if current_price > 0 else 0.0

    return {
        "current_price": current_price,
        "latest_data_date": latest_date_str,
        "trend_analysis": {"direction": trend_direction, "details": trend_details},
        "sentiment_analysis": {"score": round(sentiment_score_avg, 4), "label": sentiment_label, "details": sentiment_details},
        "predicted_change_percent": round(predicted_target_change_percent * 100, 2),
        "predicted_short_term_target": round(predicted_short_term_target, 2) if predicted_short_term_target > 0 else "N/A",
        "analysis_summary": analysis_summary,
    }


def suggest_option_strategy(analysis_output: dict, risk_tolerance: str = "moderate") -> list:
    """
    Suggests a basic option strategy based on the analysis output and risk tolerance.

    Args:
        analysis_output (dict): The output from analyze_stock_data.
        risk_tolerance (str): "low", "moderate", or "high".

    Returns:
        list: A list of suggested option strategies, or an empty list.
    """
    suggestions = []

    current_price = analysis_output.get("current_price", 0.0)
    # Convert percentage string (e.g., "3.0") to float (e.g., 0.03)
    predicted_change_percent_val = analysis_output.get("predicted_change_percent", 0.0)

    # Ensure current_price is a float, as it might be "N/A" or other string
    if not isinstance(current_price, (int, float)) or current_price <= 0:
        return [{"strategy": "Hold", "rationale": "Cannot determine strategy due to invalid current price.", "confidence_level": "Low"}]


    # Placeholder for expiration date (e.g., 30-45 days out)
    # This needs to be made more sophisticated later (e.g., find actual exchange-traded expirations)
    try:
        latest_data_date_str = analysis_output.get("latest_data_date")
        if latest_data_date_str and latest_data_date_str != "Unknown":
            base_date = datetime.datetime.strptime(latest_data_date_str, "%Y-%m-%d").date()
        else:
            base_date = datetime.date.today() # Fallback to today if date is unknown
        expiration_date = (base_date + timedelta(days=40)).strftime("%Y-%m-%d")
    except ValueError: # Handle cases where date parsing might fail
        base_date = datetime.date.today()
        expiration_date = (base_date + timedelta(days=40)).strftime("%Y-%m-%d")


    confidence = "Medium" # Default, can be adjusted
    if abs(predicted_change_percent_val) > 5: # Stronger prediction
        confidence = "High"
    elif abs(predicted_change_percent_val) < 1: # Weaker prediction
        confidence = "Low"

    strategy_details = {
        "option_type": None,
        "action": "Buy", # Defaulting to Buy for simple strategies
        "strike_price": None,
        "expiration_date": expiration_date,
        "rationale": "",
        "confidence_level": confidence
    }

    # Simplified strike calculation factor based on risk tolerance
    otm_factor_call = 1.02 # Default for moderate (2% OTM)
    otm_factor_put = 0.98  # Default for moderate (2% OTM)

    if risk_tolerance == "low":
        otm_factor_call = 1.005 # Closer to ATM (0.5% OTM)
        otm_factor_put = 0.995  # Closer to ATM (0.5% OTM)
    elif risk_tolerance == "high":
        otm_factor_call = 1.05  # Further OTM (5% OTM)
        otm_factor_put = 0.95   # Further OTM (5% OTM)


    if predicted_change_percent_val > 1.0: # Prediction of > 1% increase
        strategy_details["strategy"] = "Buy Call"
        strategy_details["option_type"] = "Call"
        # Strike price slightly OTM, adjusted by risk tolerance
        # For calls, OTM is above current price
        strike = current_price * otm_factor_call
        strategy_details["strike_price"] = round(strike, 2) # Round to 2 decimal places
        strategy_details["rationale"] = (
            f"Based on positive price prediction ({predicted_change_percent_val}%). "
            f"Suggested strike is slightly Out-of-the-Money for {risk_tolerance} risk."
        )
        suggestions.append(strategy_details.copy())

    elif predicted_change_percent_val < -1.0: # Prediction of > 1% decrease
        strategy_details["strategy"] = "Buy Put"
        strategy_details["option_type"] = "Put"
        # Strike price slightly OTM, adjusted by risk tolerance
        # For puts, OTM is below current price
        strike = current_price * otm_factor_put
        strategy_details["strike_price"] = round(strike, 2)
        strategy_details["rationale"] = (
            f"Based on negative price prediction ({predicted_change_percent_val}%). "
            f"Suggested strike is slightly Out-of-the-Money for {risk_tolerance} risk."
        )
        suggestions.append(strategy_details.copy())

    else: # Prediction between -1% and 1% (close to zero)
        suggestions.append({
            "strategy": "Hold / Neutral",
            "option_type": None,
            "action": None,
            "strike_price": None,
            "expiration_date": None,
            "rationale": (
                f"Price prediction ({predicted_change_percent_val}%) is close to zero. "
                f"Consider holding or a neutral options strategy (e.g., Iron Condor - not detailed here)."
            ),
            "confidence_level": "Low" # Confidence in a specific directional trade is low
        })

    return suggestions
