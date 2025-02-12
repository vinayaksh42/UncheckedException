import argparse
import os
import sys
import subprocess
import shutil

def delete_directory(directory):
    """Deletes the specified directory and all its contents."""
    if os.path.exists(directory):
        shutil.rmtree(directory)
        print(f"Deleted directory: {directory}")
    else:
        print(f"Directory does not exist: {directory}")

def create_directory(directory):
    """Creates the specified directory if it does not already exist."""
    os.makedirs(directory, exist_ok=True)
    print(f"Created directory: {directory}")

def delete_directory_contents(directory):
    """Deletes all contents inside the specified directory without removing the directory itself."""
    if os.path.exists(directory) and os.path.isdir(directory):
        for item in os.listdir(directory):
            item_path = os.path.join(directory, item)
            if os.path.isfile(item_path) or os.path.islink(item_path):
                os.unlink(item_path)
            elif os.path.isdir(item_path):
                shutil.rmtree(item_path)
        print(f"Deleted contents of directory: {directory}")
    else:
        print(f"Directory does not exist: {directory}")

def copy_directory(src, dst):
    """Copies the entire directory from src to dst."""
    if os.path.exists(src) and os.path.isdir(src):
        shutil.copytree(src, dst, dirs_exist_ok=True)
        print(f"Copied directory from {src} to {dst}")
    else:
        print(f"Source directory does not exist: {src}")

def copy_file(src_file, dst_file):
    """Copies a specific file from src_file to dst_file."""
    if os.path.exists(src_file) and os.path.isfile(src_file):
        os.makedirs(os.path.dirname(dst_file), exist_ok=True)
        shutil.copy2(src_file, dst_file)
        print(f"Copied file from {src_file} to {dst_file}")
    else:
        print(f"Source file does not exist: {src_file}")


def clone_repository(repo_url, clone_dir):
    """
    Clones the repository from repo_url into clone_dir.
    Removes the directory first if it already exists.
    """
    if os.path.exists(clone_dir):
        print(f"Removing existing directory: {clone_dir}")
        shutil.rmtree(clone_dir)
    try:
        print(f"Cloning repository from {repo_url} into {clone_dir}...")
        subprocess.run(["git", "clone", repo_url, clone_dir], check=True)
    except subprocess.CalledProcessError:
        print("An error occurred while cloning the repository.")
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

def download_depofdep(jar_file, depofdep_dir):
    if os.path.exists(depofdep_dir):
        shutil.rmtree(depofdep_dir)
    create_directory(depofdep_dir)
    subprocess.run(['python', 'getDepJars.py', jar_file, depofdep_dir])

def main():
    if len(sys.argv) != 2:
        print("Usage: python script.py <owner/repo>")
        sys.exit(1)

    repo_url = "https://github.com/" + sys.argv[1]
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
    jar_path = '../target/unexpectedException-1.0-SNAPSHOT.jar'

    setup_dirs = [clone_dir, dep_old_dir, dep_new_dir, depofdep_old_dir, depofdep_new_dir, client_jar_dir, client_results_dir]
    for directory in setup_dirs:
        # Delete the directory if it already exists
        delete_directory(directory)
        create_directory(directory)


    clone_repository(repo_url, clone_dir)

    pom_path = find_pom_file(clone_dir)
    if pom_path:
        print("Maven project detected!")
        print(f"Found pom.xml at: {pom_path}")

        pom_dir = os.path.dirname(pom_path)

        commands = [
            ["mvn", "clean", "package", "-DskipTests", "-fn"],
            ["mvn", "dependency:copy-dependencies"]
        ]
        
        run_maven_commands(pom_dir,commands)

        copy_artifacts(pom_dir, client_jar_dir, dep_old_dir)

        commands = [
            ["mvn", "org.codehaus.mojo:versions-maven-plugin:2.18.0:use-latest-versions"],
            ["mvn", "clean", "package", "-DskipTests", "-fn"],
            ["mvn", "dependency:copy-dependencies"]
        ]

        run_maven_commands(pom_dir, commands)

        copy_artifacts(pom_dir, client_jar_dir, dep_new_dir)

        delete_directory(clone_dir)

        client_name = ""

        # step 2-4: get the jar file which does not contain the "original" from client_jar 
        jar_files = [f for f in os.listdir(client_jar_dir) if f.endswith(".jar") and "original" not in f]
        if len(jar_files) == 1:
            print(f"Found the client jar file: {jar_files[0]}")
            client_name = jar_files[0].split(".jar")[0]
            print(f"Running the analysis on the client jar file: {jar_files[0]}")
            create_directory(client_results_dir)
            subprocess.run(['java', '-cp', jar_path, "org.vinayak.Main", "../client/client_jar/" + jar_files[0], client_name, "client"])
        else:
            print("Error: No client jar file found or multiple client jar files found.")
            sys.exit(1)

        # Step 3: Analyze the libraries client depends on (jar Files)

        # Step 3-1: get the jar file of libraries on which the client's libraries depend on
        jar_files_old = [f for f in os.listdir(dep_old_dir) if f.endswith(".jar")]
        jar_files_new = [f for f in os.listdir(dep_new_dir) if f.endswith(".jar")]

        for jar_file_old in jar_files_old:
            # Skip if the jar file is junit
            if "junit" in jar_file_old:
                continue
            # get the coresponding jar file new in the new dependency
            match = False
            jar_file_new = ""
            for jar_file_temp_new in jar_files_new:
                if jar_file_old.split("-")[0] in jar_file_temp_new:
                    match = True
                    jar_file_new = jar_file_temp_new
                    break
            if not match:
                print(f"No matching jar file found for {jar_file_old}")
                continue
            print(f"Analyzing library: {jar_file_old}")

            libraryOld = jar_file_old.split(".jar")[0]
            libraryNew = jar_file_new.split(".jar")[0]

            # check if the library is already analyzed
            if os.path.exists(result_dir + "/" + jar_file_old.split(".jar")[0] + "->" + jar_file_new.split(".jar")[0] + ".json"):
                print(f"Library {jar_file_old} and {jar_file_new} already analyzed")
                subprocess.run(['python', 'searchMethodsToTest.py', '../client/client_results/' + client_name + '.json', '../CompareResult/' + libraryOld + "->" + libraryNew + ".json" , '../Match/' + libraryOld + "->" + libraryNew  +  "->" + client_name + '.json'])
                continue

            download_depofdep(jar_file_new, depofdep_new_dir)
            download_depofdep(jar_file_old, depofdep_old_dir)
            
            print(f"Running analysis on library: {libraryOld} and {libraryNew}")
            subprocess.run(['python', 'transitiveException.py', libraryOld, libraryNew])

            subprocess.run(['python', 'searchMethodsToTest.py', '../client/client_results/' + client_name + '.json', '../CompareResult/' + libraryOld + "->" + libraryNew + ".json" , '../Match/' + libraryOld + "->" + libraryNew  +  "->" + client_name + '.json'])

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
