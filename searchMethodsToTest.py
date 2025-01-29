import json

def load_json(filepath):
    with open(filepath, 'r') as file:
        return json.load(file)

def find_external_calls(client_methods, library_methods):
    external_calls = []
    library_method_signatures = {method['methodSignature'] for method in library_methods}

    for client_class in client_methods:
        for methods in client_class.values():
            for method in methods:
                if 'external_method_calls' in method:
                    for external_call in method['external_method_calls']:
                        if external_call in library_method_signatures:
                            external_calls.append({
                                'client_method': method['methodSignature'],
                                'external_call': external_call
                            })

    return external_calls

def main():
    client_json_path = '/Users/vinayaksh42/Desktop/Research/BBC Research/unexpectedException/results/httpclient5-5.0-beta6.json'
    library_json_path = '/Users/vinayaksh42/Desktop/Research/BBC Research/unexpectedException/results/commons-codec-1.12->commons-codec-1.13.json'
    output_json_path = '/Users/vinayaksh42/Desktop/Research/BBC Research/unexpectedException/results/external_calls.json'

    client_methods = load_json(client_json_path)
    library_methods = load_json(library_json_path)

    external_calls = find_external_calls(client_methods, library_methods)

    with open(output_json_path, 'w') as output_file:
        json.dump(external_calls, output_file, indent=4)

if __name__ == '__main__':
    main()