import subprocess
import os
from tqdm import tqdm

def main():
    file_path = os.path.join(os.path.dirname(__file__), "clientDataSet.txt")
    if not os.path.exists(file_path):
        print("Error: clientDataSet.txt not found.")
        return

    # Read all lines
    with open(file_path, "r") as f:
        lines = [line.strip() for line in f if line.strip()]

    # Process each repo with a progress bar
    for owner_repo in tqdm(lines, desc="Processing repos"):
        print(f"Running script for: {owner_repo}")
        subprocess.run(["python3", "findUCBBC.py", owner_repo])

if __name__ == "__main__":
    main()