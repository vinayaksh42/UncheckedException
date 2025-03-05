import argparse
import os
import sys
import subprocess
import shutil
import requests
import csv
import datetime
import json

JAVA_VERSION = {
    "1.8": "/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home",
    "11": "/Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home" }

def delete_directory(directory):
    """Deletes the specified directory and all its contents."""
    if os.path.exists(directory):
        shutil.rmtree(directory)

def create_directory(directory):
    """Creates the specified directory if it does not already exist."""
    os.makedirs(directory, exist_ok=True)

def delete_directory_contents(directory):
    """Deletes all contents inside the specified directory without removing the directory itself."""
    if os.path.exists(directory) and os.path.isdir(directory):
        for item in os.listdir(directory):
            item_path = os.path.join(directory, item)
            if os.path.isfile(item_path) or os.path.islink(item_path):
                os.unlink(item_path)
            elif os.path.isdir(item_path):
                shutil.rmtree(item_path)

def copy_directory(src, dst):
    """Copies the entire directory from src to dst."""
    if os.path.exists(src) and os.path.isdir(src):
        shutil.copytree(src, dst, dirs_exist_ok=True)

def copy_file(src_file, dst_file):
    """Copies a specific file from src_file to dst_file."""
    if os.path.exists(src_file) and os.path.isfile(src_file):
        os.makedirs(os.path.dirname(dst_file), exist_ok=True)
        shutil.copy2(src_file, dst_file)

def clone_repository(repo_url, clone_dir, commit_sha):
    """
    Clones the repository from repo_url into clone_dir.
    Removes the directory first if it already exists.
    """
    if os.path.exists(clone_dir):
        print(f"\033[33mRemoving existing directory: {clone_dir}\033[0m")
        shutil.rmtree(clone_dir)
    try:
        print(f"\033[32mCloning repository from {repo_url} into {clone_dir}...\033[0m")
        subprocess.run(["git", "clone", repo_url, clone_dir], check=True)
    except subprocess.CalledProcessError:
        print("\033[31mAn error occurred while cloning the repository.\033[0m")
        sys.exit(1)

    if commit_sha:
        try:
            print(f"\033[32mChecking out commit: {commit_sha}\033[0m")
            subprocess.run(["git", "checkout", commit_sha], cwd=clone_dir, check=True)
        except subprocess.CalledProcessError:
            print("\033[31mAn error occurred while cloning the repository.\033[0m")
            sys.exit(1)

def find_pom_file(directory):
    """
    Recursively searches for a pom.xml file within directory.
    Returns the full path to the file if found; otherwise, returns None.
    """
    for root, _, files in os.walk(directory):
        if "pom.xml" in files:
            return os.path.join(root, "pom.xml")
    return None

def run_maven_commands(pom_dir, commands):
    """
    Runs the specified Maven commands in the directory that contains the pom.xml.
    """
    
    for command in commands:
        print(f"Running Maven command: {' '.join(command)} in directory: {pom_dir}")
        try:
            subprocess.run(command, cwd=pom_dir, check=True)
        except subprocess.CalledProcessError:
            print(f"An error occurred while running Maven command: {' '.join(command)}")
            sys.exit(1)

def copy_artifacts(pom_dir, client_jar_dir, dep_to_copy):
    # Copy dependencies from 'target/dependency' to dep_old_dir
    dependency_src = os.path.join(pom_dir, "target", "dependency")
    create_directory(dep_to_copy)
    copy_directory(dependency_src, dep_to_copy)

    # Copy any .jar files from 'target' to client_jar_dir
    target_dir = os.path.join(pom_dir, "target")
    create_directory(client_jar_dir)
    jar_files = [f for f in os.listdir(target_dir) if f.endswith(".jar")]
    for jar_file in jar_files:
        src_path = os.path.join(target_dir, jar_file)
        dst_path = os.path.join(client_jar_dir, jar_file)
        copy_file(src_path, dst_path)

