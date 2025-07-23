import json

# Load your SQuAD-style dataset
with open("filtered_pubmedqa_squad_format.json", "r", encoding="utf-8") as f:
    squad_data = json.load(f)

# Extract all unique context entries
contexts = set()
for item in squad_data["data"]:
    for paragraph in item["paragraphs"]:
        context_text = paragraph["context"].strip()
        if context_text:
            contexts.add(context_text)

# Write to medical_data.txt
with open("medical_data.txt", "w", encoding="utf-8") as out_f:
    for ctx in contexts:
        out_f.write(ctx + "\n")

print("✅ medical_data.txt created with", len(contexts), "entries.")
