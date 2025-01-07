import json
import argparse
import subprocess

def find_method_signature(data, method_signature):
    for class_methods in data:
        for methods in class_methods.values():
            for method in methods:
                if method['methodSignature'] == method_signature:
                    return method
    return None

def update_transitive_unchecked_exceptions(data):
    for class_methods in data:
        for methods in class_methods.values():
            for method in methods:
                transitive_unchecked_exceptions = set()
                for internal_call in method['internal_method_calls']:
                    called_method = find_method_signature(data, internal_call)
                    if called_method:
                        transitive_unchecked_exceptions.update(called_method['unchecked_exceptions'])
                method['transitive_unchecked_exceptions'] = list(transitive_unchecked_exceptions)

def main():
    parser = argparse.ArgumentParser(description='Process two library strings.')
    parser.add_argument('libraryOld', type=str, help='The old library string')
    parser.add_argument('libraryNew', type=str, help='The new library string')
    args = parser.parse_args()

    libraryOldPath = "resources/" + args.libraryOld
    libraryNewPath = "resources/" + args.libraryNew

    # Invoke the JAR file for older version of the library
    jar_path = 'target/unexpectedException-1.0-SNAPSHOT.jar'
    # subprocess.run(['java', '-cp', jar_path, "org.vinayak.Main", libraryOldPath, args.libraryOld])

    # Invoke the JAR file for newer version of the library
    # subprocess.run(['java', '-cp', jar_path, "org.vinayak.Main", libraryNewPath, args.libraryNew])

    jsonFilePathOld = args.libraryOld.replace('.jar', '.json')
    jsonFilePathNew = args.libraryNew.replace('.jar', '.json')

    with open(jsonFilePathOld, 'r') as file:
        data = json.load(file)

    update_transitive_unchecked_exceptions(data)

    with open(jsonFilePathOld, 'w') as file:
        json.dump(data, file, indent=4)

    with open(jsonFilePathNew, 'r') as file:
        data = json.load(file)

    update_transitive_unchecked_exceptions(data)

    with open(jsonFilePathNew, 'w') as file:
        json.dump(data, file, indent=4)

if __name__ == "__main__":
    main()