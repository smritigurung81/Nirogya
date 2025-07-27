import requests
import json
import time

# Configuration
BASE_URL = "http://127.0.0.1:5000"
TIMEOUT = 10

def print_header(title):
    """Print a formatted header"""
    print(f"\n{'='*60}")
    print(f"TESTING: {title}")
    print(f"{'='*60}")

def test_endpoint(endpoint, method="GET", data=None, description=""):
    """Test a specific endpoint and return response"""
    url = f"{BASE_URL}{endpoint}"
    
    try:
        print(f"\nTesting: {endpoint}")
        if description:
            print(f"  {description}")
        
        if method == "POST":
            response = requests.post(url, json=data, timeout=TIMEOUT)
        else:
            response = requests.get(url, timeout=TIMEOUT)
        
        print(f"  Status: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"  SUCCESS")
            return result
        else:
            print(f"  FAILED - Status {response.status_code}")
            print(f"  Response: {response.text}")
            return None
            
    except requests.exceptions.ConnectionError:
        print(f"  CONNECTION ERROR - Is the Flask server running?")
        return None
    except requests.exceptions.Timeout:
        print(f"  TIMEOUT - Server took too long to respond")
        return None
    except Exception as e:
        print(f"  ERROR: {e}")
        return None

def main():
    """Run all tests"""
    print_header("MEDICAL CHATBOT API TESTING")
    
    # Test 1: Check if server is running
    print_header("1. SERVER CONNECTIVITY")
    health_response = test_endpoint("/health", description="Checking if server is running")
    
    if not health_response:
        print("\nServer is not responding. Please:")
        print("  1. Make sure Flask app is running")
        print("  2. Check if it's running on http://127.0.0.1:5000")
        print("  3. Try: python app.py")
        return
    
    # Display system status
    print(f"\nSystem Status: {health_response.get('status', 'unknown')}")
    print(f"  Q&A Pairs: {health_response.get('loaded_qa_pairs', 0)}")
    print(f"  TF-IDF Ready: {health_response.get('vectorizer_ready', False)}")
    print(f"  QA Model: {health_response.get('qa_model_loaded', False)}")
    print(f"  Mode: {'Hybrid' if health_response.get('hybrid_mode') else 'TF-IDF Only'}")
    
    # Test 2: Debug information
    print_header("2. SYSTEM DEBUG INFO")
    debug_response = test_endpoint("/debug", description="Getting system debug information")
    
    if debug_response:
        print(f"  Directory: {debug_response.get('current_directory', 'unknown')}")
        print(f"  Text files: {debug_response.get('files_in_directory', [])}")
        print(f"  Total Q&A pairs: {debug_response.get('total_qa_pairs', 0)}")
        sample_questions = debug_response.get('sample_questions', [])
        if sample_questions:
            print(f"  First question: {sample_questions[0]}")
            print(f"  Sample questions loaded: {len(sample_questions)}")
    
    # Test 3: Built-in system test
    print_header("3. BUILT-IN SYSTEM TEST")
    test_response = test_endpoint("/test", description="Running built-in system test")
    
    if test_response:
        print(f"  Test Status: {test_response.get('status', 'unknown')}")
        if test_response.get('status') == 'success':
            print(f"  Test Question: {test_response.get('test_question', '')}")
            answer = test_response.get('answer', '')
            print(f"  Answer: {answer[:150]}{'...' if len(answer) > 150 else ''}")
        else:
            print(f"  ERROR: {test_response.get('message', 'Unknown error')}")
    
    # Test 4: Main prediction endpoint
    print_header("4. MAIN PREDICTION ENDPOINT TEST")
    
    test_cases = [
        {
            "request": {"question": "What are the symptoms of fever?"},
            "description": "Fever symptoms (should find exact match)"
        },
        {
            "request": {"question": "My heart rate is 58 bpm. Is that normal?"},
            "description": "Heart rate question (similarity matching)"
        },
        {
            "request": {"question": "What is normal blood pressure?"},
            "description": "Blood pressure question"
        },
        {
            "request": {"question": "My SpO2 is 92 percent is that okay?"},
            "description": "Oxygen level question"
        },
        {
            "request": {"question": "Is 90/60 low blood pressure?"},
            "description": "Specific BP reading question"
        },
        {
            "request": {"question": "Hello"},
            "description": "Greeting (non-medical intent)"
        }
    ]
    
    for i, test_case in enumerate(test_cases, 1):
        print(f"\nTest Case {i}: {test_case['description']}")
        print(f"  Question: {test_case['request']['question']}")
        
        response = test_endpoint("/predict", "POST", test_case['request'])
        
        if response and 'answer' in response:
            answer = response['answer']
            print(f"  ANSWER: {answer[:150]}{'...' if len(answer) > 150 else ''}")
        else:
            print(f"  NO VALID RESPONSE")
    
    # Test 5: Pure TF-IDF endpoint
    print_header("5. PURE TF-IDF RETRIEVAL TEST")
    
    retrieval_tests = [
        {"query": "What are the symptoms of headache?"},
        {"query": "fever symptoms"},
        {"query": "heart rate 58 bpm normal"}
    ]
    
    for i, test_data in enumerate(retrieval_tests, 1):
        print(f"\nRetrieval Test {i}: {test_data['query']}")
        response = test_endpoint("/predict_retrieval_only", "POST", test_data)
        
        if response and 'answer' in response:
            answer = response['answer']
            if answer == "Sorry, there was an error.":
                print(f"  ERROR RESPONSE: {answer}")
                print("  Check Flask console for detailed error messages")
            else:
                print(f"  TF-IDF SUCCESS: {answer[:120]}...")
        else:
            print(f"  NO RESPONSE")
    
    # Test 6: Alternative request formats
    print_header("6. ALTERNATIVE REQUEST FORMATS")
    
    alt_tests = [
        {"query": "What are normal SpO2 levels?"},  # Using 'query' instead of 'question'
        {"message": "Is my temperature of 99.5F a fever?"}  # Using 'message'
    ]
    
    for alt_test in alt_tests:
        field_name = list(alt_test.keys())[0]
        print(f"\nTesting format with '{field_name}' field:")
        response = test_endpoint("/predict", "POST", alt_test)
        if response and 'answer' in response:
            print(f"  SUCCESS: {response['answer'][:100]}...")
        else:
            print(f"  FAILED")
    
    # Final analysis
    print_header("FINAL SYSTEM ANALYSIS")
    
    if health_response:
        qa_pairs = health_response.get('loaded_qa_pairs', 0)
        tfidf_ready = health_response.get('vectorizer_ready', False)
        
        print(f"\nSYSTEM COMPONENTS:")
        if qa_pairs > 0:
            print(f"  [OK] Medical Data: {qa_pairs} Q&A pairs loaded")
        else:
            print(f"  [FAIL] Medical Data: No Q&A pairs loaded")
        
        if tfidf_ready:
            print(f"  [OK] TF-IDF System: Ready and functional")
        else:
            print(f"  [FAIL] TF-IDF System: Not initialized")
        
        if health_response.get('sklearn_available'):
            print(f"  [OK] Scikit-learn: Available")
        else:
            print(f"  [FAIL] Scikit-learn: Not available - run: pip install scikit-learn")
        
        print(f"\nOVERALL STATUS:")
        if qa_pairs > 0 and tfidf_ready:
            print(f"  [SUCCESS] Your AI model is working!")
            print(f"  [INFO] TF-IDF retrieval system is ready to answer medical questions")
            print(f"  [INFO] System can handle requests from your Android app")
        else:
            print(f"  [PROBLEM] System has issues that need to be fixed")
            
            if qa_pairs == 0:
                print(f"    - Check that medical_data.txt exists and has proper Q:/A: format")
            if not tfidf_ready:
                print(f"    - Install scikit-learn: pip install scikit-learn")
    
    print(f"\nTROUBLESHOOTING:")
    print(f"  - If you see 'Sorry, there was an error': Check Flask console output")
    print(f"  - If no Q&A pairs loaded: Verify medical_data.txt format and location")
    print(f"  - If TF-IDF failed: Run 'pip install scikit-learn numpy'")
    print(f"  - For Android app: Use the /predict endpoint with {{'question': 'your question'}}")
    
    print(f"\n{'='*60}")
    print("TESTING COMPLETE")
    print(f"{'='*60}")

if __name__ == "__main__":
    main()