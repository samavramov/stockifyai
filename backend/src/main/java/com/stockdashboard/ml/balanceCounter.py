import pandas as pd

try:
    df = pd.read_csv('financial_sentiment_dataset.csv')

    print("--- Dataset Balance ---")
    print("\nFinancial Event Type:")
    print(df['financial_event_type'].value_counts())

    print("\nOutlook Type:")
    print(df['outlook_type'].value_counts())

    print("\nStock Market Reaction:")
    print(df['stock_market_reaction'].value_counts())

    print("\nSupply Chain Impact:")
    print(df['supply_chain_impact'].value_counts())

    print("\nCompany Volatility:")
    print(df['company_volatility'].value_counts())

except FileNotFoundError:
    print("The file 'financial_sentiment_dataset.csv' was not found.")
    print("Please generate the dataset first before running this analysis.")
