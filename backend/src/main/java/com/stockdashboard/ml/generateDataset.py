import google.generativeai as genai
import pandas as pd
import json
import time
import os 

GEMINI_API_KEY = '' 
genai.configure(api_key=GEMINI_API_KEY)


try:
    with open('articles.txt', 'r', encoding='utf-8') as f:
        article_texts = [line.strip() for line in f if line.strip()]
except FileNotFoundError:
    print("Error: 'articles.txt' not found. Please create the file and add article text to it.")
    exit()

features_prompt = """
You are a fast financial data extractor. Analyze the provided article and return a clean JSON object containing EXACTLY these six attributes: "sentiment_magnitude", "financial_event_type", "outlook_type", "stock_market_reaction", "supply_chain_impact", "company_volatility".

The required formats are:
- "financial_event_type": A string from ["Earnings Report", "Product Launch", "Analyst Note", "Regulatory", "Merger/Acquisition", "General News"].
- "outlook_type": A string from ["Positive", "Negative", "Neutral"].
- "stock_market_reaction": A string from ["Positive Reaction", "Negative Reaction", "No Reaction Mentioned"].
- "supply_chain_impact": A string from ["Supply Chain Positive", "Supply Chain Negative", "Supply Chain Neutral/No Impact"].
- "company_volatility": A string from ["Increased Volatility", "Decreased Volatility", "Stable Volatility"].
- "sentiment_magnitude": A float from 0.0 to 1.0. This should be based on the sentiment of the article as well as the above traits

CRITICAL: Respond with ONLY the JSON object.

ARTICLE TEXT:
---
{article_text}
---
"""

score_prompt = """
You are an expert financial analyst. Your entire task is to read the following article and determine a single, final overall_sentiment_score. The score must be a float between -1.0 (extremely negative) and 1.0 (extremely positive).

CRITICAL: Respond with ONLY the numeric score.

ARTICLE TEXT:
---
{article_text}
---
"""
flashModel = genai.GenerativeModel('gemini-2.5-flash')
proModel = genai.GenerativeModel('gemini-2.5-pro')
all_data_records = []

print(f"Found {len(article_texts)} article(s) to process...")

for i, text in enumerate(article_texts):
    print(f"Processing article {i+1}...")
    try:
        flash_response = flashModel.generate_content(features_prompt.format(article_text=text))
        cleaned_json_string = flash_response.text.strip().replace('```json', '').replace('```', '')
        features_data = json.loads(cleaned_json_string)
        pro_response = proModel.generate_content(score_prompt.format(article_text=text))
        score_data = float(pro_response.text.strip())
        features_data['overall_sentiment_score'] = score_data
        all_data_records.append(features_data)
        time.sleep(2)
    except Exception as e:
        print(f"  !! ERROR processing article {i+1}: {e}")
df = pd.DataFrame(all_data_records)
csv_file_path = 'financial_sentiment_dataset.csv'
if os.path.exists(csv_file_path):
    df.to_csv(csv_file_path, mode='a', header=False, index=False)
else:
    df.to_csv(csv_file_path, mode='w', header=True, index=False)

print("\nData generation complete!")
print(f"Dataset with {len(df)} record(s) saved to {csv_file_path}")