def copy_jars_only(pom_dir, dep_to_copy):
    # Copy dependencies from 'target/dependency' to dep_new_dir
    dependency_src = os.path.join(pom_dir, "target", "dependency")
    create_directory(dep_to_copy)
    copy_directory(dependency_src, dep_to_copy)

def download_depofdep(jar_file, depofdep_dir):
    if os.path.exists(depofdep_dir):
        shutil.rmtree(depofdep_dir)
    create_directory(depofdep_dir)
    subprocess.run(['python', 'getDepJars.py', jar_file, depofdep_dir])

# function to fetch the commit sha of the repository
def get_commit_sha(owner_repo):
    response = requests.get(f"https://api.github.com/repos/{owner_repo}/commits")
    if response.status_code == 200:
        data = response.json()
        commit_sha = data[0]['sha']
        return commit_sha
    else:
        print(f"Failed to fetch commit sha for repository: {owner_repo}")
        return None

# function to save the results in a pre existing csv file if not then create a new one with the header
def saveResults(libraryOld, libraryNew, client_name, owner_repo, commit_sha, final_result, final_result_name,match_dir):
    print(f"Saving the results in {final_result}/results.csv")

    # check if the file exists
    if not os.path.exists(final_result + "/results.csv"):
        with open(final_result + "/results.csv", "w", newline="") as file:
            writer = csv.writer(file)
            writer.writerow(["ClientName","OwnerRepo","CommitSha","LibraryOld", "LibraryNew", "Match Results", "Time", "GitHubRepo", "NumberOfMatchedMethods"])
    
    # open the Match results in ../Match folder for the client
    with open(match_dir + "/" + final_result_name) as match_file:
        match_data = json.load(match_file)
    
    with open(final_result + "/results.csv", "a", newline="") as file:
        writer = csv.writer(file)
        writer.writerow([client_name, owner_repo, commit_sha, libraryOld, libraryNew, "https://github.com/vinayaksh42/UncheckedException/tree/main/Match/" + final_result_name, datetime.datetime.now(), "github,com/" + owner_repo, len(match_data)])
    
    # Create combined results CSV
    results_csv_path = final_result + "/results.csv"
    combined_results_csv_path = final_result + "/combined_results.csv"

    # Read the results.csv file into a dictionary
    results_dict = {}
    with open(results_csv_path, mode='r', newline='') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            results_dict[row['Match Results']] = row

    # Create combined results CSV
    results_csv_path = final_result + "/results.csv"
    combined_results_csv_path = final_result + "/combined_results.csv"

    # Read the results.csv file into a dictionary
    results_dict = {}
    with open(results_csv_path, mode='r', newline='') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            results_dict[row['Match Results']] = row

    # Create the output CSV file
    with open(combined_results_csv_path, mode='w', newline='') as csvfile:
        fieldnames = ['ClientName', 'OwnerRepo', 'GitHubOwnerRepo', 'LibraryOld', 'LibraryNew', 'CommitSha', 'ClientMethod', 'ExternalCall']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()

        # Iterate over each JSON file in the Match folder
        for filename in os.listdir(match_dir):
            if filename.endswith('.json'):
                json_path = os.path.join(match_dir, filename)
                with open(json_path, 'r') as jsonfile:
                    match_data = json.load(jsonfile)
                    match_results_url = f"https://github.com/vinayaksh42/UncheckedException/tree/main/Match/{filename}"
                    
                    if match_results_url in results_dict:
                        result_row = results_dict[match_results_url]
                        for entry in match_data:
                            writer.writerow({
                                'ClientName': result_row['ClientName'],
                                'OwnerRepo': result_row['OwnerRepo'],
                                'GitHubOwnerRepo': result_row['GitHubRepo'],
                                'LibraryOld': result_row['LibraryOld'],
                                'LibraryNew': result_row['LibraryNew'],
                                'CommitSha': result_row['CommitSha'],
                                'ClientMethod': entry['client_method'],
                                'ExternalCall': entry['external_call']
                            })

