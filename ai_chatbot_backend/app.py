import os
import re
import torch
import numpy as np
from flask import Flask, request, jsonify
# from flask_cors import CORS  # REMOVED - not needed for emulator
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from transformers import AutoTokenizer, AutoModelForQuestionAnswering

app = Flask(__name__)


# Global variables
medical_data = []
tfidf_vectorizer = None
tfidf_matrix = None
qa_tokenizer = None
qa_model = None

def load_medical_data():
    """Load medical Q&A data from text file"""
    global medical_data
    
    print("\n" + "="*50)
    print("LOADING MEDICAL DATA")
    print("="*50)
    
    try:
        if os.path.exists('medical_data.txt'):
            print("[INFO] Found medical_data.txt")
            with open('medical_data.txt', 'r', encoding='utf-8') as file:
                content = file.read().strip()
                print(f"[DEBUG] File loaded, {len(content.splitlines())} lines")
                
                # Parse Q&A pairs
                pairs = content.split('\n\n')  # Assuming Q&A pairs are separated by double newlines
                
                for pair in pairs:
                    if pair.strip():
                        lines = pair.strip().split('\n')
                        if len(lines) >= 2:
                            question = lines[0].replace('Q:', '').replace('Question:', '').strip()
                            answer = '\n'.join(lines[1:]).replace('A:', '').replace('Answer:', '').strip()
                            
                            if question and answer:
                                medical_data.append({
                                    'question': question,
                                    'answer': answer
                                })
                
                print(f"[SUCCESS] Loaded {len(medical_data)} Q&A pairs")
                if medical_data:
                    print(f"[DEBUG] Sample question: {medical_data[0]['question'][:50]}...")
                    print(f"[DEBUG] Sample answer: {medical_data[0]['answer'][:50]}...")
                
        else:
            print("[ERROR] medical_data.txt not found!")
            return False
            
    except Exception as e:
        print(f"[ERROR] Error loading medical data: {e}")
        return False
    
    return len(medical_data) > 0

def initialize_tfidf():
    """Initialize TF-IDF vectorizer and create question vectors"""
    global tfidf_vectorizer, tfidf_matrix
    
    print("\n" + "="*50)
    print("INITIALIZING TF-IDF")
    print("="*50)
    
    try:
        if not medical_data:
            print("[ERROR] No medical data available for TF-IDF")
            return False
        
        questions = [qa['question'] for qa in medical_data]
        print(f"[DEBUG] Creating TF-IDF vectorizer for {len(questions)} questions")
        
        tfidf_vectorizer = TfidfVectorizer(
            lowercase=True,
            stop_words='english',
            ngram_range=(1, 2),
            max_features=1000
        )
        
        tfidf_matrix = tfidf_vectorizer.fit_transform(questions)
        print("[SUCCESS] TF-IDF initialized")
        print(f"[DEBUG] Vocabulary size: {len(tfidf_vectorizer.vocabulary_)}")
        print(f"[DEBUG] Vector shape: {tfidf_matrix.shape}")
        
        return True
        
    except Exception as e:
        print(f"[ERROR] Error initializing TF-IDF: {e}")
        return False

def load_qa_model():
    """Load the QA model and tokenizer"""
    global qa_tokenizer, qa_model
    
    try:
        model_path = "qa_model"
        if os.path.exists(model_path):
            print(f"[INFO] Loading QA model from {model_path}")
            qa_tokenizer = AutoTokenizer.from_pretrained(model_path)
            qa_model = AutoModelForQuestionAnswering.from_pretrained(model_path)
            print("[SUCCESS] QA model loaded successfully")
            return True
        else:
            print(f"[WARNING] QA model not found at {model_path}")
            return False
    except Exception as e:
        print(f"[ERROR] Error loading QA model: {e}")
        return False

def is_medical_question(question):
    """Check if the question is medical-related"""
    medical_keywords = [
        'symptoms', 'fever', 'headache', 'pain', 'blood', 'pressure', 'heart', 'rate',
        'sugar', 'diabetes', 'medicine', 'drug', 'doctor', 'hospital', 'health',
        'sick', 'disease', 'treatment', 'cure', 'medication', 'bpm', 'temperature',
        'spo2', 'oxygen', 'breathing', 'chest', 'stomach', 'nausea', 'vomiting',
        'dizziness', 'fatigue', 'tired', 'sleep', 'insomnia', 'anxiety', 'stress'
    ]
    
    question_lower = question.lower()
    return any(keyword in question_lower for keyword in medical_keywords)

