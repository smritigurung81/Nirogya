package com.example.nirogya;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.nirogya.adapters.ChatAdapter;
import com.example.nirogya.models.ChatMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatbotActivity extends AppCompatActivity {

    private static final String TAG = "ChatbotActivity";
    private static final String BASE_URL = "http://10.0.2.2:5000"; // For emulator

    private RecyclerView rvChat;
    private EditText etUserInput;
    private ImageButton btnSend;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        initViews();
        setupRecyclerView();
        initRequestQueue();
        addWelcomeMessage();
        setupClickListeners();

        Log.d(TAG, "ChatbotActivity initialized");
    }

    private void initViews() {
        rvChat = findViewById(R.id.rvChat);
        etUserInput = findViewById(R.id.etUserInput);
        btnSend = findViewById(R.id.btnSend);
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);
    }

    private void initRequestQueue() {
        requestQueue = Volley.newRequestQueue(this);
    }

    private void addWelcomeMessage() {
        addBotMessage("Hello! I'm MediBot, your personal health assistant. How can I help you today?");

    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());

        etUserInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void sendMessage() {
        String userInput = etUserInput.getText().toString().trim();
        if (!userInput.isEmpty()) {
            Log.d(TAG, "Sending message: " + userInput);
            addUserMessage(userInput);
            etUserInput.setText("");

            // Show typing indicator
            addBotMessage("Thinking...");

            sendQueryToBackend(userInput);
        } else {
            Toast.makeText(this, "Please type a message", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendQueryToBackend(String userQuery) {
        String url = BASE_URL + "/predict";
        Log.d(TAG, "Sending request to: " + url);

        JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("question", userQuery);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request", e);
            removeBotTypingIndicator();
            addBotMessage("Failed to build request. Please try again.");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonRequest,
                response -> {
                    Log.d(TAG, "Raw response received");
                    handleSuccessResponse(response);
                },
                error -> {
                    Log.e(TAG, "Network error", error);
                    handleErrorResponse(error);
                }
        );

        // Set retry policy - longer timeout for medical processing
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10 seconds timeout
                2,     // 2 retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void handleSuccessResponse(JSONObject response) {
        try {
            removeBotTypingIndicator();

            if (response.has("answer")) {
                String botReply = response.getString("answer");

                // Clean the response (remove any remaining question repetition)
                String cleanedReply = cleanBotResponse(botReply);

                Log.d(TAG, "Bot reply: " + cleanedReply);
                addBotMessage(cleanedReply);

            } else if (response.has("error")) {
                String errorMsg = response.getString("error");
                addBotMessage("Error: " + errorMsg);
            } else {
                addBotMessage("Unexpected response format from server.");
                Log.w(TAG, "No 'answer' or 'error' field in response");
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing response JSON", e);
            addBotMessage("Invalid response format from server. Please try again.");
        }
    }

    private void handleErrorResponse(com.android.volley.VolleyError error) {
        removeBotTypingIndicator();

        String errorMessage = "Connection failed. ";

        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            Log.e(TAG, "HTTP Error: " + statusCode);

            switch (statusCode) {
                case 404:
                    errorMessage += "Server endpoint not found.";
                    break;
                case 500:
                    errorMessage += "Server internal error.";
                    break;
                case 503:
                    errorMessage += "Server temporarily unavailable.";
                    break;
                default:
                    errorMessage += "Server error (Code: " + statusCode + ")";
            }
        } else {
            errorMessage += "Please check your internet connection and ensure the medical server is running.";
        }

        addBotMessage(errorMessage);
    }

    private String cleanBotResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "I'm not sure about that. Please consult a healthcare professional.";
        }

        String cleaned = response.trim();

        // Remove common question repetitions that might still slip through
        String[] questionStarters = {
                "what are the symptoms of", "what are symptoms of",
                "what is normal", "what is", "what are",
                "how to", "why is", "when should"
        };

        String lowerCleaned = cleaned.toLowerCase();
        for (String starter : questionStarters) {
            if (lowerCleaned.startsWith(starter)) {
                cleaned = cleaned.substring(starter.length()).trim();
                break;
            }
        }

        // Remove question marks at the beginning
        if (cleaned.startsWith("?")) {
            cleaned = cleaned.substring(1).trim();
        }

        // Capitalize first letter if needed
        if (!cleaned.isEmpty() && Character.isLowerCase(cleaned.charAt(0))) {
            cleaned = Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
        }

        return cleaned.isEmpty() ? response : cleaned;
    }

    private void removeBotTypingIndicator() {
        // Remove the last message if it's the typing indicator
        if (!messageList.isEmpty()) {
            ChatMessage lastMessage = messageList.get(messageList.size() - 1);
            if (lastMessage.getSender() == ChatMessage.SENDER_BOT &&
                    "Thinking...".equals(lastMessage.getMessage())) {
                messageList.remove(messageList.size() - 1);
                chatAdapter.notifyItemRemoved(messageList.size());
            }
        }
    }

    private void addUserMessage(String text) {
        messageList.add(new ChatMessage(text, ChatMessage.SENDER_USER));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        messageList.add(new ChatMessage(text, ChatMessage.SENDER_BOT));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (messageList.size() > 0) {
            rvChat.smoothScrollToPosition(messageList.size() - 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}