def main():
    if len(sys.argv) < 1:
        # commit sha is optional
        print("Usage: python script.py <owner/repo> <commit_sha>")
        sys.exit(1)
    owner_repo = sys.argv[1]
    repo_url = "https://github.com/" + owner_repo
    clone_dir = "../client/cloned_repo"
    dep_old_dir = "../client/dep_old"
    dep_new_dir = "../client/dep_new"
    depofdep_old_dir = "../client/depofdep_old"
    depofdep_new_dir = "../client/depofdep_new"
    client_jar_dir = "../client/client_jar"
    client_results_dir = "../client/client_results"
    result_dir = "../CompareResult"
    library_dir = "../LibraryResult"
    match_dir = "../Match"
    final_result = "../results"
    jar_path = '../target/unexpectedException-1.0-SNAPSHOT.jar'
    temp_file = "../client/temp"

    # create ../client folder if it does not exist
    create_directory("../client")

    setup_dirs = [clone_dir, dep_old_dir, dep_new_dir, depofdep_old_dir, depofdep_new_dir, client_jar_dir, client_results_dir, temp_file]
    for directory in setup_dirs:
        # Delete the directory if it already exists
        delete_directory(directory)
        create_directory(directory)

    commit_sha = sys.argv[2] if len(sys.argv) > 2 else get_commit_sha(owner_repo)

    # 1 - Clone Repository
    clone_repository(repo_url, clone_dir, commit_sha)

    # 2 - Check if the repository is a Maven project
    pom_path = find_pom_file(clone_dir)
    if pom_path:
        # check the current java version
        os.environ["JAVA_HOME"] = JAVA_VERSION.get("1.8")
        print("Maven project detected!")
        print(f"Found pom.xml at: {pom_path}")

        pom_dir = os.path.dirname(pom_path)

        commands = [
            ["mvn", "clean", "package", "-DskipTests", "-fn"],
            ["mvn", "dependency:copy-dependencies"]
        ]
        
        # 3 - Run Maven commands to build the project
        run_maven_commands(pom_dir,commands)

        # 4 - Copy the artifacts to the dep_old
        copy_artifacts(pom_dir, client_jar_dir, dep_old_dir)

        commands = [
            ["mvn", "org.codehaus.mojo:versions-maven-plugin:2.18.0:use-latest-versions"],
            ["mvn", "clean", "package", "-DskipTests", "-fn"],
            ["mvn", "dependency:copy-dependencies"]
        ]

        # 5 - Run Maven commands to update the dependencies
        run_maven_commands(pom_dir, commands)

        # 6 - Copy the artifacts to the dep_new
        copy_jars_only(pom_dir, dep_new_dir)

        client_name = ""

        # 7 - Run the analysis on the client jar file
        jar_files = [f for f in os.listdir(client_jar_dir) if f.endswith(".jar") and "original" not in f]
        if len(jar_files) == 1:
            print(f"Found the client jar file: {jar_files[0]}")
            client_name = jar_files[0].split(".jar")[0]
            print(f"Running the analysis on the client jar file: {jar_files[0]}")
            create_directory(client_results_dir)
            subprocess.run(['java', '-Xmx8g', '-cp', jar_path, "org.vinayak.Main", "../client/client_jar/" + jar_files[0], client_name, "client"])
        else:
            print("Error: No client jar file found or multiple client jar files found.")
            sys.exit(1)

        
        jar_files_old = [f for f in os.listdir(dep_old_dir) if f.endswith(".jar")]
        jar_files_new = [f for f in os.listdir(dep_new_dir) if f.endswith(".jar")]

        with open(client_results_dir + "/" + client_name + ".json") as client_file:
            client_data = json.load(client_file)
        
        external_calls = set()
        for entry in client_data:
            for class_name, methods in entry.items():
                for method_info in methods:
                    external_calls.update(method_info["external_method_calls"])

        # 8 - Compare the dependencies
        for jar_file_old in jar_files_old:
            # Remove .jar and split.
            old_parts = jar_file_old.replace(".jar", "").split("-")
            # Strip out snapshot and remove the last chunk (often the version).
            old_parts = [p for p in old_parts if p.lower() != "snapshot"]
            if len(old_parts) > 1:
                old_parts = old_parts[:-1]
            old_base = "-".join(old_parts)

            match = False
            jar_file_new = ""
            for jar_file_temp_new in jar_files_new:
                new_parts = jar_file_temp_new.replace(".jar", "").split("-")
                new_parts = [p for p in new_parts if p.lower() != "snapshot"]
                if len(new_parts) > 1:
                    new_parts = new_parts[:-1]
                new_base = "-".join(new_parts)

                if old_base == new_base:
                    match = True
                    jar_file_new = jar_file_temp_new
                    break

            if not match:
                print(f"No matching jar file found for {jar_file_old}")
                continue

            if jar_file_new == jar_file_old:
                continue
            print(f"Analyzing library: {jar_file_old}")

            libraryOld = jar_file_old.split(".jar")[0]
            libraryNew = jar_file_new.split(".jar")[0]

            libraryOldPath = "../client/dep_old/" + libraryOld + ".jar"

            subprocess.run(['java', '-Xmx8g', '-cp', jar_path, "org.vinayak.Main", libraryOldPath, libraryOld, "jarAnalysis"])

            with open(temp_file + "/" + libraryOld + ".json") as lib_file:
                library_methods = set(json.load(lib_file))
            used_library_methods = external_calls.intersection(library_methods)

            if not used_library_methods:
                print(f"No methods from library {jar_file_old} are used in the client")
                continue

            # check if the library is already analyzed
            if os.path.exists(result_dir + "/" + jar_file_old.split(".jar")[0] + "->" + jar_file_new.split(".jar")[0] + ".json"):
                print(f"Library {jar_file_old} and {jar_file_new} already analyzed")
                final_result_name = libraryOld + "->" + libraryNew  +  "->" + client_name + '.json'
                # 9A - Search methods in the client that might have a BBC due to the newly added unchecked exception
                subprocess.run(['python', 'searchMethodsToTest.py', '../client/client_results/' + client_name + '.json', '../CompareResult/' + libraryOld + "->" + libraryNew + ".json" , '../Match/' + final_result_name])
                # 10A - save the results in a csv file, save the client repo url, library names, library version, matched methods, git commit sha, and the time of the analysis
                saveResults(libraryOld, libraryNew, client_name, owner_repo, commit_sha, final_result, final_result_name,match_dir)
                continue

            # 9B - Download the dependencies of the dependencies
            download_depofdep(jar_file_new, depofdep_new_dir)
            download_depofdep(jar_file_old, depofdep_old_dir)
        
            print(f"Running analysis on library: {libraryOld} and {libraryNew}")
            # 10B - Run the analysis on the library
            os.environ["JAVA_HOME"] = JAVA_VERSION.get("11")
            subprocess.run(['python', 'transitiveException.py', libraryOld, libraryNew])
            final_result_name = libraryOld + "->" + libraryNew  +  "->" + client_name + '.json'
            # 11B - Search methods in the client that might have a BBC due to the newly added unchecked exception
            subprocess.run(['python', 'searchMethodsToTest.py', '../client/client_results/' + client_name + '.json', '../CompareResult/' + libraryOld + "->" + libraryNew + ".json" , '../Match/' + final_result_name])
            # 12B - save the results in a csv file, save the client repo url, library names, library version, matched methods, git commit sha, and the time of the analysis
            saveResults(libraryOld, libraryNew, client_name, owner_repo, commit_sha, final_result, final_result_name,match_dir)

        for directory in setup_dirs:
            # CleanUp
            delete_directory(directory)

    else:
        print("No Maven project detected in this repository (pom.xml not found).")
        # Cleanup the cloned repository if no pom.xml is found.
        print("Cleaning up cloned repository...")
        shutil.rmtree(clone_dir)

if __name__ == "__main__":
    main()
