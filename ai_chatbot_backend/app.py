from flask import Flask, request, jsonify
from transformers import BertTokenizerFast, BertForQuestionAnswering
from sklearn.feature_extraction.text import TfidfVectorizer
import torch

# Load tokenizer and model
tokenizer = BertTokenizerFast.from_pretrained("qa_model")
model = BertForQuestionAnswering.from_pretrained("qa_model")

# Load medical knowledge base
with open("medical_data.txt", "r", encoding="utf-8") as f:
    knowledge_sentences = [line.strip() for line in f if line.strip()]

# Prepare TF-IDF vectorizer
vectorizer = TfidfVectorizer().fit(knowledge_sentences)
knowledge_vectors = vectorizer.transform(knowledge_sentences)

# Flask app
app = Flask(__name__)

@app.route("/predict", methods=["POST"])
def predict():
    data = request.get_json()
    question = data.get("question", "")

    if not question:
        return jsonify({"error": "Question is required"}), 400

    # Use TF-IDF to find best matching context
    question_vec = vectorizer.transform([question])
    similarity_scores = (question_vec @ knowledge_vectors.T).toarray()[0]
    best_idx = similarity_scores.argmax()
    best_score = similarity_scores[best_idx]

    # Fallback if similarity is too low
    if best_score < 0.2:
        return jsonify({
            "answer": "If you are feeling unwell and unsure, it's always safest to speak with a medical professional for proper advice."
        })

    best_context = knowledge_sentences[best_idx]

    # Tokenize inputs
    inputs = tokenizer.encode_plus(question, best_context, return_tensors="pt", truncation=True)
    input_ids = inputs["input_ids"].tolist()[0]

    # Predict
    with torch.no_grad():
        outputs = model(**inputs)
        answer_start = torch.argmax(outputs.start_logits)
        answer_end = torch.argmax(outputs.end_logits) + 1
        answer_ids = input_ids[answer_start:answer_end]
        answer = tokenizer.convert_tokens_to_string(tokenizer.convert_ids_to_tokens(answer_ids))

    # Clean the answer
    answer = answer.replace("[SEP]", "").replace("?", "").replace("##", "").strip().capitalize()

    return jsonify({
        "answer": answer
    })

if __name__ == "__main__":
    app.run(debug=True)
