import argparse
import os
import sys
import subprocess
import shutil

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

def run_maven_commands(pom_dir):
    """
    Runs the specified Maven commands in the directory that contains the pom.xml.
    """
    commands = [
        ["mvn", "clean", "package", "-DskipTests", "-fn"],
        ["mvn", "dependency:copy-dependencies"]
    ]
    for command in commands:
        print(f"Running Maven command: {' '.join(command)} in directory: {pom_dir}")
        try:
            subprocess.run(command, cwd=pom_dir, check=True)
        except subprocess.CalledProcessError:
            print(f"An error occurred while running Maven command: {' '.join(command)}")
            sys.exit(1)

def copy_artifacts(pom_dir):
    # Copy dependencies from 'target/dependency' to a 'dependency' folder outside this directory
    dependency_src = os.path.join(pom_dir, "target", "dependency")
    dependency_dest = os.path.join(os.path.dirname(__file__), "dependency")
    if os.path.isdir(dependency_src):
        shutil.copytree(dependency_src, dependency_dest, dirs_exist_ok=True)

    # Copy any .jar files from 'target' to 'client_jar'
    target_dir = os.path.join(pom_dir, "target")
    client_jar_dir = os.path.join(os.path.dirname(__file__), "client_jar")
    os.makedirs(client_jar_dir, exist_ok=True)
    jar_files = [f for f in os.listdir(target_dir) if f.endswith(".jar")]
    for jar_file in jar_files:
        src_path = os.path.join(target_dir, jar_file)
        dst_path = os.path.join(client_jar_dir, jar_file)
        shutil.copy2(src_path, dst_path)

def main():
    if len(sys.argv) != 2:
        print("Usage: python script.py <owner/repo>")
        sys.exit(1)

    repo_url = "https://github.com/" + sys.argv[1]
    clone_dir = "cloned_repo"  # Temporary directory for cloning

    # Step 1: Clone the repository
    clone_repository(repo_url, clone_dir)

    # Step 2: Analyze the client
    pom_path = find_pom_file(clone_dir)
    if pom_path:
        print("Maven project detected!")
        print(f"Found pom.xml at: {pom_path}")

        # Change working directory to the location of pom.xml
        pom_dir = os.path.dirname(pom_path)
        
        # Step 2-1: Run Maven commands
        run_maven_commands(pom_dir)

        # Step 2-2: Copy artifacts to the current directory
        copy_artifacts(pom_dir)

        # Step 2-3: Clean up the cloned repository
        shutil.rmtree(clone_dir)

        # step 2-4: get the jar file which does not contain the "original" from client_jar 
        client_jar_dir = os.path.join(os.path.dirname(__file__), "client_jar")
        jar_files = [f for f in os.listdir(client_jar_dir) if f.endswith(".jar") and "original" not in f]
        if len(jar_files) == 1:
            print(f"Found the client jar file: {jar_files[0]}")
            client_name = jar_files[0].split("-")[0]
            jar_path = 'target/unexpectedException-1.0-SNAPSHOT.jar'
            subprocess.run(['java', '-cp', jar_path, "org.vinayak.Main", "client_jar/" + jar_files[0], client_name, "client"])
        else:
            print("Error: No client jar file found or multiple client jar files found.")
            sys.exit(1)

        # Step 3: Analyze the libraries client depends on (jar Files)

        # Step 3-1: get the jar file of libraries on which the client's libraries depend on
        dependency_dir = os.path.join(os.path.dirname(__file__), "dependency")
        jar_files = [f for f in os.listdir(dependency_dir) if f.endswith(".jar")]

        for jar_file in jar_files:
            print(f"Analyzing library: {jar_file}")
            # Step 3-2: Download the latest version of the library
            if os.path.exists('newdependency'):
                shutil.rmtree('newdependency')
            subprocess.run(['mkdir', 'newdependency'])
            subprocess.run(['python', 'downloadLatestVersion.py', jar_file])
            # Step 3-3: get the deps of new library
            new_jar_files = [f for f in os.listdir('newdependency') if f.endswith(".jar")]
            depofdep_dir = os.path.join(os.path.dirname(__file__), "depofdepNew")
            if os.path.exists(depofdep_dir):
                shutil.rmtree(depofdep_dir)
            subprocess.run(['python', 'getDepJars.py', new_jar_files[0], "depofdepNew"])
            # Step 3-4: get tjhe deps of old library
            depofdep_dir = os.path.join(os.path.dirname(__file__), "depofdepOld")
            if os.path.exists(depofdep_dir):
                shutil.rmtree(depofdep_dir)
            subprocess.run(['python', 'getDepJars.py', jar_file, "depofdepOld"])
            
            # Step 3-5: Run the analysis on the old and new library
            libraryOld = jar_file.split(".jar")[0]
            libraryNew = new_jar_files[0].split(".jar")[0]
            subprocess.run(['python', 'transitiveException.py', libraryOld, libraryNew])

            # Step 4: perform the comparison between client and library under analysis
            subprocess.run(['python', 'searchMethodsToTest.py', 'client_results/' + client_name + '.json', 'finalResults/' + libraryOld + "->" + libraryNew  + '.json'])

            

    else:
        print("No Maven project detected in this repository (pom.xml not found).")
        # Cleanup the cloned repository if no pom.xml is found.
        print("Cleaning up cloned repository...")
        shutil.rmtree(clone_dir)

if __name__ == "__main__":
    main()
