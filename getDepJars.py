#!/usr/bin/env python3

import os
import sys
import textwrap
import subprocess
import shutil

def create_maven_project_and_download_jars(
    dep_group_id, 
    dep_artifact_id, 
    dep_version,
    project_dir="my-maven-project",
    proj_group_id="com.example",
    proj_artifact_id="my-app",
    proj_version="1.0-SNAPSHOT"
):
    """
    1) Creates a simple Maven project folder structure with a minimal pom.xml.
    2) Adds the provided dependency (GAV) to the pom.xml.
    3) Invokes Maven to download all the required JAR files (direct + transitive).
    4) Places the downloaded dependencies into project_dir/libs/.
    """

    # Step A: Create the project directory (if it doesn't already exist)
    os.makedirs(project_dir, exist_ok=True)

    # Step B: Generate a minimal POM that includes the desired dependency
    pom_content = textwrap.dedent(f"""\
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                                 http://maven.apache.org/xsd/maven-4.0.0.xsd">
      <modelVersion>4.0.0</modelVersion>

      <!-- Basic project coordinates (adjust as needed) -->
      <groupId>{proj_group_id}</groupId>
      <artifactId>{proj_artifact_id}</artifactId>
      <version>{proj_version}</version>

      <dependencies>
        <dependency>
          <groupId>{dep_group_id}</groupId>
          <artifactId>{dep_artifact_id}</artifactId>
          <version>{dep_version}</version>
        </dependency>
      </dependencies>

    </project>
    """)

    pom_path = os.path.join(project_dir, "pom.xml")
    with open(pom_path, "w", encoding="utf-8") as pom_file:
        pom_file.write(pom_content)

    # Step C: Create standard Maven src directory structure
    for subdir in [
        "src/main/java",
        "src/main/resources",
        "src/test/java",
        "src/test/resources"
    ]:
        os.makedirs(os.path.join(project_dir, subdir), exist_ok=True)

    print(f"Created Maven project in '{project_dir}'")
    print(f"Added dependency {dep_group_id}:{dep_artifact_id}:{dep_version} to pom.xml")

    # Step D: Run Maven to download dependencies into a 'libs' folder
    # We'll use `mvn dependency:copy-dependencies` with -DoutputDirectory=libs
    try:
        print("\n=== Running Maven to download all dependencies ===")
        subprocess.run(
            ["mvn", "clean", "dependency:copy-dependencies", "-DoutputDirectory=libs"],
            cwd=project_dir,
            check=True
        )
        print("Maven successfully downloaded all dependencies to 'libs/' folder.")
    except subprocess.CalledProcessError as e:
        print("Error running Maven:", e)
        sys.exit(e.returncode)


def main():
    """
    Usage:
      create_and_run_maven_project.py <depGroupId> <depArtifactId> <depVersion>

    Example:
      ./create_and_run_maven_project.py com.esotericsoftware.kryo kryo 3.0.3
    """
    if len(sys.argv) != 4:
        print("Usage: create_and_run_maven_project.py <depGroupId> <depArtifactId> <depVersion>")
        sys.exit(1)

    dep_group_id = sys.argv[1]
    dep_artifact_id = sys.argv[2]
    dep_version = sys.argv[3]

    # Customize the project directory and base coordinates if you like
    project_dir = "my-maven-project"
    proj_group_id = "com.example"
    proj_artifact_id = "my-app"
    proj_version = "1.0-SNAPSHOT"

    create_maven_project_and_download_jars(
        dep_group_id,
        dep_artifact_id,
        dep_version,
        project_dir=project_dir,
        proj_group_id=proj_group_id,
        proj_artifact_id=proj_artifact_id,
        proj_version=proj_version
    )
    excluded_jar = f"{dep_artifact_id}-{dep_version}.jar"
    jars_to_return = []
    resource_folder = os.path.join(os.path.dirname(project_dir), "resources")
    os.makedirs(resource_folder, exist_ok=True)
    libs_path = os.path.join(project_dir, "libs")
    if os.path.isdir(libs_path):
        for jar_file in os.listdir(libs_path):
            if jar_file.endswith(".jar"):
                if jar_file != excluded_jar:
                    jars_to_return.append(jar_file)
                shutil.copy2(os.path.join(libs_path, jar_file), resource_folder)
    shutil.rmtree(project_dir)
    print("\n".join(jars_to_return))


if __name__ == "__main__":
    main()
