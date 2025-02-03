import requests
import argparse
import os
import shutil

def get_latest_version(group_id, artifact_id):
    """Fetches the latest version of a Maven library from Maven Central."""
    url = f"https://search.maven.org/solrsearch/select?q=g:{group_id}+AND+a:{artifact_id}&core=gav&rows=1&wt=json"
    response = requests.get(url)
    
    if response.status_code == 200:
        data = response.json()
        if data["response"]["docs"]:
            latest_version = data["response"]["docs"][0]["v"]
            return latest_version
        else:
            print("Library not found on Maven Central.")
            return None
    else:
        print("Failed to connect to Maven Central.")
        return None

def download_jar(group_id, artifact_id, version):
    """Downloads the JAR file of the specified Maven library version."""
    jar_url = f"https://repo1.maven.org/maven2/{group_id.replace('.', '/')}/{artifact_id}/{version}/{artifact_id}-{version}.jar"
    
    response = requests.get(jar_url, stream=True)
    if response.status_code == 200:
        jar_filename = f"{artifact_id}-{version}.jar"
        with open(jar_filename, "wb") as file:
            for chunk in response.iter_content(chunk_size=1024):
                file.write(chunk)
        print(f"{jar_filename}")
    else:
        print("Error downloading JAR file.")

    jar_name = f"{artifact_id}-{version}.jar"

    resources_dir = os.path.join(os.path.dirname(__file__), "resources")
    os.makedirs(resources_dir, exist_ok=True)
    shutil.copy2(jar_name, resources_dir)

def main():
    parser = argparse.ArgumentParser(description="Download the latest JAR file of a Maven library.")
    parser.add_argument("group_id", help="Group ID of the Maven library")
    parser.add_argument("artifact_id", help="Artifact ID of the Maven library")
    parser.add_argument("--version", help="Specific version to download (if omitted, latest version will be used)", default=None)

    args = parser.parse_args()

    group_id = args.group_id
    artifact_id = args.artifact_id
    version = args.version

    if not version:
        version = get_latest_version(group_id, artifact_id)
        if not version:
            return

    download_jar(group_id, artifact_id, version)

if __name__ == "__main__":
    main()
