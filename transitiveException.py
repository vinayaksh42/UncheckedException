import json
import argparse
import subprocess
from collections import Counter

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
                method['unchecked_exceptions'].extend(transitive_unchecked_exceptions)

def update_transitive_unchecked_exceptions_external(data, externalData):
    for class_methods in data:
        for methods in class_methods.values():
            for method in methods:
                JAVASTL_unchecked_exceptions = set()
                for external_call in method['external_method_calls']:
                    called_method = find_method_signature(externalData, external_call)
                    if called_method:
                        JAVASTL_unchecked_exceptions.update(called_method['unchecked_exceptions'])
                method['unchecked_java_exceptions_external'] = list(JAVASTL_unchecked_exceptions)

def runAnalysisOnLibrary(libraryOld, libraryNew):
    libraryOldPath = "resources/" + libraryOld + ".jar"
    libraryNewPath = "resources/" + libraryNew + ".jar"

    # Invoke the JAR file for older version of the library
    jar_path = 'target/unexpectedException-1.0-SNAPSHOT.jar'
    subprocess.run(['java', '-cp', jar_path, "org.vinayak.Main", libraryOldPath, libraryOld, "library"])

    # Invoke the JAR file for newer version of the library
    subprocess.run(['java', '-cp', jar_path, "org.vinayak.Main", libraryNewPath, libraryNew, "library"])

def runInternalExceptionAddition(libraryOld, libraryNew):
    jsonFilePathOld = 'results/' + libraryOld + '.json'
    jsonFilePathNew = 'results/' + libraryNew + '.json'

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

def compareOldandNew(libraryOld, libraryNew):
    jsonFilePathOld = 'results/' + libraryOld + '.json'
    jsonFilePathNew = 'results/' + libraryNew + '.json'

    with open(jsonFilePathOld, 'r') as file:
        dataOld = json.load(file)

    with open(jsonFilePathNew, 'r') as file:
        dataNew = json.load(file)

    new_exceptions = []

    for classes in dataNew:
        for methods in classes.values():
            for method in methods:
                old_method = find_method_signature(dataOld, method['methodSignature'])
                if old_method:
                    difference = list(Counter(method['unchecked_exceptions']) - Counter(old_method['unchecked_exceptions']))
                    new_exceptions.extend([{'methodSignature': method['methodSignature'], 'new_exceptions': difference}])

    new_exceptions = [entry for entry in new_exceptions if entry['new_exceptions']]

    for entry in new_exceptions:
        entry['new_exceptions'] = list(set(entry['new_exceptions']))

    with open(('results/' + libraryOld + "->" + libraryNew + ".json"), 'w') as file:
        json.dump(new_exceptions, file, indent=4)

def runExternalJavaSTLException(libraryOld, libraryNew):
    jsonFilePathOld = 'results/' + libraryOld + '.json'
    jsonFilePathNew = 'results/' + libraryNew + '.json'

    with open(jsonFilePathOld, 'r') as file:
        data = json.load(file)
    
    with open('resources/rt.json', 'r') as file:
        externalData = json.load(file)
    
    update_transitive_unchecked_exceptions_external(data, externalData)

    with open(jsonFilePathOld, 'w') as file:
        json.dump(data, file, indent=4)
    
    with open(jsonFilePathNew, 'r') as file:
        data = json.load(file)
    
    update_transitive_unchecked_exceptions_external(data, externalData)

    with open(jsonFilePathNew, 'w') as file:
        json.dump(data, file, indent=4)


def main():
    parser = argparse.ArgumentParser(description='Process two library strings.')
    parser.add_argument('libraryOld', type=str, help='The old library string')
    parser.add_argument('libraryNew', type=str, help='The new library string')
    args = parser.parse_args()

    # generates the JSON files for the old and new version of the library - step 0
    runAnalysisOnLibrary(args.libraryOld, args.libraryNew)

    # adding the transitive unchecked exceptions for the old and new version of the library - step 1
    # runInternalExceptionAddition(args.libraryOld, args.libraryNew)

    # adding the transitive unchecked exceptions for the old and new version of the library from the JAVA Standard library - step 2
    # runExternalJavaSTLException(args.libraryOld, args.libraryNew)

    # comparing the old and new version of the library - step final
    compareOldandNew(args.libraryOld, args.libraryNew)

if __name__ == "__main__":
    main()