def clean_answer(answer, question):
    """Simple function to remove repeated question from answer"""
    if not answer:
        return answer
    
    # Remove question mark and common question words
    answer = answer.strip()
    
    # If answer starts with the question, remove it
    if '?' in answer:
        question_part = answer.split('?')[0] + '?'
        if len(question_part) < len(answer) * 0.4:  # Question is less than 40% of response
            answer = answer.split('?', 1)[1].strip()
    
    # Remove common question starters if they appear at the beginning
    question_starters = [
        'what are the symptoms of',
        'what are symptoms of', 
        'what is normal',
        'what is',
        'what are',
        'how to',
        'why is',
        'when should',
        'my heart rate is',
        'my blood pressure is',
        'is 90/60 low blood pressure',
        'is that okay'
    ]
    
    answer_lower = answer.lower()
    for starter in question_starters:
        if answer_lower.startswith(starter):
            answer = answer[len(starter):].strip()
            break
    
    # Remove leading punctuation
    answer = answer.lstrip('?.,!:').strip()
    
    # Capitalize first letter
    if answer and answer[0].islower():
        answer = answer[0].upper() + answer[1:]
    
    return answer

def get_response(question):
    """Get response from the medical chatbot with cleaned output"""
    try:
        print(f"[DEBUG] Processing question: '{question}'")
        
        # Check if it's a medical question
        if not is_medical_question(question):
            return "Hello! I'm MediBot, here to help with health-related questions."
        
        raw_response = ""
        
        # Try TF-IDF first
        if tfidf_vectorizer and tfidf_matrix is not None:
            question_vector = tfidf_vectorizer.transform([question])
            similarities = cosine_similarity(question_vector, tfidf_matrix).flatten()
            best_match_idx = similarities.argmax()
            best_score = similarities[best_match_idx]
            
            if best_score > 0.3:  # TF-IDF threshold
                print(f"[DEBUG] Best TF-IDF match score: {best_score:.3f}")
                print("[DEBUG] Using TF-IDF retrieval answer")
                raw_response = medical_data[best_match_idx]['answer']
            else:
                # Use QA model if available
                if qa_model and qa_tokenizer:
                    print("[DEBUG] Using QA model answer")
                    context = " ".join([qa['question'] + " " + qa['answer'] for qa in medical_data])
                    inputs = qa_tokenizer(question, context, return_tensors="pt", max_length=512, truncation=True)
                    
                    with torch.no_grad():
                        outputs = qa_model(**inputs)
                        start_idx = torch.argmax(outputs.start_logits)
                        end_idx = torch.argmax(outputs.end_logits)
                        
                        if end_idx >= start_idx:
                            answer_tokens = inputs['input_ids'][0][start_idx:end_idx+1]
                            raw_response = qa_tokenizer.decode(answer_tokens, skip_special_tokens=True)
                        else:
                            raw_response = "I'm not sure about that. Please consult a healthcare professional."
                else:
                    raw_response = "I'm not sure about that. Please consult a healthcare professional."
        else:
            raw_response = "System not properly initialized. Please try again."
        
        # Clean the response before returning
        cleaned_response = clean_answer(raw_response, question)
        return cleaned_response
        
    except Exception as e:
        print(f"[ERROR] Error processing question: {e}")
        return "I'm having trouble processing your question. Please try again or consult a healthcare professional."

# Routes
@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    status = {
        "status": "healthy",
        "qa_pairs": len(medical_data),
        "tfidf_ready": tfidf_matrix is not None,
        "qa_model": qa_model is not None,
        "mode": "Hybrid" if qa_model else "TF-IDF Only"
    }
    return jsonify(status)

@app.route('/debug', methods=['GET'])
def debug_info():
    """Debug information endpoint"""
    current_dir = os.getcwd()
    files = [f for f in os.listdir(current_dir) if f.endswith('.txt')]
    
    debug_data = {
        "directory": current_dir,
        "text_files": files,
        "medical_data_loaded": len(medical_data),
        "sample_questions": [qa['question'] for qa in medical_data[:5]] if medical_data else []
    }
    return jsonify(debug_data)

@app.route('/test', methods=['GET'])
def test_system():
    """Test the system with a sample question"""
    test_question = "What are the symptoms of fever?"
    answer = get_response(test_question)
    
    return jsonify({
        "status": "success",
        "test_question": test_question,
        "answer": answer
    })

@app.route('/predict', methods=['POST'])
def predict():
    try:
        print("\n[API] /predict called")
        data = request.get_json()
        print(f"[API] Request data: {data}")
        
        # Handle different request formats
        question = data.get('question') or data.get('query') or data.get('message', '')
        print(f"[API] Extracted question: '{question}'")
        
        if not question.strip():
            return jsonify({"error": "Please provide a question"}), 400
        
        # Check if it's a non-medical question
        if not is_medical_question(question.strip()):
            non_medical_response = "Hello! I'm MediBot, here to help with health-related questions."
            print(f"[API] Non-medical response: '{non_medical_response}'")
            return jsonify({"answer": non_medical_response})
        
        # Get the raw answer
        raw_answer = get_response(question.strip())
        
        # Clean the answer to remove repeated question
        clean_answer_text = clean_answer(raw_answer, question)
        
        print(f"[API] Final answer: '{clean_answer_text[:80]}...'")
        
        return jsonify({"answer": clean_answer_text})
        
    except Exception as e:
        print(f"[API] Error in /predict: {e}")
        return jsonify({"error": "Internal server error"}), 500

@app.route('/predict_retrieval_only', methods=['POST'])
def predict_retrieval_only():
    """Endpoint for testing TF-IDF retrieval only"""
    try:
        data = request.get_json()
        question = data.get('query', data.get('question', ''))
        
        print(f"[API] /predict_retrieval_only: '{question}'")
        
        if not question.strip():
            return jsonify({"error": "Please provide a question"}), 400
        
        if tfidf_vectorizer and tfidf_matrix is not None:
            question_vector = tfidf_vectorizer.transform([question])
            similarities = cosine_similarity(question_vector, tfidf_matrix).flatten()
            best_match_idx = similarities.argmax()
            best_score = similarities[best_match_idx]
            
            print(f"[API] Best match score: {best_score:.3f}")
            print(f"[API] Matched question: '{medical_data[best_match_idx]['question']}'")
            
            answer = medical_data[best_match_idx]['answer']
            cleaned_answer = clean_answer(answer, question)
            
            return jsonify({
                "answer": cleaned_answer,
                "confidence": float(best_score),
                "matched_question": medical_data[best_match_idx]['question']
            })
        else:
            return jsonify({"error": "TF-IDF system not initialized"}), 500
            
    except Exception as e:
        print(f"[API] Error in /predict_retrieval_only: {e}")
        return jsonify({"error": "Internal server error"}), 500

def initialize_system():
    """Initialize the complete system"""
    print("STARTING MEDIBOT INITIALIZATION...\n")
    
    # Load medical data
    if not load_medical_data():
        print("[FATAL] Failed to load medical data")
        return False
    
    # Initialize TF-IDF
    if not initialize_tfidf():
        print("[FATAL] Failed to initialize TF-IDF")
        return False
    
    # Load QA model (optional)
    load_qa_model()
    
    print("\n" + "="*50)
    print("SYSTEM STATUS")
    print("="*50)
    print(f"Medical Data: LOADED ({len(medical_data)} pairs)")
    print(f"TF-IDF System: READY")
    print(f"QA Model: {'LOADED' if qa_model else 'NOT AVAILABLE'}")
    print(f"MODE: {'HYBRID (TF-IDF + QA Model)' if qa_model else 'TF-IDF ONLY'}")
    print("="*50)
    
    return True

if __name__ == '__main__':
    # Check for required libraries
    try:
        import sklearn
        print("[INFO] Scikit-learn available")
    except ImportError:
        print("[ERROR] Scikit-learn not found. Install with: pip install scikit-learn")
        exit(1)
    
    try:
        import transformers
        print("[INFO] Transformers available")
    except ImportError:
        print("[WARNING] Transformers not found. QA model will not be available.")
    
    # Initialize the system
    if initialize_system():
        print("\nStarting Flask server...")
        print("System ready for requests!")
        app.run(host='0.0.0.0', port=5000, debug=True)
    else:
        print("[FATAL] System initialization failed!")
        exit(1)